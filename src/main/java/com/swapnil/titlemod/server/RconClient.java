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

    public RconClient(String host, int port, String password) {
        this.host = host;
        this.port = port;
        this.password = password;
    }

    public void connect() throws IOException {
        if (this.socket != null && !this.socket.isClosed()) {
            throw new IllegalStateException("RconClient is already connected.");
        }
        this.socket = new Socket();
        this.socket.connect(new InetSocketAddress(this.host, this.port), 5000);
        this.in = new BufferedInputStream(this.socket.getInputStream());
        this.out = new BufferedOutputStream(this.socket.getOutputStream());
    }

    public boolean authenticate() throws IOException {
        int packetId = this.getNextPacketId();
        this.sendPacket(packetId, 3, this.password);
        RconClient.RconPacket response = this.readPacket();
        return response.id == packetId && response.type == 2;
    }

    public String sendCommand(String command) throws IOException {
        int packetId = this.getNextPacketId();
        this.sendPacket(packetId, 2, command);
        StringBuilder response = new StringBuilder();

        while (true) {
            RconClient.RconPacket packet = this.readPacket();
            if (packet.id != packetId || packet.type != 0) {
                if (packet.id != packetId || packet.type != 2) {
                    throw new IOException("Unexpected packet received");
                }
                break;
            }

            response.append(packet.payload);
            if (this.in.available() == 0) {
                this.socket.setSoTimeout(100);
                try {
                    RconClient.RconPacket extraPacket = this.readPacket();
                    if (extraPacket.id == packetId && extraPacket.type == 0 && extraPacket.payload.isEmpty()) {
                        break;
                    }
                    response.append(extraPacket.payload);
                } catch (SocketTimeoutException e) {
                    break;
                } finally {
                    this.socket.setSoTimeout(0);
                }
            }
        }

        return response.toString().trim();
    }

    public void close() throws IOException {
        if (this.socket != null && !this.socket.isClosed()) {
            this.socket.close();
        }
    }

    private void sendPacket(int id, int type, String payload) throws IOException {
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
        int length = 8 + payloadBytes.length + 2;
        ByteBuffer buffer = ByteBuffer.allocate(length + 4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(length);
        buffer.putInt(id);
        buffer.putInt(type);
        buffer.put(payloadBytes);
        buffer.put((byte)0);
        buffer.put((byte)0);
        this.out.write(buffer.array());
        this.out.flush();
    }

    private RconClient.RconPacket readPacket() throws IOException {
        byte[] lengthBytes = new byte[4];
        int read = this.in.read(lengthBytes);
        if (read == -1) {
            throw new IOException("End of stream reached");
        }
        
        ByteBuffer buffer = ByteBuffer.wrap(lengthBytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        int length = buffer.getInt();
        
        if (length < 10) {
            throw new IOException("Invalid packet length");
        }
        
        byte[] packetData = new byte[length];
        int totalRead = 0;
        while (totalRead < length) {
            int bytesRead = this.in.read(packetData, totalRead, length - totalRead);
            if (bytesRead == -1) {
                throw new IOException("End of stream while reading packet");
            }
            totalRead += bytesRead;
        }

        ByteBuffer packetBuffer = ByteBuffer.wrap(packetData);
        packetBuffer.order(ByteOrder.LITTLE_ENDIAN);
        int id = packetBuffer.getInt();
        int type = packetBuffer.getInt();
        byte[] payloadBytes = new byte[length - 4 - 4 - 2];
        packetBuffer.get(payloadBytes);
        String payload = new String(payloadBytes, StandardCharsets.UTF_8);
        
        return new RconClient.RconPacket(id, type, payload);
    }

    private int getNextPacketId() {
        return ++this.packetIdCounter;
    }

    private static class RconPacket {
        public final int id;
        public final int type;
        public final String payload;

        public RconPacket(int id, int type, String payload) {
            this.id = id;
            this.type = type;
            this.payload = payload;
        }
    }
}
