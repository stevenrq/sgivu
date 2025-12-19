package com.sgivu.purchasesale.client;

import com.sgivu.purchasesale.dto.Company;
import com.sgivu.purchasesale.dto.Person;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/** Cliente HTTP para consultar datos maestros de clientes (personas y empresas). */
@HttpExchange("/v1")
public interface ClientServiceClient {

  /**
   * Obtiene un cliente persona natural desde el microservicio de clientes.
   *
   * @param id identificador del cliente
   * @return datos de persona
   */
  @GetExchange("/persons/{id}")
  Person getPersonById(@PathVariable Long id);

  /**
   * Obtiene una empresa desde el microservicio de clientes.
   *
   * @param id identificador de la empresa
   * @return datos de empresa
   */
  @GetExchange("/companies/{id}")
  Company getCompanyById(@PathVariable Long id);
}
