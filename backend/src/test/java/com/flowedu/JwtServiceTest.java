package com.flowedu;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {
    private static final String SECRET = "unit-test-secret-unit-test-secret-0123456789";
    private static final String PLACEHOLDER = "change-this-secret-change-this-secret-123456";

    private static Domain.AppUser user() {
        return Domain.AppUser.builder().email("ann@example.edu").name("Ann").role(Domain.Role.TEACHER).build();
    }

    @Test
    void tokenRoundTripKeepsSubjectAndRole() {
        JwtService jwt = new JwtService(SECRET, 5, new MockEnvironment());
        var claims = jwt.parse(jwt.create(user()));
        assertThat(claims.getSubject()).isEqualTo("ann@example.edu");
        assertThat(claims.get("role", String.class)).isEqualTo("TEACHER");
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() {
        JwtService mine = new JwtService(SECRET, 5, new MockEnvironment());
        JwtService other = new JwtService("a-different-secret-a-different-secret-0123", 5, new MockEnvironment());
        assertThatThrownBy(() -> mine.parse(other.create(user()))).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsExpiredToken() {
        JwtService jwt = new JwtService(SECRET, -1, new MockEnvironment());
        assertThatThrownBy(() -> jwt.parse(jwt.create(user()))).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void rejectsSecretsShorterThan32Characters() {
        assertThatThrownBy(() -> new JwtService("too-short", 5, new MockEnvironment()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 characters");
    }

    @Test
    void rejectsPlaceholderSecretInProd() {
        MockEnvironment prod = new MockEnvironment();
        prod.setActiveProfiles("prod");
        assertThatThrownBy(() -> new JwtService(PLACEHOLDER, 5, prod))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("placeholder");
    }

    @Test
    void toleratesPlaceholderSecretOutsideProd() {
        assertThatCode(() -> new JwtService(PLACEHOLDER, 5, new MockEnvironment())).doesNotThrowAnyException();
    }
}
