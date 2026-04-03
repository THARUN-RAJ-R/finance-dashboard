package com.finance.dashboard.user.service;

import com.finance.dashboard.audit.service.AuditService;
import com.finance.dashboard.common.exception.ApiException;
import com.finance.dashboard.common.model.PageResponse;
import com.finance.dashboard.user.dto.CreateUserRequest;
import com.finance.dashboard.user.dto.UpdateUserRequest;
import com.finance.dashboard.user.dto.UserResponse;
import com.finance.dashboard.user.entity.RoleEntity;
import com.finance.dashboard.user.entity.UserEntity;
import com.finance.dashboard.user.entity.UserStatus;
import com.finance.dashboard.user.repository.RoleRepository;
import com.finance.dashboard.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * User management business logic.
 * All mutating operations are audited via {@link AuditService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository  userRepository;
    private final RoleRepository  roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService    auditService;

    // ── Create ─────────────────────────────────────────────────

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw ApiException.conflict(
                    "A user with email '" + request.getEmail() + "' already exists");
        }

        Set<RoleEntity> roles = roleRepository.findByNameIn(request.getRoles());
        
        if (roles.size() != request.getRoles().size()) {
            throw ApiException.badRequest("One or more specified roles do not exist");
        }

        UserEntity user = UserEntity.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .status(UserStatus.ACTIVE)
                .roles(roles)
                .build();

        user = userRepository.save(user);

        auditService.log("USER_CREATED", "USER", user.getId(),
                Map.of("email", user.getEmail(),
                       "roles", request.getRoles().stream()
                               .map(Enum::name).collect(Collectors.joining(","))));

        log.info("Created user: {}", user.getEmail());
        return UserResponse.from(user);
    }

    // ── Read ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listUsers(int page, int size) {
        size = Math.min(size, 100);
        Page<UserEntity> pageResult = userRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        return PageResponse.of(
                pageResult.getContent().stream()
                        .map(UserResponse::from)
                        .collect(Collectors.toList()),
                page, size, pageResult.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        return UserResponse.from(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("User", email));
        return UserResponse.from(user);
    }

    // ── Update (PATCH) ─────────────────────────────────────────

    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        UserEntity user = findOrThrow(id);

        boolean changed = false;

        if (request.getStatus() != null && !request.getStatus().equals(user.getStatus())) {
            user.setStatus(request.getStatus());
            changed = true;
        }

        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            Set<RoleEntity> roles = roleRepository.findByNameIn(request.getRoles());
            if (roles.size() != request.getRoles().size()) {
                throw ApiException.badRequest("One or more specified roles do not exist");
            }
            user.setRoles(roles);
            changed = true;
        }

        if (changed) {
            user = userRepository.save(user);
            auditService.log("USER_UPDATED", "USER", user.getId(),
                    Map.of("status", user.getStatus().name()));
        }

        return UserResponse.from(user);
    }

    // ── Helpers ────────────────────────────────────────────────

    private UserEntity findOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("User", id));
    }


}
