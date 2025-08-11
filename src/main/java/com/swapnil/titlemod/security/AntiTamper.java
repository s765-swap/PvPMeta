package com.swapnil.titlemod.security;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Arrays;
import java.util.List;

public class AntiTamper {
    private static final String[] DECOY_IPS = {
        "127.0.0.1", "192.168.1.1", "10.0.0.1", "172.16.0.1", "203.0.113.1"
    };
    
    private static final List<String> DEBUG_INDICATORS = Arrays.asList(
        "-agentlib:jdwp", "-Xdebug", "-Xrunjdwp", "-javaagent", "-verbose", "-Didea.launcher.bin.path"
    );
    
    public static boolean isDebugEnvironment() {
        try {
            RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
            String jvmArgs = runtimeMXBean.getInputArguments().toString().toLowerCase();
            
            for (String indicator : DEBUG_INDICATORS) {
                if (jvmArgs.contains(indicator.toLowerCase())) {
                    return true;
                }
            }
            
            // Check for IDE debug mode
            String classpath = System.getProperty("java.class.path");
            return classpath.contains("idea_rt.jar") || classpath.contains("eclipse");
        } catch (Exception e) {
            return false;
        }
    }
    
    public static boolean isValidEnvironment() {
        if (isDebugEnvironment()) {
            return false;
        }

        // Native anti-debugger check (JNI)
        try {
            if (!nativeAntiDebugCheck()) {
                return false;
            }
        } catch (Throwable t) {
            // If native check fails, be suspicious
            return false;
        }

        // Runtime hash self-check for this class
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            java.io.InputStream is = AntiTamper.class.getResourceAsStream("/com/swapnil/titlemod/security/AntiTamper.class");
            if (is != null) {
                byte[] buf = new byte[4096];
                int len;
                while ((len = is.read(buf)) > 0) {
                    md.update(buf, 0, len);
                }
                is.close();
                byte[] hash = md.digest();
                // Replace with your expected hash (update after every build!)
                String expected = "REPLACE_WITH_EXPECTED_HASH";
                StringBuilder sb = new StringBuilder();
                for (byte b : hash) sb.append(String.format("%02x", b));
                if (!sb.toString().equals(expected)) {
                    return false;
                }
            }
        } catch (Exception e) {
            return false;
        }

        // Check for more decompilers
        String[] moreDecompilers = {"cfr", "fernflower", "procyon", "jadx", "jd-gui"};
        String classpath = System.getProperty("java.class.path", "");
        for (String dc : moreDecompilers) {
            if (classpath.toLowerCase().contains(dc)) {
                return false;
            }
        }

        try {
            // Check for decompiler presence
            Class.forName("org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler");
            return false;
        } catch (ClassNotFoundException e) {
            // Expected
        }
        try {
            // Check for debug tools
            Class.forName("com.sun.jdi.VirtualMachine");
            return false;
        } catch (ClassNotFoundException e) {
            // Expected
        }
        return true;
    }

    // Native anti-debugger stub (JNI)
    private static native boolean nativeAntiDebugCheck();
    static {
        try {
            System.loadLibrary("titlemod_antidebug");
        } catch (Throwable t) {
            // Ignore, will fail safe in isValidEnvironment
        }
    }
    
    public static String getDecoyIP(int seed) {
        return DECOY_IPS[Math.abs(seed) % DECOY_IPS.length];
    }
    
    public static void triggerSecurityResponse() {
        if (!isValidEnvironment()) {
            System.exit(0);
        }
    }
    
    public static void selfDestruct() {
        Runtime.getRuntime().gc();
    }
}