package com.sgivu.user.controller;

import com.sgivu.user.controller.api.UserApi;
import com.sgivu.user.dto.ApiResponse;
import com.sgivu.user.dto.UserFilterCriteria;
import com.sgivu.user.dto.UserResponse;
import com.sgivu.user.dto.UserUpdateRequest;
import com.sgivu.user.entity.User;
import com.sgivu.user.mapper.UserMapper;
import com.sgivu.user.service.UserService;
import com.sgivu.user.validation.ValidationService;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RestController;

@RefreshScope
@RestController
public class UserController implements UserApi {

  private final UserService userService;
  private final ValidationService validationService;

  private final UserMapper userMapper;

  public UserController(
      UserService userService, ValidationService validationService, UserMapper userMapper) {
    this.userService = userService;
    this.validationService = validationService;
    this.userMapper = userMapper;
  }

  @Override
  @PreAuthorize("hasAuthority('user:create')")
  public ResponseEntity<ApiResponse<UserResponse>> create(User user, BindingResult bindingResult) {

    ResponseEntity<ApiResponse<UserResponse>> validationResult =
        validationService.handleValidation(user, bindingResult);

    if (validationResult != null) return validationResult;

    User savedUser = userService.save(user);
    UserResponse userResponse = userMapper.toUserResponse(savedUser);
    ApiResponse<UserResponse> apiResponse = new ApiResponse<>(userResponse);

    return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
  }

  @Override
  @PreAuthorize("hasAuthority('user:read')")
  public ResponseEntity<UserResponse> getById(Long id) {
    return userService
        .findById(id)
        .map(user -> ResponseEntity.ok(userMapper.toUserResponse(user)))
        .orElse(ResponseEntity.notFound().build());
  }

  @Override
  public ResponseEntity<UserResponse> getByUsername(String username) {
    return userService
        .findByUsername(username)
        .map(user -> ResponseEntity.ok(userMapper.toUserResponse(user)))
        .orElse(ResponseEntity.notFound().build());
  }

  @Override
  @PreAuthorize("hasAuthority('user:read')")
  public ResponseEntity<List<UserResponse>> getAll() {
    return ResponseEntity.ok(
        userService.findAll().stream().map(userMapper::toUserResponse).toList());
  }

  @Override
  @PreAuthorize("hasAuthority('user:read')")
  public ResponseEntity<Page<UserResponse>> getAllPaginated(Integer page) {
    return ResponseEntity.ok(
        userService.findAll(PageRequest.of(page, 10)).map(userMapper::toUserResponse));
  }

  @Override
  @PreAuthorize("hasAuthority('user:update') or #id.toString() == #authenticatedUserId")
  public ResponseEntity<ApiResponse<UserResponse>> update(
      Long id,
      String authenticatedUserId,
      UserUpdateRequest userUpdateRequest,
      BindingResult bindingResult) {

    ResponseEntity<ApiResponse<UserResponse>> validationResult =
        validationService.handleValidation(userUpdateRequest, bindingResult);

    if (validationResult != null) return validationResult;

    return userService
        .update(id, userUpdateRequest)
        .map(
            user -> {
              UserResponse userResponse = userMapper.toUserResponse(user);
              ApiResponse<UserResponse> apiResponse = new ApiResponse<>(userResponse);
              return ResponseEntity.ok(apiResponse);
            })
        .orElse(ResponseEntity.notFound().build());
  }

  @Override
  @PreAuthorize("hasAuthority('user:delete')")
  public ResponseEntity<Void> deleteById(Long id) {
    Optional<User> userOptional = userService.findById(id);

    if (userOptional.isPresent()) {
      userService.deleteById(id);
      return ResponseEntity.noContent().build();
    }
    return ResponseEntity.notFound().build();
  }

  @Override
  @PreAuthorize("hasAuthority('user:update')")
  public ResponseEntity<Map<String, Boolean>> updateStatus(Long id, boolean enabled) {
    boolean isUpdated = userService.changeStatus(id, enabled);

    if (isUpdated) {
      return ResponseEntity.ok(Collections.singletonMap("User status: ", enabled));
    }
    return ResponseEntity.notFound().build();
  }

  @Override
  public ResponseEntity<Map<String, Long>> getUserCounts() {
    long totalUsers = userService.findAll().size();
    long activeUsers = userService.countActiveUsers();
    long inactiveUsers = totalUsers - activeUsers;

    Map<String, Long> counts = new HashMap<>();
    counts.put("totalUsers", totalUsers);
    counts.put("activeUsers", activeUsers);
    counts.put("inactiveUsers", inactiveUsers);

    return ResponseEntity.ok(counts);
  }

  @Override
  @PreAuthorize("hasAuthority('user:read')")
  public ResponseEntity<List<UserResponse>> searchUsers(
      String name, String username, String email, String role, Boolean enabled) {

    UserFilterCriteria criteria =
        UserFilterCriteria.builder()
            .name(name)
            .username(username)
            .email(email)
            .role(role)
            .enabled(enabled)
            .build();

    List<User> users = userService.search(criteria);
    return ResponseEntity.ok(users.stream().map(userMapper::toUserResponse).toList());
  }

  @Override
  @PreAuthorize("hasAuthority('user:read')")
  public ResponseEntity<Page<UserResponse>> searchUsersPaginated(
      Integer page,
      Integer size,
      String name,
      String username,
      String email,
      String role,
      Boolean enabled) {

    UserFilterCriteria criteria =
        UserFilterCriteria.builder()
            .name(name)
            .username(username)
            .email(email)
            .role(role)
            .enabled(enabled)
            .build();

    Page<UserResponse> responsePage =
        userService.search(criteria, PageRequest.of(page, size)).map(userMapper::toUserResponse);
    return ResponseEntity.ok(responsePage);
  }
}
