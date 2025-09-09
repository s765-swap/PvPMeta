package com.swapnil.titlemod.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class RconClient implements AutoCloseable {
   private static final int SERVERDATA_AUTH = 3;
   private static final int SERVERDATA_AUTH_RESPONSE = 2;
   private static final int SERVERDATA_EXECCOMMAND = 2;
   private static final int SERVERDATA_RESPONSE_VALUE = 0;
   private final String host;
   private final int port;
   private final String password;
   private Socket socket;
   private BufferedInputStream in;
   private BufferedOutputStream out;
   private int packetIdCounter = 0;

   public RconClient(String var1, int var2, String var3) {
      this.host = var1;
      this.port = var2;
      this.password = var3;
   }

   public void connect() throws IOException {
      if (this.socket != null && !this.socket.isClosed()) {
         throw new IllegalStateException("RconClient is already connected.");
      } else {
         this.socket = new Socket();
         this.socket.connect(new InetSocketAddress(this.host, this.port), 5000);
         this.in = new BufferedInputStream(this.socket.getInputStream());
         this.out = new BufferedOutputStream(this.socket.getOutputStream());
      }
   }

   public boolean authenticate() throws IOException {
      int var1 = this.getNextPacketId();
      this.sendPacket(var1, 3, this.password);
      RconClient.RconPacket var2 = this.readPacket();
      return var2.id == var1 && var2.type == 2;
   }

   public String sendCommand(String var1) throws IOException {
      int var2 = this.getNextPacketId();
      this.sendPacket(var2, 2, var1);
      StringBuilder var3 = new StringBuilder();

      while(true) {
         RconClient.RconPacket var4 = this.readPacket();
         if (var4.id != var2 || var4.type != 0) {
            if (var4.id == var2 && var4.type == 2) {
               break;
            }

            throw new IOException("Unexpected packet received");
         }

         var3.append(var4.payload);
         if (this.in.available() == 0) {
            this.socket.setSoTimeout(100);

            try {
               RconClient.RconPacket var5 = this.readPacket();
               if (var5.id == var2 && var5.type == 0 && var5.payload.isEmpty()) {
                  break;
               }

               var3.append(var5.payload);
            } catch (SocketTimeoutException var9) {
               break;
            } finally {
               this.socket.setSoTimeout(0);
            }
         }
      }

      return var3.toString().trim();
   }

   public void close() throws IOException {
      if (this.socket != null && !this.socket.isClosed()) {
         this.socket.close();
      }

   }

   private void sendPacket(int var1, int var2, String var3) throws IOException {
      byte[] var4 = var3.getBytes(StandardCharsets.UTF_8);
      int var5 = 8 + var4.length + 2;
      ByteBuffer var6 = ByteBuffer.allocate(var5 + 4);
      var6.order(ByteOrder.LITTLE_ENDIAN);
      var6.putInt(var5);
      var6.putInt(var1);
      var6.putInt(var2);
      var6.put(var4);
      var6.put((byte)0);
      var6.put((byte)0);
      this.out.write(var6.array());
      this.out.flush();
   }

   private RconClient.RconPacket readPacket() throws IOException {
      byte[] var1 = new byte[4];
      int var2 = this.in.read(var1);
      if (var2 == -1) {
         throw new IOException("End of stream reached");
      } else {
         ByteBuffer var3 = ByteBuffer.wrap(var1);
         var3.order(ByteOrder.LITTLE_ENDIAN);
         int var4 = var3.getInt();
         if (var4 < 10) {
            throw new IOException("Invalid packet length");
         } else {
            byte[] var5 = new byte[var4];

            int var7;
            for(int var6 = 0; var6 < var4; var6 += var7) {
               var7 = this.in.read(var5, var6, var4 - var6);
               if (var7 == -1) {
                  throw new IOException("End of stream while reading packet");
               }
            }

            ByteBuffer var12 = ByteBuffer.wrap(var5);
            var12.order(ByteOrder.LITTLE_ENDIAN);
            int var8 = var12.getInt();
            int var9 = var12.getInt();
            byte[] var10 = new byte[var4 - 4 - 4 - 2];
            var12.get(var10);
            String var11 = new String(var10, StandardCharsets.UTF_8);
            return new RconClient.RconPacket(var8, var9, var11);
         }
      }
   }

   private int getNextPacketId() {
      return ++this.packetIdCounter;
   }

   private static class RconPacket {
      public final int id;
      public final int type;
      public final String payload;

      public RconPacket(int var1, int var2, String var3) {
         this.id = var1;
         this.type = var2;
         this.payload = var3;
      }
   }
}
