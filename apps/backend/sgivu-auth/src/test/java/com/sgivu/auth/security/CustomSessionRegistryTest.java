package com.sgivu.auth.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sgivu.auth.dto.User;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.session.SessionDestroyedEvent;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.userdetails.UserDetails;

class CustomSessionRegistryTest {

  private CustomSessionRegistry registry;

  @BeforeEach
  void setUp() {
    registry = new CustomSessionRegistry();
  }

  @Test
  void getAllSessions_returnsOnlyActiveSessionsWhenRequested() {
    CustomUserDetails principal = principal("alice");
    registry.registerNewSession("s1", principal);
    registry.registerNewSession("s2", principal);

    SessionInformation first = registry.getSessionInformation("s1");
    assertNotNull(first);
    first.expireNow();

    UserDetails lookupPrincipal =
        new org.springframework.security.core.userdetails.User("alice", "pwd", List.of());
    List<SessionInformation> active = registry.getAllSessions(lookupPrincipal, false);
    assertEquals(1, active.size());
    assertEquals("s2", active.getFirst().getSessionId());

    List<SessionInformation> includingExpired = registry.getAllSessions(lookupPrincipal, true);
    assertEquals(2, includingExpired.size());
  }

  @Test
  void onApplicationEvent_removesDestroyedSessions() {
    CustomUserDetails principal = principal("bob");
    MockHttpSession httpSession = new MockHttpSession();
    registry.registerNewSession(httpSession.getId(), principal);

    SessionDestroyedEvent event =
        new SessionDestroyedEvent(httpSession) {
          @Override
          public List<SecurityContext> getSecurityContexts() {
            return List.of();
          }

          @Override
          public String getId() {
            return httpSession.getId();
          }
        };
    registry.onApplicationEvent(event);

    assertNull(registry.getSessionInformation(httpSession.getId()));
    assertTrue(registry.getAllSessions(principal, true).isEmpty());
  }

  private CustomUserDetails principal(String username) {
    User user = new User(99L, username, "secret", true, true, true, true, Set.of());
    return new CustomUserDetails(user, List.of());
  }
}
