package com.finance.dashboard.record.repository;

import com.finance.dashboard.record.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<CategoryEntity, UUID> {

    /** All non-deleted categories ordered by name. */
    @Query("SELECT c FROM CategoryEntity c WHERE c.deletedAt IS NULL ORDER BY c.name ASC")
    List<CategoryEntity> findAllActive();

    /** Find active category by ID. */
    @Query("SELECT c FROM CategoryEntity c WHERE c.id = :id AND c.deletedAt IS NULL")
    Optional<CategoryEntity> findActiveById(@Param("id") UUID id);

    /** Case-insensitive name uniqueness check (only among active categories). */
    @Query("SELECT COUNT(c) > 0 FROM CategoryEntity c WHERE LOWER(c.name) = LOWER(:name) AND c.deletedAt IS NULL")
    boolean existsByNameIgnoreCase(@Param("name") String name);
}
