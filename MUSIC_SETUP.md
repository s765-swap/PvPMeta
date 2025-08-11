   # Music Setup Guide

## How to Add Your Own Music

Since direct YouTube streaming isn't possible in Minecraft mods, here's how to add your own music files:

### 1. Convert YouTube Music to OGG Format

1. **Download the audio** from YouTube using a tool like yt-dlp or a YouTube to MP3 converter
2. **Convert to OGG format** using tools like:
   - FFmpeg: `ffmpeg -i input.mp3 -c:a libvorbis -q:a 4 output.ogg`
   - Online converters
   - Audacity (free audio editor)

### 2. Add Music Files to the Mod

Place your OGG files in the following locations:

```
src/main/resources/assets/titlemod/sounds/music/
├── menu_theme.ogg          (Main menu music)
├── background_1.ogg        (Background music track 1)
├── background_2.ogg        (Background music track 2)
└── main_theme.ogg          (Original theme)
```

### 3. Available Music Tracks

The mod now supports multiple music tracks:

- **menu_theme** - Plays on the main title screen
- **background_1** - Additional background track
- **background_2** - Additional background track
- **main_theme** - Original theme (legacy)

### 4. Music Controls

The new `MusicManager` class provides these features:

- **Automatic music start** when entering the title screen
- **Track logging** - Shows which track is playing in chat
- **Error handling** - Graceful fallback if music files are missing
- **Multiple track support** - Easy to add more tracks

### 5. For the YouTube Video You Mentioned

For the video at https://www.youtube.com/watch?v=L5Kwhd5FjpQ:

1. Download the audio from that video
2. Convert it to OGG format
3. Save it as `menu_theme.ogg` in the music folder
4. The mod will automatically play it when you open the title screen

### 6. Adding More Tracks

To add more music tracks:

1. Add the OGG file to the music folder
2. Add a new entry in `ModSounds.java`:
   ```java
   public static final Identifier NEW_TRACK_IDENTIFIER = new Identifier("titlemod", "music.new_track");
   public static SoundEvent NEW_TRACK_SOUND_EVENT = SoundEvent.of(NEW_TRACK_IDENTIFIER);
   ```
3. Register it in the `registerSounds()` method
4. Add it to `sounds.json`
5. Add it to the `MUSIC_TRACKS` array in `MusicManager.java`

### 7. Music Quality Tips

- Use **OGG Vorbis** format for best compatibility
- Keep file sizes reasonable (under 10MB per track)
- Use **44.1kHz** sample rate for good quality
- **128-192kbps** bitrate is usually sufficient

### 8. Troubleshooting

- **No music playing**: Check that OGG files are in the correct location
- **Compilation errors**: Ensure all sound events are registered
- **File not found**: Verify the file paths in `sounds.json` match your actual files

The mod will show helpful messages in chat when music starts or if there are any issues!
