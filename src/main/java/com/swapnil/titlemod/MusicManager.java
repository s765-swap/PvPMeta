package com.swapnil.titlemod;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.text.Text;

import java.util.Random;

public class MusicManager {
    private static boolean musicStarted = false;
    private static String currentTrack = null;
    private static final Random random = new Random();
    
    // Available music tracks
    private static final String[] MUSIC_TRACKS = {
        "menu_theme",
        "background_1", 
        "background_2"
    };
    
    /**
     * Starts playing music with the specified track
     */
    public static void startMusic(String trackName) {
        if (musicStarted) {
            return; // Already playing
        }
        
        try {
            switch (trackName) {
                case "menu_theme":
                    MinecraftClient.getInstance().getSoundManager().play(
                        PositionedSoundInstance.master(ModSounds.MENU_THEME_SOUND_EVENT, 1.0F)
                    );
                    break;
                case "background_1":
                    MinecraftClient.getInstance().getSoundManager().play(
                        PositionedSoundInstance.master(ModSounds.BACKGROUND_MUSIC_1_SOUND_EVENT, 1.0F)
                    );
                    break;
                case "background_2":
                    MinecraftClient.getInstance().getSoundManager().play(
                        PositionedSoundInstance.master(ModSounds.BACKGROUND_MUSIC_2_SOUND_EVENT, 1.0F)
                    );
                    break;
                default:
                    // Fallback to menu theme
                    MinecraftClient.getInstance().getSoundManager().play(
                        PositionedSoundInstance.master(ModSounds.MENU_THEME_SOUND_EVENT, 1.0F)
                    );
                    trackName = "menu_theme";   
            }
            
            musicStarted = true;
            currentTrack = trackName;
            
            // Log the music start
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                Text.literal("§a[Music] §fMUSIC started: " + trackName)
            );
            
        } catch (Exception e) {
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                Text.literal("§c[Music] §fFailed to start music: " + e.getMessage())
            );
        }
    }
    
    /**
     * Starts playing a random music track
     */
    public static void startRandomMusic() {
        String randomTrack = MUSIC_TRACKS[random.nextInt(MUSIC_TRACKS.length)];
        startMusic(randomTrack);
    }
    
    /**
     * Stops the current music
     */
    public static void stopMusic() {
        if (musicStarted) {
            MinecraftClient.getInstance().getSoundManager().stopAll();
            musicStarted = false;
            currentTrack = null;
            
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                Text.literal("§a[Music] §fMUSIC stopped")
            );
        }
    }
    
    /**
     * Checks if music is currently playing
     */
    public static boolean isMusicPlaying() {
        return musicStarted;
    }
    
    /**
     * Gets the current track name
     */
    public static String getCurrentTrack() {
        return currentTrack;
    }
    
    /**
     * Resets the music state (useful for switching screens)
     */
    public static void reset() {
        musicStarted = false;
        currentTrack = null;
    }
}
