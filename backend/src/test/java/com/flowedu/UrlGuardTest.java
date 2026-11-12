package com.flowedu;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UrlGuardTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "http://127.0.0.1/hook",
            "http://localhost:8080/hook",
            "http://0.0.0.0/",
            "http://10.1.2.3/",
            "http://172.16.0.1/",
            "http://192.168.1.10/",
            "http://169.254.169.254/latest/meta-data",
            "http://100.64.0.1/",
            "http://[::1]/",
            "http://[fd00::1]/"
    })
    void blocksInternalAddresses(String url) {
        assertThatThrownBy(() -> UrlGuard.checkWebhookUrl(url, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("private or internal");
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://8.8.8.8/hook", "https://1.1.1.1/"})
    void allowsPublicAddresses(String url) {
        assertThatCode(() -> UrlGuard.checkWebhookUrl(url, false)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ftp://8.8.8.8/", "file:///etc/passwd", "8.8.8.8", ""})
    void rejectsOtherSchemes(String url) {
        assertThatThrownBy(() -> UrlGuard.checkWebhookUrl(url, false))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsEmbeddedCredentials() {
        assertThatThrownBy(() -> UrlGuard.checkWebhookUrl("https://user:secret@8.8.8.8/", false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("credentials");
    }

    @Test
    void allowsPrivateAddressesWhenExplicitlyEnabled() {
        assertThatCode(() -> UrlGuard.checkWebhookUrl("http://localhost:8080/hook", true)).doesNotThrowAnyException();
    }
}
