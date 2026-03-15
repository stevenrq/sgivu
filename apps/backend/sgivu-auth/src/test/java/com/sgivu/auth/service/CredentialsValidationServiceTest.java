package com.sgivu.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.sgivu.auth.client.UserClient;
import com.sgivu.auth.dto.CredentialsValidationResponse;
import com.sgivu.auth.dto.User;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.HttpClientErrorException;

public class CredentialsValidationServiceTest {

  @Mock private UserClient userClient;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private Tracer tracer;
  @Mock private Span span;
  @Mock private Tracer.SpanInScope spanInScope;

  @InjectMocks private CredentialsValidationService service;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    // tracing mocks para evitar NullPointerExceptions al crear spans
    when(tracer.nextSpan()).thenReturn(span);
    when(span.name(any())).thenReturn(span);
    when(span.start()).thenReturn(span);
    when(tracer.withSpan(span)).thenReturn(spanInScope);
  }

  @Nested
  @DisplayName("validateCredentials(String, String)")
  class ValidateCredentialsTests {

    @Test
    @DisplayName("Debe retornar respuesta válida cuando las credenciales son correctas")
    void shouldReturnValidWhenCredentialsCorrect() {
      User user = new User();
      user.setPassword("encodedPassword");
      user.setEnabled(true);
      user.setAccountNonExpired(true);
      user.setAccountNonLocked(true);
      user.setCredentialsNonExpired(true);

      when(userClient.findByUsername("alice")).thenReturn(user);
      when(passwordEncoder.matches("plain", "encodedPassword")).thenReturn(true);

      CredentialsValidationResponse resp = service.validateCredentials("alice", "plain");

      assertTrue(resp.valid());
      assertEquals("", resp.reason());
      verify(userClient).findByUsername("alice");
    }

    @Test
    @DisplayName("Debe retornar invalid_credentials cuando la contraseña es incorrecta")
    void shouldReturnInvalidWhenPasswordWrong() {
      User user = new User();
      user.setPassword("encodedPassword");
      user.setEnabled(true);
      user.setAccountNonExpired(true);
      user.setAccountNonLocked(true);
      user.setCredentialsNonExpired(true);

      when(userClient.findByUsername("bob")).thenReturn(user);
      when(passwordEncoder.matches("wrong", "encodedPassword")).thenReturn(false);

      CredentialsValidationResponse resp = service.validateCredentials("bob", "wrong");

      assertFalse(resp.valid());
      assertEquals("invalid_credentials", resp.reason());
    }

    @Test
    @DisplayName("Debe retornar invalid_credentials cuando el usuario no se encuentra (404)")
    void shouldReturnInvalidWhenUserNotFound404() {
      HttpClientErrorException.NotFound notFound = mock(HttpClientErrorException.NotFound.class);
      when(userClient.findByUsername("missing")).thenThrow(notFound);

      CredentialsValidationResponse resp = service.validateCredentials("missing", "x");

      assertFalse(resp.valid());
      assertEquals("invalid_credentials", resp.reason());
    }

    @Test
    @DisplayName("Debe retornar invalid_credentials cuando el usuario es nulo")
    void shouldReturnInvalidWhenUserIsNull() {
      when(userClient.findByUsername("nobody")).thenReturn(null);

      CredentialsValidationResponse resp = service.validateCredentials("nobody", "x");

      assertFalse(resp.valid());
      assertEquals("invalid_credentials", resp.reason());
    }

    @Nested
    @DisplayName("status checks")
    class StatusChecks {

      @Test
      @DisplayName("Debe retornar disabled cuando el usuario no está habilitado")
      void shouldReturnDisabledWhenNotEnabled() {
        User user = new User();
        user.setPassword("p");
        user.setEnabled(false);
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);

        when(userClient.findByUsername(any())).thenReturn(user);

        CredentialsValidationResponse resp = service.validateCredentials("u", "x");

        assertFalse(resp.valid());
        assertEquals("disabled", resp.reason());
      }

      @Test
      @DisplayName("Debe retornar expired cuando la cuenta está expirada")
      void shouldReturnExpiredWhenAccountExpired() {
        User user = new User();
        user.setPassword("p");
        user.setEnabled(true);
        user.setAccountNonExpired(false);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);

        when(userClient.findByUsername(any())).thenReturn(user);

        CredentialsValidationResponse resp = service.validateCredentials("u", "x");

        assertFalse(resp.valid());
        assertEquals("expired", resp.reason());
      }

      @Test
      @DisplayName("Debe retornar locked cuando la cuenta está bloqueada")
      void shouldReturnLockedWhenAccountLocked() {
        User user = new User();
        user.setPassword("p");
        user.setEnabled(true);
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(false);
        user.setCredentialsNonExpired(true);

        when(userClient.findByUsername(any())).thenReturn(user);

        CredentialsValidationResponse resp = service.validateCredentials("u", "x");

        assertFalse(resp.valid());
        assertEquals("locked", resp.reason());
      }

      @Test
      @DisplayName("Debe retornar credentials cuando las credenciales están expiradas")
      void shouldReturnCredentialsWhenExpired() {
        User user = new User();
        user.setPassword("p");
        user.setEnabled(true);
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(false);

        when(userClient.findByUsername(any())).thenReturn(user);

        CredentialsValidationResponse resp = service.validateCredentials("u", "x");

        assertFalse(resp.valid());
        assertEquals("credentials", resp.reason());
      }
    }

    @Test
    @DisplayName(
        "Debe retornar unexpected_error cuando el cliente de usuario lanza excepción inesperada")
    void shouldReturnUnexpectedWhenClientThrows() {
      when(userClient.findByUsername("boom")).thenThrow(new RuntimeException("boom"));

      CredentialsValidationResponse resp = service.validateCredentials("boom", "x");

      assertFalse(resp.valid());
      assertEquals("unexpected_error", resp.reason());
    }
  }

  @Nested
  @DisplayName("checkUserStatus(User)")
  class CheckUserStatusTests {

    private CredentialsValidationResponse invokeCheckUserStatus(User user) throws Exception {
      java.lang.reflect.Method m =
          CredentialsValidationService.class.getDeclaredMethod("checkUserStatus", User.class);
      m.setAccessible(true);
      return (CredentialsValidationResponse) m.invoke(service, user);
    }

    @Test
    @DisplayName("Debe retornar null cuando el estado del usuario es OK")
    void shouldReturnNullWhenUserOk() throws Exception {
      User user = new User();
      user.setEnabled(true);
      user.setAccountNonExpired(true);
      user.setAccountNonLocked(true);
      user.setCredentialsNonExpired(true);

      CredentialsValidationResponse resp = invokeCheckUserStatus(user);
      assertNull(resp);
    }

    @Test
    @DisplayName("Debe retornar disabled cuando no está habilitado")
    void shouldReturnDisabledWhenNotEnabled() throws Exception {
      User user = new User();
      user.setEnabled(false);
      user.setAccountNonExpired(true);
      user.setAccountNonLocked(true);
      user.setCredentialsNonExpired(true);

      CredentialsValidationResponse resp = invokeCheckUserStatus(user);
      assertNotNull(resp);
      assertFalse(resp.valid());
      assertEquals("disabled", resp.reason());
    }

    @Test
    @DisplayName("Debe retornar expired cuando la cuenta está expirada")
    void shouldReturnExpiredWhenAccountExpired() throws Exception {
      User user = new User();
      user.setEnabled(true);
      user.setAccountNonExpired(false);
      user.setAccountNonLocked(true);
      user.setCredentialsNonExpired(true);

      CredentialsValidationResponse resp = invokeCheckUserStatus(user);
      assertNotNull(resp);
      assertFalse(resp.valid());
      assertEquals("expired", resp.reason());
    }

    @Test
    @DisplayName("Debe retornar locked cuando la cuenta está bloqueada")
    void shouldReturnLockedWhenAccountLocked() throws Exception {
      User user = new User();
      user.setEnabled(true);
      user.setAccountNonExpired(true);
      user.setAccountNonLocked(false);
      user.setCredentialsNonExpired(true);

      CredentialsValidationResponse resp = invokeCheckUserStatus(user);
      assertNotNull(resp);
      assertFalse(resp.valid());
      assertEquals("locked", resp.reason());
    }

    @Test
    @DisplayName("Debe retornar credentials cuando las credenciales están expiradas")
    void shouldReturnCredentialsWhenCredentialsExpired() throws Exception {
      User user = new User();
      user.setEnabled(true);
      user.setAccountNonExpired(true);
      user.setAccountNonLocked(true);
      user.setCredentialsNonExpired(false);

      CredentialsValidationResponse resp = invokeCheckUserStatus(user);
      assertNotNull(resp);
      assertFalse(resp.valid());
      assertEquals("credentials", resp.reason());
    }
  }
}
