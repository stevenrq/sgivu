package com.sgivu.purchasesale.client;

import com.sgivu.purchasesale.dto.Company;
import com.sgivu.purchasesale.dto.Person;
import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("/v1")
public interface ClientServiceClient {

  @GetExchange("/persons/{id}")
  Person getPersonById(@PathVariable Long id);

  @GetExchange("/companies/{id}")
  Company getCompanyById(@PathVariable Long id);

  @PostExchange("/persons/batch")
  List<Person> getPersonsByIds(@RequestBody List<Long> ids);

  @PostExchange("/companies/batch")
  List<Company> getCompaniesByIds(@RequestBody List<Long> ids);
}
