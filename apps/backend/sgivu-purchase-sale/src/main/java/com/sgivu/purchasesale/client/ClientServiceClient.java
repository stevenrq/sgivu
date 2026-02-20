package com.sgivu.purchasesale.client;

import com.sgivu.purchasesale.dto.Company;
import com.sgivu.purchasesale.dto.Person;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("/v1")
public interface ClientServiceClient {

  @GetExchange("/persons/{id}")
  Person getPersonById(@PathVariable Long id);

  @GetExchange("/companies/{id}")
  Company getCompanyById(@PathVariable Long id);
}
