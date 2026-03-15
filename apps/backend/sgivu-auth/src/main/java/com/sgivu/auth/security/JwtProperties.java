package com.sgivu.auth.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sgivu.jwt")
public record JwtProperties(KeyStore keyStore, Key key) {

  public record KeyStore(String location, String password) {}

  public record Key(String alias, String password) {}
}
