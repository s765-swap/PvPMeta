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
            
            
            String classpath = System.getProperty("java.class.path");
            return classpath.contains("idea_rt.jar") || classpath.contains("eclipse");
        } catch (Exception e) {
            return false;
        }
    }
    
    public static boolean isValidEnvironment() {
        
        if (isDebugEnvironment()) {
            SecureLogger.logSecurityEvent("Debug environment detected");
            return false;
        }

        
        if (!performSecurityChecks()) {
            SecureLogger.logSecurityEvent("Security checks failed");
            return false;
        }

        
        if (!performIntegrityCheck()) {
            SecureLogger.logSecurityEvent("Integrity check failed");
            return false;
        }

        
        if (detectAnalysisTools()) {
            SecureLogger.logSecurityEvent("Analysis tools detected");
            return false;
        }

        return true;
    }
    
    private static boolean performSecurityChecks() {
        try {
            
            String[] debugTools = {
                "jdwp", "debug", "agent", "jdi", "jdb", "jstack", "jmap", "jhat", "jvisualvm"
            };
            
            String jvmArgs = System.getProperty("sun.java.command", "").toLowerCase();
            for (String tool : debugTools) {
                if (jvmArgs.contains(tool)) {
                    return false;
                }
            }
            
            
            String classpath = System.getProperty("java.class.path", "").toLowerCase();
            if (classpath.contains("idea_rt.jar") || classpath.contains("eclipse") || 
                classpath.contains("netbeans") || classpath.contains("vscode")) {
                return false;
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private static boolean performIntegrityCheck() {
        try {
            
            Class.forName("com.swapnil.titlemod.TitleMod");
            Class.forName("com.swapnil.titlemod.security.SecureIPManager");
            Class.forName("com.swapnil.titlemod.security.HTTPRequestProtector");
            
            
            
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    private static boolean detectAnalysisTools() {
        try {
            
            String[] decompilers = {
                "cfr", "fernflower", "procyon", "jadx", "jd-gui", "recaf", "bytecode-viewer"
            };
            
            String classpath = System.getProperty("java.class.path", "").toLowerCase();
            for (String decompiler : decompilers) {
                if (classpath.contains(decompiler)) {
                    return true;
                }
            }
            
            
            try {
                Class.forName("org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler");
                return true;
            } catch (ClassNotFoundException e) {
                
            }
            
            try {
                Class.forName("com.sun.jdi.VirtualMachine");
                return true;
            } catch (ClassNotFoundException e) {
               
            }
            
            return false;
        } catch (Exception e) {
            return true; 
        }
    }

   
    private static boolean nativeAntiDebugCheck() {
        return true;
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