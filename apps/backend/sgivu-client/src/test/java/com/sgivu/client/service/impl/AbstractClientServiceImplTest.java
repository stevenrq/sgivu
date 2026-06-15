package com.sgivu.client.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.sgivu.client.entity.Address;
import com.sgivu.client.entity.Client;
import com.sgivu.client.repository.ClientRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AbstractClientServiceImplTest {

  static class TestClient extends Client {}

  static class TestClientService
      extends AbstractClientServiceImpl<TestClient, ClientRepository<TestClient>> {

    protected TestClientService(ClientRepository<TestClient> clientRepository) {
      super(clientRepository);
    }
  }

  @Mock private ClientRepository<TestClient> clientRepository;

  @InjectMocks private TestClientService service;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    service = new TestClientService(clientRepository);
  }

  @Nested
  @DisplayName("update(Long, T)")
  class UpdateTests {

    @Test
    @DisplayName("Debe actualizar campos del cliente existente y guardar")
    void shouldUpdateExistingClientAndSave() {
      Long id = 1L;
      TestClient existing = new TestClient();
      existing.setId(id);
      existing.setEmail("old@example.com");
      existing.setPhoneNumber(111111111L);
      Address oldAddress = new Address();
      oldAddress.setStreet("Old St");
      existing.setAddress(oldAddress);

      TestClient updated = new TestClient();
      updated.setEmail("new@example.com");
      updated.setPhoneNumber(222222222L);
      Address newAddress = new Address();
      newAddress.setStreet("New St");
      updated.setAddress(newAddress);

      when(clientRepository.findById(id)).thenReturn(Optional.of(existing));
      when(clientRepository.save(any(TestClient.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      Optional<TestClient> result = service.update(id, updated);

      assertTrue(result.isPresent());
      TestClient saved = result.get();
      assertEquals("new@example.com", saved.getEmail());
      assertEquals(222222222L, saved.getPhoneNumber());
      assertEquals("New St", saved.getAddress().getStreet());
      verify(clientRepository).findById(id);
      verify(clientRepository).save(existing);
    }

    @Test
    @DisplayName("Debe retornar Optional vacío cuando el cliente no se encuentra")
    void shouldReturnEmptyWhenNotFound() {
      Long id = 99L;
      TestClient updated = new TestClient();

      when(clientRepository.findById(id)).thenReturn(Optional.empty());

      Optional<TestClient> result = service.update(id, updated);

      assertFalse(result.isPresent());
      verify(clientRepository).findById(id);
      verify(clientRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe propagar excepción cuando falla el guardado del repositorio")
    void shouldPropagateExceptionWhenSaveFails() {
      Long id = 2L;
      TestClient existing = new TestClient();
      existing.setId(id);

      TestClient updated = new TestClient();
      updated.setEmail("x@example.com");

      when(clientRepository.findById(id)).thenReturn(Optional.of(existing));
      when(clientRepository.save(any(TestClient.class)))
          .thenThrow(new RuntimeException("DB error"));

      assertThrows(RuntimeException.class, () -> service.update(id, updated));
      verify(clientRepository).findById(id);
      verify(clientRepository).save(existing);
    }
  }

  @Nested
  @DisplayName("changeStatus(Long, boolean)")
  class ChangeStatusTests {

    @Test
    @DisplayName("Debe habilitar cliente cuando se encuentra")
    void shouldEnableClientWhenFound() {
      Long id = 1L;
      TestClient client = new TestClient();
      client.setId(id);
      client.setEnabled(false);

      when(clientRepository.findById(id)).thenReturn(Optional.of(client));
      when(clientRepository.save(any(TestClient.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      boolean result = service.changeStatus(id, true);

      assertTrue(result);
      assertTrue(client.isEnabled());
      verify(clientRepository).findById(id);
      verify(clientRepository).save(client);
    }

    @Test
    @DisplayName("Debe deshabilitar cliente cuando se encuentra")
    void shouldDisableClientWhenFound() {
      Long id = 2L;
      TestClient client = new TestClient();
      client.setId(id);
      client.setEnabled(true);

      when(clientRepository.findById(id)).thenReturn(Optional.of(client));
      when(clientRepository.save(any(TestClient.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      boolean result = service.changeStatus(id, false);

      assertTrue(result);
      assertFalse(client.isEnabled());
      verify(clientRepository).findById(id);
      verify(clientRepository).save(client);
    }

    @Test
    @DisplayName("Debe retornar false si el cliente no se encuentra")
    void shouldReturnFalseIfClientNotFound() {
      Long id = 99L;

      when(clientRepository.findById(id)).thenReturn(Optional.empty());

      boolean result = service.changeStatus(id, true);
      assertFalse(result);
      verify(clientRepository).findById(id);
      verify(clientRepository, never()).save(any());
    }
  }
}
