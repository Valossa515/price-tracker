package io.github.valossa515.pricetracker.publicapi.webhook;

import io.github.valossa515.pricetracker.publicapi.PublicApiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;

/**
 * Validates webhook target URLs before persisting and before delivery.
 *
 * <p>Goals:
 * <ul>
 *   <li>Force HTTPS in production (toggle via {@code app.publicapi.webhook.require-https}).</li>
 *   <li>Block SSRF: refuse hosts that resolve to loopback, link-local, site-local
 *       (RFC1918), multicast or any-local addresses
 *       (toggle via {@code app.publicapi.webhook.allow-private-targets}).</li>
 *   <li>Refuse credentials in URL, non-default ports outside {80, 443}, and
 *       suspicious schemes.</li>
 * </ul>
 */
@Slf4j
@Service
public class WebhookUrlValidator {

    private final boolean requireHttps;
    private final boolean allowPrivateTargets;

    public WebhookUrlValidator(PublicApiProperties properties) {
        this.requireHttps = properties.webhook().requireHttps();
        this.allowPrivateTargets = properties.webhook().allowPrivateTargets();
    }

    /** @throws InvalidWebhookUrlException if the URL is unsafe. */
    public void validate(String url) {
        if (url == null || url.isBlank()) {
            throw new InvalidWebhookUrlException("webhookUrl is required");
        }
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new InvalidWebhookUrlException("webhookUrl is not a valid URI");
        }
        String scheme = uri.getScheme();
        if (scheme == null) {
            throw new InvalidWebhookUrlException("webhookUrl must include a scheme");
        }
        scheme = scheme.toLowerCase(Locale.ROOT);
        if (requireHttps) {
            if (!"https".equals(scheme)) {
                throw new InvalidWebhookUrlException("webhookUrl must use https");
            }
        } else if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw new InvalidWebhookUrlException("webhookUrl must use http or https");
        }
        if (uri.getUserInfo() != null) {
            throw new InvalidWebhookUrlException("webhookUrl must not contain credentials");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new InvalidWebhookUrlException("webhookUrl must include a host");
        }
        int port = uri.getPort();
        if (!allowPrivateTargets && port != -1 && port != 80 && port != 443) {
            throw new InvalidWebhookUrlException("webhookUrl port must be 80 or 443");
        }
        if (!allowPrivateTargets) {
            assertHostIsPublic(host);
        }
    }

    private void assertHostIsPublic(String host) {
        InetAddress[] addrs;
        try {
            addrs = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new InvalidWebhookUrlException("webhookUrl host could not be resolved");
        }
        for (InetAddress addr : addrs) {
            if (addr.isLoopbackAddress()
                    || addr.isAnyLocalAddress()
                    || addr.isLinkLocalAddress()
                    || addr.isSiteLocalAddress()
                    || addr.isMulticastAddress()
                    || isCgnatOrSpecial(addr)) {
                throw new InvalidWebhookUrlException(
                        "webhookUrl host resolves to a private or reserved address");
            }
        }
    }

    /**
     * Catches a few address ranges {@link InetAddress} does not flag as
     * site-local or link-local: the AWS/GCP metadata IP (169.254.169.254 is
     * already link-local) plus the IPv4 carrier-grade NAT block 100.64.0.0/10
     * and the IPv6 unique-local fc00::/7.
     */
    private static boolean isCgnatOrSpecial(InetAddress addr) {
        byte[] b = addr.getAddress();
        if (b.length == 4) {
            int o0 = b[0] & 0xff;
            int o1 = b[1] & 0xff;
            // 100.64.0.0/10
            if (o0 == 100 && (o1 & 0xc0) == 64) return true;
            // 0.0.0.0/8 reserved
            if (o0 == 0) return true;
        } else if (b.length == 16) {
            int first = b[0] & 0xff;
            // fc00::/7 unique-local
            if ((first & 0xfe) == 0xfc) return true;
        }
        return false;
    }

    public static class InvalidWebhookUrlException extends RuntimeException {
        public InvalidWebhookUrlException(String message) {
            super(message);
        }
    }
}
