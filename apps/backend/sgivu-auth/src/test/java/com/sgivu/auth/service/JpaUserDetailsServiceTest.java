package com.sgivu.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.sgivu.auth.client.UserClient;
import com.sgivu.auth.dto.Permission;
import com.sgivu.auth.dto.Role;
import com.sgivu.auth.dto.User;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.client.HttpClientErrorException;

public class JpaUserDetailsServiceTest {

  @Mock private UserClient userClient;
  @Mock private Tracer tracer;
  @Mock private Span span;
  @Mock private Span currentSpan;
  @Mock private Tracer.SpanInScope spanInScope;

  @InjectMocks private JpaUserDetailsService service;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    when(tracer.nextSpan()).thenReturn(span);
    when(span.name(any())).thenReturn(span);
    when(span.start()).thenReturn(span);
    when(tracer.withSpan(span)).thenReturn(spanInScope);
    when(tracer.currentSpan()).thenReturn(currentSpan);
  }

  @Nested
  @DisplayName("loadUserByUsername(String)")
  class LoadUserByUsernameTests {

    @Test
    @DisplayName("Debe cargar usuario, mapear autoridades y etiquetar span")
    void shouldLoadUserAndMapAuthoritiesAndTagSpan() {
      User user = new User();
      user.setId(10L);
      user.setUsername("alice");
      user.setPassword("pwd");
      user.setEnabled(true);
      user.setAccountNonExpired(true);
      user.setAccountNonLocked(true);
      user.setCredentialsNonExpired(true);

      Role r = new Role();
      r.setName("ADMIN");
      Permission p = new Permission();
      p.setName("user:read");
      Set<Permission> perms = new HashSet<>();
      perms.add(p);
      r.setPermissions(perms);

      Set<Role> roles = new HashSet<>();
      roles.add(r);
      user.setRoles(roles);

      when(userClient.findByUsername("alice")).thenReturn(user);

      var details = service.loadUserByUsername("alice");

      assertNotNull(details);
      assertTrue(details instanceof com.sgivu.auth.security.CustomUserDetails);
      var custom = (com.sgivu.auth.security.CustomUserDetails) details;
      assertEquals("alice", custom.getUsername());
      assertEquals("pwd", custom.getPassword());
      assertEquals(10L, custom.getId());
      assertTrue(custom.getAuthorities().stream().anyMatch(a -> "ADMIN".equals(a.getAuthority())));
      assertTrue(
          custom.getAuthorities().stream().anyMatch(a -> "user:read".equals(a.getAuthority())));

      verify(currentSpan).tag("authentication.event", "usuario_encontrado");
      verify(currentSpan).tag("authentication.username", "alice");
      verify(currentSpan).tag("authentication.user_id", "10");
      verify(currentSpan).event("Registro de usuario cargado desde el servicio de usuarios");
    }

    @Test
    @DisplayName("Debe lanzar UsernameNotFoundException cuando el cliente de usuario retorna null")
    void shouldThrowWhenUserClientReturnsNull() {
      when(userClient.findByUsername("nope")).thenReturn(null);
      assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("nope"));
    }

    @Test
    @DisplayName("Debe lanzar UsernameNotFoundException cuando el cliente retorna 404")
    void shouldThrowWhenClientReturns404() {
      HttpClientErrorException.NotFound nf = mock(HttpClientErrorException.NotFound.class);
      when(userClient.findByUsername("missing")).thenThrow(nf);
      assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("missing"));
    }

    @Test
    @DisplayName("Debe propagar excepciones de runtime del cliente")
    void shouldPropagateRuntimeExceptionFromClient() {
      when(userClient.findByUsername("boom")).thenThrow(new RuntimeException("boom"));
      assertThrows(RuntimeException.class, () -> service.loadUserByUsername("boom"));
    }
  }

  @Nested
  @DisplayName("mapToGrantedAuthorities(User)")
  class MapToGrantedAuthoritiesTests {

    @SuppressWarnings("unchecked")
    private java.util.Set<org.springframework.security.core.GrantedAuthority>
        invokeMapToGrantedAuthorities(User user) throws Exception {
      java.lang.reflect.Method m =
          JpaUserDetailsService.class.getDeclaredMethod("mapToGrantedAuthorities", User.class);
      m.setAccessible(true);
      return (java.util.Set<org.springframework.security.core.GrantedAuthority>)
          m.invoke(service, user);
    }

    @Test
    @DisplayName("Debe mapear roles y permisos a conjunto de GrantedAuthority")
    void shouldMapRolesAndPermissions() throws Exception {
      User user = new User();
      Role r = new Role();
      r.setName("ADMIN");
      Permission p = new Permission();
      p.setName("user:read");
      java.util.Set<Permission> perms = new java.util.HashSet<>();
      perms.add(p);
      r.setPermissions(perms);

      java.util.Set<Role> roles = new java.util.HashSet<>();
      roles.add(r);
      user.setRoles(roles);

      java.util.Set<org.springframework.security.core.GrantedAuthority> result =
          invokeMapToGrantedAuthorities(user);

      assertNotNull(result);
      assertTrue(result.stream().anyMatch(a -> "ADMIN".equals(a.getAuthority())));
      assertTrue(result.stream().anyMatch(a -> "user:read".equals(a.getAuthority())));
      assertEquals(2, result.size());
    }

    @Test
    @DisplayName("Debe retornar conjunto vacío cuando no hay roles")
    void shouldReturnEmptyWhenNoRoles() throws Exception {
      User user = new User();
      user.setRoles(new java.util.HashSet<>());

      java.util.Set<org.springframework.security.core.GrantedAuthority> result =
          invokeMapToGrantedAuthorities(user);
      assertNotNull(result);
      assertTrue(result.isEmpty());
    }
  }
}
