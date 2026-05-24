package com.sgivu.gateway.dto;

import java.util.List;

public record AuthSessionResponse(
    boolean authenticated,
    String userId,
    String username,
    List<String> rolesAndPermissions,
    boolean isAdmin) {}
