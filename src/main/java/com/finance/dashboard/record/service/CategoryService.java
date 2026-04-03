package com.finance.dashboard.record.service;

import com.finance.dashboard.audit.service.AuditService;
import com.finance.dashboard.common.exception.ApiException;
import com.finance.dashboard.record.dto.CategoryRequest;
import com.finance.dashboard.record.dto.CategoryResponse;
import com.finance.dashboard.record.entity.CategoryEntity;
import com.finance.dashboard.record.repository.CategoryRepository;
import com.finance.dashboard.record.repository.RecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Category management business logic.
 *
 * <p>Soft-delete only: a category cannot be hard-deleted if any active
 * financial record references it (returns 409 Conflict).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final RecordRepository   recordRepository;
    private final AuditService       auditService;

    // ── List all active ───────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CategoryResponse> listAll() {
        return categoryRepository.findAllActive().stream()
                .map(CategoryResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(UUID id) {
        return CategoryResponse.from(findActiveOrThrow(id));
    }

    // ── Create ─────────────────────────────────────────────────

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw ApiException.conflict("Category '" + request.getName() + "' already exists");
        }

        CategoryEntity saved = categoryRepository.save(
                CategoryEntity.builder().name(request.getName().trim()).build());

        auditService.log("CATEGORY_CREATED", "CATEGORY", saved.getId(),
                Map.of("name", saved.getName()));

        log.info("Category created: {}", saved.getName());
        return CategoryResponse.from(saved);
    }

    // ── Update (PATCH — rename) ────────────────────────────────

    @Transactional
    public CategoryResponse update(UUID id, CategoryRequest request) {
        CategoryEntity cat = findActiveOrThrow(id);

        if (categoryRepository.existsByNameIgnoreCase(request.getName())
                && !cat.getName().equalsIgnoreCase(request.getName())) {
            throw ApiException.conflict("Category '" + request.getName() + "' already exists");
        }

        cat.setName(request.getName().trim());
        categoryRepository.save(cat);

        auditService.log("CATEGORY_UPDATED", "CATEGORY", id,
                Map.of("name", cat.getName()));

        return CategoryResponse.from(cat);
    }

    // ── Soft Delete ────────────────────────────────────────────

    @Transactional
    public void delete(UUID id) {
        CategoryEntity cat = findActiveOrThrow(id);

        // 409 if any non-deleted record references this category
        long activeRecordCount = recordRepository.countActiveByCategory(id);
        if (activeRecordCount > 0) {
            throw ApiException.conflict(
                    "Cannot delete category '" + cat.getName() + "': " +
                    activeRecordCount + " active record(s) reference it. " +
                    "Re-assign or delete those records first.");
        }

        cat.softDelete();
        categoryRepository.save(cat);

        auditService.log("CATEGORY_DELETED", "CATEGORY", id,
                Map.of("name", cat.getName()));

        log.info("Category soft-deleted: {}", cat.getName());
    }

    // ── Helpers ────────────────────────────────────────────────

    private CategoryEntity findActiveOrThrow(UUID id) {
        return categoryRepository.findActiveById(id)
                .orElseThrow(() -> ApiException.notFound("Category", id));
    }


}
