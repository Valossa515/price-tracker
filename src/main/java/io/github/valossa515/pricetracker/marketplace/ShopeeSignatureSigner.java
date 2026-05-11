package io.github.valossa515.pricetracker.marketplace;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

/**
 * Builds the Authorization header required by Shopee Affiliate Open API.
 * <p>
 * Format: {@code SHA256 Credential={AppID}, Timestamp={ts}, Signature={sig}}
 * where {@code sig = SHA256(AppID + Timestamp + Payload + Secret)} in hex.
 */
@Component
public class ShopeeSignatureSigner {

    private final String appId;
    private final String appSecret;

    public ShopeeSignatureSigner(
            @Value("${SHOPEE_APP_ID:}") String appId,
            @Value("${SHOPEE_APP_SECRET:}") String appSecret) {
        this.appId = appId;
        this.appSecret = appSecret;
    }

    public boolean isConfigured() {
        return appId != null && !appId.isBlank()
                && appSecret != null && !appSecret.isBlank();
    }

    public String authorizationHeader(String payload) {
        long timestamp = Instant.now().getEpochSecond();
        String base = appId + timestamp + payload + appSecret;
        String signature = sha256Hex(base);
        return "SHA256 Credential=" + appId
                + ", Timestamp=" + timestamp
                + ", Signature=" + signature;
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
