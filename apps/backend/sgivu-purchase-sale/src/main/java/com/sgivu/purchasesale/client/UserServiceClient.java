package com.sgivu.purchasesale.client;

import com.sgivu.purchasesale.dto.User;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("/v1/users")
public interface UserServiceClient {

  @GetExchange("/{id}")
  User getUserById(@PathVariable Long id);
}
