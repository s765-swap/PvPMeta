package com.swapnil.titlemod.queue;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import com.swapnil.titlemod.MatchmakingClient;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.net.Socket;
import java.net.InetSocketAddress;

public class QueueManager {
    private static QueueManager instance;
    private final Set<String> queuedKits = new HashSet<>();
    private long queueStartTime = 0;
    private final AtomicBoolean isInQueue = new AtomicBoolean(false);
    private QueueStatusListener statusListener;
    
    public interface QueueStatusListener {
        void onQueueStatusChanged(boolean inQueue, long queueTime, Set<String> kits);
    }
    
    private QueueManager() {}
    
    public static QueueManager getInstance() {
        if (instance == null) {
            instance = new QueueManager();
        }
        return instance;
    }
    
    public void setStatusListener(QueueStatusListener listener) {
        this.statusListener = listener;
    }
    
    public boolean isInQueue() {
        return isInQueue.get();
    }
    
    public Set<String> getQueuedKits() {
        return new HashSet<>(queuedKits);
    }
    
    public long getQueueTime() {
        if (!isInQueue.get()) return 0;
        return System.currentTimeMillis() - queueStartTime;
    }
    
    /**
     * @param callback Called with true if server is up, false if down
     */
    public void checkServerStatus(Consumer<Boolean> callback) {
        new Thread(() -> {
            try {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress("34.159.92.94", 5000), 3000); 
                socket.close();
                callback.accept(true); 
            } catch (Exception e) {
                callback.accept(false); 
            }
        }).start();
    }
    
    public void joinQueue(Set<String> kits) {
        if (isInQueue.get()) {
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                Text.literal("§c[Queue] §fAlready in queue. Cancel current queue first.")
            );
            return;
        }
        
        queuedKits.clear();
        queuedKits.addAll(kits);
        queueStartTime = System.currentTimeMillis();
        isInQueue.set(true);
        
      
        new Thread(() -> {
            for (String kit : kits) {
                System.out.println("[QueueManager] Sending JOIN: " + kit);
                MatchmakingClient.joinQueue(kit);
            }
        }).start();
        
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
            Text.literal("§a[Queue] §fJoined queue for: " + String.join(", ", kits))
        );
        
        if (statusListener != null) {
            statusListener.onQueueStatusChanged(true, 0, new HashSet<>(kits));
        }
    }
    
    public void leaveQueue() {
        if (!isInQueue.get()) {
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                Text.literal("§e[Queue] §fNot currently in queue.")
            );
            return;
        }
        
        Set<String> kits = new HashSet<>(queuedKits);
        queuedKits.clear();
        isInQueue.set(false);
        queueStartTime = 0;
        
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
            Text.literal("§e[Queue] §fLeft queue for: " + String.join(", ", kits))
        );
        
        if (statusListener != null) {
            statusListener.onQueueStatusChanged(false, 0, new HashSet<>());
        }
    }
    
    public void onMatchFound(String kit) {
        if (isInQueue.get() && queuedKits.contains(kit)) {
            queuedKits.clear();
            isInQueue.set(false);
            queueStartTime = 0;
            
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                Text.literal("§a[Queue] §fMatch found for " + kit + "!")
            );
            
            if (statusListener != null) {
                statusListener.onQueueStatusChanged(false, 0, new HashSet<>());
            }
        }
    }
    
    public String getFormattedQueueTime() {
        long queueTime = getQueueTime();
        if (queueTime == 0) return "0s";
        
        long seconds = queueTime / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        
        if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        } else {
            return seconds + "s";
        }
    }
}
