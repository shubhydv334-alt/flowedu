package com.flowedu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminBootstrapTest {
    private Repos.UserRepo users;
    private PasswordEncoder encoder;
    private AdminBootstrap bootstrap;

    @BeforeEach
    void setUp() {
        users = mock(Repos.UserRepo.class);
        encoder = mock(PasswordEncoder.class);
        when(encoder.encode(any())).thenAnswer(i -> "hashed:" + i.getArgument(0));
        bootstrap = new AdminBootstrap(users, encoder);
    }

    private void configure(String email, String password) {
        ReflectionTestUtils.setField(bootstrap, "email", email);
        ReflectionTestUtils.setField(bootstrap, "password", password);
    }

    @Test
    void doesNothingWhenNoEmailIsConfigured() {
        configure("", "");
        bootstrap.run();
        verify(users, never()).save(any());
    }

    @Test
    void leavesAnExistingAccountUntouched() {
        configure("Boss@Example.edu", "a-long-enough-password");
        when(users.findByEmail("boss@example.edu")).thenReturn(Optional.of(new Domain.AppUser()));
        bootstrap.run();
        verify(users, never()).save(any());
    }

    @Test
    void refusesShortPasswords() {
        configure("boss@example.edu", "short");
        when(users.findByEmail("boss@example.edu")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> bootstrap.run())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 12");
        verify(users, never()).save(any());
    }

    @Test
    void createsAnAdministratorWithAHashedPassword() {
        configure(" Boss@Example.edu ", "a-long-enough-password");
        when(users.findByEmail("boss@example.edu")).thenReturn(Optional.empty());
        bootstrap.run();
        var saved = ArgumentCaptor.forClass(Domain.AppUser.class);
        verify(users).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("boss@example.edu");
        assertThat(saved.getValue().getRole()).isEqualTo(Domain.Role.ADMIN);
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("hashed:a-long-enough-password");
    }
}
