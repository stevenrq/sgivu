package com.sgivu.user.service.impl;

import static com.sgivu.user.util.RolePermissionUtils.getRoles;

import com.sgivu.user.dto.UserFilterCriteria;
import com.sgivu.user.dto.UserUpdateRequest;
import com.sgivu.user.entity.User;
import com.sgivu.user.repository.RoleRepository;
import com.sgivu.user.repository.UserRepository;
import com.sgivu.user.service.UserService;
import com.sgivu.user.specification.UserSpecifications;
import com.sgivu.user.util.RolePermissionUtils;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio central de gestión de usuarios.
 *
 * <p>Encapsula reglas de negocio compartidas entre el Gateway y el Authorization Server: resolución
 * de roles solicitados, codificación de credenciales y construcción de consultas dinámicas para
 * inventario de usuarios (altas/bajas, bloqueos y búsquedas multi-criterio).
 */
@Service
public class UserServiceImpl extends AbstractPersonServiceImpl<User, UserRepository>
    implements UserService {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;

  public UserServiceImpl(
      UserRepository userRepository,
      RoleRepository roleRepository,
      PasswordEncoder passwordEncoder) {
    super(userRepository);
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
    this.passwordEncoder = passwordEncoder;
  }

  /** {@inheritDoc} */
  @Override
  @Transactional
  public User save(User user) {
    // Normaliza roles solicitados por el cliente contra el catálogo persistido.
    user.setRoles(getRoles(user, roleRepository));
    user.setPassword(passwordEncoder.encode(user.getPassword()));
    return userRepository.save(user);
  }

  /** {@inheritDoc} */
  @Override
  @Transactional(readOnly = true)
  public Optional<User> findById(Long id) {
    return userRepository.findById(id);
  }

  /** {@inheritDoc} */
  @Override
  @Transactional(readOnly = true)
  public Optional<User> findByUsername(String username) {
    return userRepository.findByUsername(username);
  }

  /** {@inheritDoc} */
  @Override
  @Transactional(readOnly = true)
  public List<User> findAll() {
    return userRepository.findAll();
  }

  /** {@inheritDoc} */
  @Override
  @Transactional(readOnly = true)
  public Page<User> findAll(Pageable pageable) {
    return userRepository.findAll(pageable);
  }

  @Transactional
  public Optional<User> update(Long id, UserUpdateRequest userUpdateRequest) {
    Optional<User> userOptional = userRepository.findById(id);

    if (userOptional.isPresent()) {
      User userToUpdate = userOptional.orElseThrow();

      userToUpdate.setFirstName(userUpdateRequest.getFirstName());
      userToUpdate.setLastName(userUpdateRequest.getLastName());
      userToUpdate.setAddress(userUpdateRequest.getAddress());
      userToUpdate.setPhoneNumber(userUpdateRequest.getPhoneNumber());
      userToUpdate.setEmail(userUpdateRequest.getEmail());
      userToUpdate.setUsername(userUpdateRequest.getUsername());
      // Rehidrata roles desde el catálogo para evitar asignaciones inexistentes al actualizar.
      userToUpdate.setRoles(RolePermissionUtils.getRoles(userToUpdate, roleRepository));
      if (userUpdateRequest.getPassword() != null && !userUpdateRequest.getPassword().isEmpty()) {
        userToUpdate.setPassword(passwordEncoder.encode(userUpdateRequest.getPassword()));
      }

      return Optional.of(userRepository.save(userToUpdate));
    }
    return Optional.empty();
  }

  /** {@inheritDoc} */
  @Override
  @Transactional
  public void deleteById(Long id) {
    userRepository.deleteById(id);
  }

  @Override
  @Transactional
  public boolean changeStatus(Long id, boolean isEnabled) {
    Optional<User> userOptional = userRepository.findById(id);

    if (userOptional.isPresent()) {
      User user = userOptional.orElseThrow();

      user.setEnabled(isEnabled);
      userRepository.save(user);
      return true;
    }
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public long countActiveUsers() {
    return userRepository.countByIsEnabled(true);
  }

  /** {@inheritDoc} */
  @Override
  public List<User> findByFirstNameOrLastName(String name) {
    return userRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
        name, name);
  }

  /**
   * Busca usuarios aplicando los filtros declarativos.
   *
   * @param criteria filtros como nombre, rol, estado
   * @return lista de coincidencias
   */
  @Override
  public List<User> search(UserFilterCriteria criteria) {
    return search(criteria, Pageable.unpaged()).getContent();
  }

  /**
   * Variante paginada de {@link #search(UserFilterCriteria)}.
   *
   * @param criteria filtros
   * @param pageable configuración de paginación
   * @return página de usuarios
   */
  @Transactional(readOnly = true)
  public Page<User> search(UserFilterCriteria criteria, Pageable pageable) {
    return userRepository.findAll(UserSpecifications.withFilters(criteria), pageable);
  }
}
