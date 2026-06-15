package com.sgivu.purchasesale.client;

import com.sgivu.purchasesale.dto.User;
import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("/v1/users")
public interface UserServiceClient {

  @GetExchange("/{id}")
  User getUserById(@PathVariable Long id);

  @PostExchange("/batch")
  List<User> getUsersByIds(@RequestBody List<Long> ids);
}
