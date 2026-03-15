package com.sgivu.auth.client;

import com.sgivu.auth.dto.User;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("/v1/users")
public interface UserClient {

  @GetExchange("/username/{username}")
  User findByUsername(@PathVariable String username);
}
