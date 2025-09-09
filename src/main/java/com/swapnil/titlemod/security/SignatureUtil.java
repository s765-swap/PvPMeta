package com.swapnil.titlemod.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;


public final class SignatureUtil {
	public static final String CLIENT_SIGNATURE = "TitleMod-Client-v1.0.0";
	public static final String SIGNING_SECRET = "TitleModSigningSecret2025";

	private SignatureUtil() {}

	public static String generateSignature(String context, String... parts) {
		StringBuilder inputBuilder = new StringBuilder();
		inputBuilder.append(context);
		for (String part : parts) {
			inputBuilder.append("::").append(part == null ? "" : part);
		}
		inputBuilder.append("::").append(CLIENT_SIGNATURE).append("::").append(SIGNING_SECRET);
		
		return java.util.Base64.getEncoder().encodeToString(inputBuilder.toString().getBytes());
	}

	public static String sha256Base64(String input) {
		
		return java.util.Base64.getEncoder().encodeToString(input.getBytes());
	}
}


