package com.swapnil.titlemod;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {
   
    public static final Identifier MAIN_THEME_IDENTIFIER = new Identifier("titlemod", "music.main_theme");
    public static SoundEvent MAIN_THEME_SOUND_EVENT = SoundEvent.of(MAIN_THEME_IDENTIFIER);

   
    public static final Identifier MENU_THEME_IDENTIFIER = new Identifier("titlemod", "music.menu_theme");
    public static SoundEvent MENU_THEME_SOUND_EVENT = SoundEvent.of(MENU_THEME_IDENTIFIER);
    
    
    public static final Identifier BACKGROUND_MUSIC_1_IDENTIFIER = new Identifier("titlemod", "music.background_1");
    public static SoundEvent BACKGROUND_MUSIC_1_SOUND_EVENT = SoundEvent.of(BACKGROUND_MUSIC_1_IDENTIFIER);
    
    public static final Identifier BACKGROUND_MUSIC_2_IDENTIFIER = new Identifier("titlemod", "music.background_2");
    public static SoundEvent BACKGROUND_MUSIC_2_SOUND_EVENT = SoundEvent.of(BACKGROUND_MUSIC_2_IDENTIFIER);


    public static final Identifier BUTTON_CLICK_IDENTIFIER = new Identifier("titlemod", "ui.button_click");
    public static SoundEvent BUTTON_CLICK_SOUND_EVENT = SoundEvent.of(BUTTON_CLICK_IDENTIFIER);


    public static void registerSounds() {
        
        Registry.register(Registries.SOUND_EVENT, MAIN_THEME_IDENTIFIER, MAIN_THEME_SOUND_EVENT);
        Registry.register(Registries.SOUND_EVENT, MENU_THEME_IDENTIFIER, MENU_THEME_SOUND_EVENT);
        Registry.register(Registries.SOUND_EVENT, BACKGROUND_MUSIC_1_IDENTIFIER, BACKGROUND_MUSIC_1_SOUND_EVENT);
        Registry.register(Registries.SOUND_EVENT, BACKGROUND_MUSIC_2_IDENTIFIER, BACKGROUND_MUSIC_2_SOUND_EVENT);
        Registry.register(Registries.SOUND_EVENT, BUTTON_CLICK_IDENTIFIER, BUTTON_CLICK_SOUND_EVENT);
    }
}
