package com.finance.dashboard.user.repository;

import com.finance.dashboard.user.entity.RoleEntity;
import com.finance.dashboard.user.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

@Repository
public interface RoleRepository extends JpaRepository<RoleEntity, Short> {

    Optional<RoleEntity> findByName(RoleName name);

    Set<RoleEntity> findByNameIn(Collection<RoleName> names);
}
