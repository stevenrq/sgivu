package com.sgivu.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Answers.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sgivu.auth.client.UserClient;
import com.sgivu.auth.dto.Permission;
import com.sgivu.auth.dto.Role;
import com.sgivu.auth.dto.User;
import com.sgivu.auth.exception.ServiceUnavailableException;
import com.sgivu.auth.security.CustomUserDetails;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.client.HttpClientErrorException;

@ExtendWith(MockitoExtension.class)
class JpaUserDetailsServiceTest {

  @Mock private UserClient userClient;

  @Mock private Tracer tracer;

  @Mock private Tracer.SpanInScope spanInScope;

  private Span span;

  private JpaUserDetailsService service;

  @BeforeEach
  void setUp() {
    span = mock(Span.class, RETURNS_SELF);
    when(tracer.nextSpan()).thenReturn(span);
    when(tracer.withSpan(span)).thenReturn(spanInScope);

    service = new JpaUserDetailsService(userClient, tracer);
  }

  @Test
  void loadUserByUsername_whenUserExists_returnsCustomUserDetails() {
    User user = buildUser();
    when(userClient.findByUsername("jdoe")).thenReturn(user);
    when(tracer.currentSpan()).thenReturn(span);

    UserDetails result = service.loadUserByUsername("jdoe");

    assertTrue(result instanceof CustomUserDetails);
    CustomUserDetails details = (CustomUserDetails) result;
    assertEquals(user.getId(), details.getId());
    assertEquals("jdoe", details.getUsername());
    assertTrue(
        result
            .getAuthorities()
            .containsAll(
                List.of(
                    new SimpleGrantedAuthority("ADMIN"),
                    new SimpleGrantedAuthority("inventory:read"))));
    verify(span).tag("authentication.username", "jdoe");
  }

  @Test
  void loadUserByUsername_whenUserIsNull_throwsUsernameNotFoundException() {
    when(userClient.findByUsername("missing")).thenReturn(null);

    assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("missing"));
  }

  @Test
  void loadUserByUsername_whenUserClientReturns404_throwsUsernameNotFoundException() {
    HttpClientErrorException notFound =
        HttpClientErrorException.create(
            org.springframework.http.HttpStatus.NOT_FOUND,
            "not found",
            HttpHeaders.EMPTY,
            new byte[0],
            StandardCharsets.UTF_8);
    when(userClient.findByUsername("missing")).thenThrow(notFound);

    assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("missing"));
  }

  @Test
  void fallbackFetchUser_alwaysThrowsServiceUnavailableException() throws Exception {
    Method fallback =
        JpaUserDetailsService.class.getDeclaredMethod(
            "fallbackFetchUser", String.class, Throwable.class);
    fallback.setAccessible(true);

    InvocationTargetException thrown =
        assertThrows(
            InvocationTargetException.class,
            () -> fallback.invoke(service, "jdoe", new RuntimeException("boom")));

    assertTrue(thrown.getCause() instanceof ServiceUnavailableException);
  }

  private User buildUser() {
    Permission readInventory = new Permission();
    readInventory.setId(10L);
    readInventory.setName("inventory:read");

    Role admin = new Role();
    admin.setId(5L);
    admin.setName("ADMIN");
    admin.setPermissions(Set.of(readInventory));

    return new User(1L, "jdoe", "password", true, true, true, true, Set.of(admin));
  }
}
