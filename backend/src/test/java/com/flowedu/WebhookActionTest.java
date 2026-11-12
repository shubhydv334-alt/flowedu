package com.flowedu;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebhookActionTest {
    private static final Map<String, Object> CTX = Map.of();

    @Test
    void refusesInternalTargetsOnLiveRuns() {
        var action = new Actions.WebhookAction(false);
        Map<String, Object> cfg = Map.of("url", "http://169.254.169.254/latest/meta-data", "body", "{}");
        assertThatThrownBy(() -> action.run(cfg, CTX, Action.Options.live()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("private or internal");
    }

    @Test
    void previewRendersTheRequestWithoutValidatingOrSending() {
        var action = new Actions.WebhookAction(false);
        Map<String, Object> cfg = Map.of("url", "http://localhost/hook", "body", "{\"a\":1}");
        var out = action.run(cfg, CTX, new Action.Options(true, false, null, null));
        assertThat(out).containsEntry("url", "http://localhost/hook").containsEntry("method", "POST");
    }

    @Test
    void sendTestToMeSkipsTheExternalCall() {
        var action = new Actions.WebhookAction(false);
        Map<String, Object> cfg = Map.of("url", "https://8.8.8.8/hook", "body", "{}");
        var out = action.run(cfg, CTX, new Action.Options(false, true, "me@example.edu", null));
        assertThat(out).containsEntry("simulated", true);
    }
}
