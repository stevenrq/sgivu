package com.sgivu.purchasesale.service;

import com.sgivu.purchasesale.client.ClientServiceClient;
import com.sgivu.purchasesale.client.UserServiceClient;
import com.sgivu.purchasesale.client.VehicleServiceClient;
import com.sgivu.purchasesale.dto.Car;
import com.sgivu.purchasesale.dto.ClientSummary;
import com.sgivu.purchasesale.dto.Company;
import com.sgivu.purchasesale.dto.Motorcycle;
import com.sgivu.purchasesale.dto.Person;
import com.sgivu.purchasesale.dto.PurchaseSaleDetailResponse;
import com.sgivu.purchasesale.dto.User;
import com.sgivu.purchasesale.dto.UserSummary;
import com.sgivu.purchasesale.dto.VehicleSummary;
import com.sgivu.purchasesale.entity.PurchaseSale;
import com.sgivu.purchasesale.mapper.PurchaseSaleMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Servicio para transformar entidades de compra-venta en respuestas detalladas, enriqueciendo con
 * datos de clientes, usuarios y vehículos desde servicios remotos.
 *
 * <p>Resolución batch: para N contratos con IDs únicos se hacen a lo sumo 5 llamadas HTTP (persons,
 * companies fallback, users, cars, motorcycles fallback) ejecutadas en paralelo por dominio.
 */
@Service
public class PurchaseSaleDetailService {

  private static final String UNKNOWN_VALUE = "UNKNOWN";

  /**
   * Executor que propaga el SecurityContext del hilo de la petición a los hilos del ForkJoinPool,
   * necesario para que las llamadas a servicios remotos incluyan el token de autenticación.
   */
  private static final Executor EXECUTOR =
      new DelegatingSecurityContextExecutor(ForkJoinPool.commonPool());

  private final PurchaseSaleMapper purchaseSaleMapper;
  private final ClientServiceClient clientServiceClient;
  private final UserServiceClient userServiceClient;
  private final VehicleServiceClient vehicleServiceClient;

  public PurchaseSaleDetailService(
      PurchaseSaleMapper purchaseSaleMapper,
      ClientServiceClient clientServiceClient,
      UserServiceClient userServiceClient,
      VehicleServiceClient vehicleServiceClient) {
    this.purchaseSaleMapper = purchaseSaleMapper;
    this.clientServiceClient = clientServiceClient;
    this.userServiceClient = userServiceClient;
    this.vehicleServiceClient = vehicleServiceClient;
  }

  public List<PurchaseSaleDetailResponse> toDetails(List<PurchaseSale> contracts) {
    Set<Long> clientIds = collectIds(contracts, PurchaseSale::getClientId);
    Set<Long> userIds = collectIds(contracts, PurchaseSale::getUserId);
    Set<Long> vehicleIds = collectIds(contracts, PurchaseSale::getVehicleId);

    CompletableFuture<Map<Long, ClientSummary>> clientsFuture =
        CompletableFuture.supplyAsync(() -> resolveClientSummaries(clientIds), EXECUTOR);
    CompletableFuture<Map<Long, UserSummary>> usersFuture =
        CompletableFuture.supplyAsync(() -> resolveUserSummaries(userIds), EXECUTOR);
    CompletableFuture<Map<Long, VehicleSummary>> vehiclesFuture =
        CompletableFuture.supplyAsync(() -> resolveVehicleSummaries(vehicleIds), EXECUTOR);

    Map<Long, ClientSummary> clientCache = clientsFuture.join();
    Map<Long, UserSummary> userCache = usersFuture.join();
    Map<Long, VehicleSummary> vehicleCache = vehiclesFuture.join();

    return contracts.stream()
        .map(
            contract -> {
              PurchaseSaleDetailResponse detail =
                  purchaseSaleMapper.toPurchaseSaleDetailResponse(contract);
              if (contract.getClientId() != null) {
                detail.setClientSummary(clientCache.get(contract.getClientId()));
              }
              if (contract.getUserId() != null) {
                detail.setUserSummary(userCache.get(contract.getUserId()));
              }
              if (contract.getVehicleId() != null) {
                detail.setVehicleSummary(vehicleCache.get(contract.getVehicleId()));
              }
              return detail;
            })
        .toList();
  }

  public PurchaseSaleDetailResponse toDetail(PurchaseSale contract) {
    return toDetails(List.of(contract)).stream().findFirst().orElse(null);
  }

  private Set<Long> collectIds(
      List<PurchaseSale> contracts, Function<PurchaseSale, Long> extractor) {
    return contracts.stream().map(extractor).filter(Objects::nonNull).collect(Collectors.toSet());
  }

  /**
   * Resuelve los resúmenes de cliente en dos pasos: 1) batch a /persons/batch (la mayoría de
   * clientes son personas); 2) los IDs no encontrados se consultan contra /companies/batch. Los IDs
   * restantes quedan marcados como UNKNOWN.
   */
  private Map<Long, ClientSummary> resolveClientSummaries(Set<Long> ids) {
    if (ids.isEmpty()) {
      return Map.of();
    }
    Map<Long, ClientSummary> result = new HashMap<>();
    List<Long> idsList = new ArrayList<>(ids);

    List<Person> persons = safeBatchCall(() -> clientServiceClient.getPersonsByIds(idsList));
    for (Person p : persons) {
      if (p != null && p.getId() != null) {
        result.put(p.getId(), buildPersonSummary(p));
      }
    }

    List<Long> missing = idsList.stream().filter(id -> !result.containsKey(id)).toList();
    if (!missing.isEmpty()) {
      List<Company> companies = safeBatchCall(() -> clientServiceClient.getCompaniesByIds(missing));
      for (Company c : companies) {
        if (c != null && c.getId() != null) {
          result.put(c.getId(), buildCompanySummary(c));
        }
      }
    }

    for (Long id : idsList) {
      result.computeIfAbsent(id, this::buildUnknownClient);
    }
    return result;
  }

  private Map<Long, UserSummary> resolveUserSummaries(Set<Long> ids) {
    if (ids.isEmpty()) {
      return Map.of();
    }
    Map<Long, UserSummary> result = new HashMap<>();
    List<Long> idsList = new ArrayList<>(ids);

    List<User> users = safeBatchCall(() -> userServiceClient.getUsersByIds(idsList));
    for (User u : users) {
      if (u != null && u.getId() != null) {
        result.put(u.getId(), buildUserSummary(u));
      }
    }

    for (Long id : idsList) {
      result.computeIfAbsent(id, this::buildUnknownUser);
    }
    return result;
  }

  private Map<Long, VehicleSummary> resolveVehicleSummaries(Set<Long> ids) {
    if (ids.isEmpty()) {
      return Map.of();
    }
    Map<Long, VehicleSummary> result = new HashMap<>();
    List<Long> idsList = new ArrayList<>(ids);

    List<Car> cars = safeBatchCall(() -> vehicleServiceClient.getCarsByIds(idsList));
    for (Car c : cars) {
      if (c != null && c.getId() != null) {
        result.put(c.getId(), buildCarSummary(c));
      }
    }

    List<Long> missing = idsList.stream().filter(id -> !result.containsKey(id)).toList();
    if (!missing.isEmpty()) {
      List<Motorcycle> motorcycles =
          safeBatchCall(() -> vehicleServiceClient.getMotorcyclesByIds(missing));
      for (Motorcycle m : motorcycles) {
        if (m != null && m.getId() != null) {
          result.put(m.getId(), buildMotorcycleSummary(m));
        }
      }
    }

    for (Long id : idsList) {
      result.computeIfAbsent(id, this::buildUnknownVehicle);
    }
    return result;
  }

  /**
   * Envuelve llamadas batch contra microservicios y devuelve lista vacía en 404, para que la lógica
   * de fallback (persona→empresa, auto→moto) se pueda encadenar limpiamente.
   */
  private <T> List<T> safeBatchCall(Supplier<List<T>> call) {
    try {
      List<T> result = call.get();
      return result == null ? List.of() : result;
    } catch (HttpClientErrorException ex) {
      if (ex.getStatusCode() != HttpStatus.NOT_FOUND) {
        throw ex;
      }
      return List.of();
    }
  }

  private ClientSummary buildPersonSummary(Person person) {
    return ClientSummary.builder()
        .id(person.getId())
        .type("PERSON")
        .name((person.getFirstName() + " " + person.getLastName()).trim())
        .identifier(
            person.getNationalId() != null ? "CC " + person.getNationalId() : "Persona natural")
        .email(person.getEmail())
        .phoneNumber(person.getPhoneNumber())
        .build();
  }

  private ClientSummary buildCompanySummary(Company company) {
    return ClientSummary.builder()
        .id(company.getId())
        .type("COMPANY")
        .name(company.getCompanyName())
        .identifier(company.getTaxId() != null ? "NIT " + company.getTaxId() : "Empresa registrada")
        .email(company.getEmail())
        .phoneNumber(company.getPhoneNumber())
        .build();
  }

  private ClientSummary buildUnknownClient(Long clientId) {
    return ClientSummary.builder()
        .id(clientId)
        .type(UNKNOWN_VALUE)
        .name("Cliente no disponible")
        .identifier("ID " + clientId)
        .build();
  }

  private UserSummary buildUserSummary(User user) {
    String fullName = String.format("%s %s", user.getFirstName(), user.getLastName()).trim();
    return UserSummary.builder()
        .id(user.getId())
        .fullName(fullName)
        .email(user.getEmail())
        .username(user.getUsername())
        .build();
  }

  private UserSummary buildUnknownUser(Long userId) {
    return UserSummary.builder()
        .id(userId)
        .fullName("Usuario no disponible")
        .username("N/D")
        .build();
  }

  private VehicleSummary buildCarSummary(Car car) {
    return VehicleSummary.builder()
        .id(car.getId())
        .type("CAR")
        .brand(car.getBrand())
        .line(car.getLine())
        .model(car.getModel())
        .plate(car.getPlate())
        .status(resolveVehicleStatus(car.getStatus()))
        .build();
  }

  private VehicleSummary buildMotorcycleSummary(Motorcycle motorcycle) {
    return VehicleSummary.builder()
        .id(motorcycle.getId())
        .type("MOTORCYCLE")
        .brand(motorcycle.getBrand())
        .line(motorcycle.getLine())
        .model(motorcycle.getModel())
        .plate(motorcycle.getPlate())
        .status(resolveVehicleStatus(motorcycle.getStatus()))
        .build();
  }

  private VehicleSummary buildUnknownVehicle(Long vehicleId) {
    return VehicleSummary.builder()
        .id(vehicleId)
        .type(UNKNOWN_VALUE)
        .brand("Vehículo no disponible")
        .line("N/D")
        .model("N/D")
        .plate("N/D")
        .status(UNKNOWN_VALUE)
        .build();
  }

  private String resolveVehicleStatus(String status) {
    if (status == null || status.isBlank()) {
      return UNKNOWN_VALUE;
    }
    return status.trim().toUpperCase(Locale.ROOT);
  }
}
