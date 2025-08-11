# ProGuard Rules for TitleMod - No Obfuscation

# Keep essential Minecraft and Fabric classes
-keep class net.minecraft.client.** { *; }
-keep class net.fabricmc.api.** { *; }
-keep class com.mojang.blaze3d.** { *; }
-keep class com.mojang.serialization.** { *; }

# Keep entry points and API
-keep class com.swapnil.titlemod.TitleMod { *; }
-keep class com.swapnil.titlemod.config.ModConfig { *; }
-keep class * implements net.fabricmc.api.ModInitializer { *; }
-keep class * implements net.fabricmc.api.ClientModInitializer { *; }

# Keep mixin classes and their targets
-keep class com.swapnil.titlemod.mixin.** { *; }
-keepclasseswithmembers class * {
    @org.spongepowered.asm.mixin.** *;
}

# Keep network, data, util, security, server, client
-keep class com.swapnil.titlemod.network.** { *; }
-keep class com.swapnil.titlemod.data.** { *; }
-keep class com.swapnil.titlemod.util.** { *; }
-keep class com.swapnil.titlemod.security.** { *; }
-keep class com.swapnil.titlemod.server.** { *; }
-keep class com.swapnil.titlemod.client.** { *; }

# Keep lambda expressions and functional interfaces
-keepclassmembers class * {
    @java.lang.FunctionalInterface *;
}

# Keep Gson and JSON related classes
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.** { *; }

# Keep logging classes
-keep class org.apache.logging.log4j.** { *; }

# Keep serialization
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
}

# Keep enum values
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep main methods
-keepclassmembers class * {
    public static void main(java.lang.String[]);
}

# Keep attributes for debugging
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable,LocalVariableTable,LocalVariableTypeTable
