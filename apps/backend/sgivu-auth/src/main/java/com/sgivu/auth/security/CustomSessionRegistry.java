package com.sgivu.auth.security;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.session.*;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Registro de sesiones que mantiene el mapa principal-sesiones para poder invalidarlas cuando un
 * usuario cambia de estado.
 */
public class CustomSessionRegistry
    implements SessionRegistry, ApplicationListener<AbstractSessionEvent> {

  private final Map<AuthenticatedPrincipal, Set<String>> principals = new ConcurrentHashMap<>();

  private final Map<String, SessionInformation> sessions = new ConcurrentHashMap<>();

  @Override
  public void registerNewSession(String sessionId, Object principal) {
    AuthenticatedPrincipal authenticatedPrincipal = (AuthenticatedPrincipal) principal;
    principals
        .computeIfAbsent(authenticatedPrincipal, ap -> ConcurrentHashMap.newKeySet())
        .add(sessionId);
    sessions.put(sessionId, new SessionInformation(principal, sessionId, new Date()));
  }

  @Override
  public void removeSessionInformation(String sessionId) {
    sessions.remove(sessionId);
    principals.values().forEach(set -> set.remove(sessionId));
  }

  @Override
  public List<Object> getAllPrincipals() {
    return new ArrayList<>(principals.keySet());
  }

  @Override
  public List<SessionInformation> getAllSessions(Object principal, boolean includeExpiredSessions) {
    if (!(principal instanceof UserDetails userDetails)) {
      return Collections.emptyList();
    }
    String principalName = userDetails.getUsername();
    for (Map.Entry<AuthenticatedPrincipal, Set<String>> entry : principals.entrySet()) {
      if (entry.getKey().getName().equals(principalName)) {
        Set<String> sessionIds = entry.getValue();
        List<SessionInformation> result = new ArrayList<>();
        for (String sid : sessionIds) {
          SessionInformation info = sessions.get(sid);
          if (info != null && (includeExpiredSessions || !info.isExpired())) {
            result.add(info);
          }
        }
        return result;
      }
    }
    return Collections.emptyList();
  }

  @Override
  public SessionInformation getSessionInformation(String sessionId) {
    return sessions.get(sessionId);
  }

  @Override
  public void refreshLastRequest(String sessionId) {
    SessionInformation info = sessions.get(sessionId);
    if (info != null) {
      info.refreshLastRequest();
    }
  }

  @Override
  public void onApplicationEvent(@NonNull AbstractSessionEvent event) {
    if (event instanceof SessionDestroyedEvent destroyed) {
      removeSessionInformation(destroyed.getId());
    }
  }
}
