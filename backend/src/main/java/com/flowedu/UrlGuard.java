package com.flowedu;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

/**
 * Guards outbound webhook calls against server-side request forgery.
 * Webhook URLs are entered by workflow authors, so without this check a workflow could be used to probe
 * localhost, the private network or cloud metadata endpoints (169.254.169.254) from the server.
 * The host is resolved here before the request; a hostile DNS server that changes its answer between this
 * check and the actual connection is not covered, so keep the server on a network with restricted egress too.
 */
final class UrlGuard {
    private UrlGuard() {}

    /** Throws IllegalStateException with a user-readable reason when the URL must not be called. */
    static void checkWebhookUrl(String url, boolean allowPrivate) {
        URI uri;
        try {
            uri = URI.create(url == null ? "" : url.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Webhook URL is not valid");
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new IllegalStateException("Webhook URL must start with http:// or https://");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalStateException("Webhook URL must contain a host name");
        }
        if (uri.getUserInfo() != null) {
            throw new IllegalStateException("Webhook URL must not contain credentials");
        }
        if (allowPrivate) return;

        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new IllegalStateException("Webhook host could not be resolved");
        }
        for (InetAddress a : addresses) {
            if (isInternal(a)) {
                throw new IllegalStateException(
                        "Webhook URL points to a private or internal address (set ALLOW_PRIVATE_WEBHOOKS=true to allow it)");
            }
        }
    }

    static boolean isInternal(InetAddress a) {
        if (a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isLinkLocalAddress()
                || a.isSiteLocalAddress() || a.isMulticastAddress()) {
            return true;
        }
        byte[] b = a.getAddress();
        if (b.length == 4) {
            int first = b[0] & 0xff;
            int second = b[1] & 0xff;
            return first == 0                                   // "this" network
                    || (first == 100 && second >= 64 && second <= 127); // carrier-grade NAT 100.64.0.0/10
        }
        return b.length == 16 && (b[0] & 0xfe) == 0xfc;         // IPv6 unique local fc00::/7
    }
}
