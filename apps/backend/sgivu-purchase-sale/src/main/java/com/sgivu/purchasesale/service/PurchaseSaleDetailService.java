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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Servicio para transformar entidades de compra-venta en respuestas detalladas, enriqueciendo con
 * datos de clientes, usuarios y vehículos desde servicios remotos.
 */
@Service
public class PurchaseSaleDetailService {

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
    Map<Long, ClientSummary> clientCache = new HashMap<>();
    Map<Long, UserSummary> userCache = new HashMap<>();
    Map<Long, VehicleSummary> vehicleCache = new HashMap<>();

    return contracts.stream()
        .map(
            contract -> {
              PurchaseSaleDetailResponse detail =
                  purchaseSaleMapper.toPurchaseSaleDetailResponse(contract);
              if (contract.getClientId() != null) {
                detail.setClientSummary(
                    clientCache.computeIfAbsent(
                        contract.getClientId(), this::resolveClientSummary));
              }
              if (contract.getUserId() != null) {
                detail.setUserSummary(
                    userCache.computeIfAbsent(contract.getUserId(), this::resolveUserSummary));
              }
              if (contract.getVehicleId() != null) {
                detail.setVehicleSummary(
                    vehicleCache.computeIfAbsent(
                        contract.getVehicleId(), this::resolveVehicleSummary));
              }
              return detail;
            })
        .toList();
  }

  public PurchaseSaleDetailResponse toDetail(PurchaseSale contract) {
    return toDetails(List.of(contract)).stream().findFirst().orElse(null);
  }

  private ClientSummary resolveClientSummary(Long clientId) {
    try {
      Person person = clientServiceClient.getPersonById(clientId);
      return ClientSummary.builder()
          .id(person.getId())
          .type("PERSON")
          .name((person.getFirstName() + " " + person.getLastName()).trim())
          .identifier(
              person.getNationalId() != null ? "CC " + person.getNationalId() : "Persona natural")
          .email(person.getEmail())
          .phoneNumber(person.getPhoneNumber())
          .build();
    } catch (HttpClientErrorException ex) {
      if (ex.getStatusCode() != HttpStatus.NOT_FOUND) {
        throw ex;
      }
    }

    try {
      Company company = clientServiceClient.getCompanyById(clientId);
      return ClientSummary.builder()
          .id(company.getId())
          .type("COMPANY")
          .name(company.getCompanyName())
          .identifier(
              company.getTaxId() != null ? "NIT " + company.getTaxId() : "Empresa registrada")
          .email(company.getEmail())
          .phoneNumber(company.getPhoneNumber())
          .build();
    } catch (HttpClientErrorException ex) {
      if (ex.getStatusCode() != HttpStatus.NOT_FOUND) {
        throw ex;
      }
      return ClientSummary.builder()
          .id(clientId)
          .type("UNKNOWN")
          .name("Cliente no disponible")
          .identifier("ID " + clientId)
          .build();
    }
  }

  private UserSummary resolveUserSummary(Long userId) {
    try {
      User user = userServiceClient.getUserById(userId);
      String fullName = String.format("%s %s", user.getFirstName(), user.getLastName()).trim();
      return UserSummary.builder()
          .id(user.getId())
          .fullName(fullName)
          .email(user.getEmail())
          .username(user.getUsername())
          .build();
    } catch (HttpClientErrorException ex) {
      if (ex.getStatusCode() != HttpStatus.NOT_FOUND) {
        throw ex;
      }
      return UserSummary.builder()
          .id(userId)
          .fullName("Usuario no disponible")
          .username("N/D")
          .build();
    }
  }

  private VehicleSummary resolveVehicleSummary(Long vehicleId) {
    try {
      Car car = vehicleServiceClient.getCarById(vehicleId);
      return VehicleSummary.builder()
          .id(car.getId())
          .type("CAR")
          .brand(car.getBrand())
          .line(car.getLine())
          .model(car.getModel())
          .plate(car.getPlate())
          .status(null)
          .build();
    } catch (HttpClientErrorException ex) {
      if (ex.getStatusCode() != HttpStatus.NOT_FOUND) {
        throw ex;
      }
    }

    try {
      Motorcycle motorcycle = vehicleServiceClient.getMotorcycleById(vehicleId);
      return VehicleSummary.builder()
          .id(motorcycle.getId())
          .type("MOTORCYCLE")
          .brand(motorcycle.getBrand())
          .line(motorcycle.getLine())
          .model(motorcycle.getModel())
          .plate(motorcycle.getPlate())
          .status(null)
          .build();
    } catch (HttpClientErrorException ex) {
      if (ex.getStatusCode() != HttpStatus.NOT_FOUND) {
        throw ex;
      }
      return VehicleSummary.builder()
          .id(vehicleId)
          .type("UNKNOWN")
          .brand("Vehículo no disponible")
          .line("N/D")
          .model("N/D")
          .plate("N/D")
          .status("N/D")
          .build();
    }
  }
}
