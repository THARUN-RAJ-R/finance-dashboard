package com.finance.dashboard.user.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Maps to the {@code roles} table.
 * Authorities exposed as {@code ROLE_ADMIN}, {@code ROLE_ANALYST}, etc.
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Short id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 50)
    private RoleName name;

    /** Returns the Spring Security authority string e.g. {@code ROLE_ADMIN}. */
    public String toAuthority() {
        return "ROLE_" + name.name();
    }
}
