package com.finance.dashboard.record.service;

import com.finance.dashboard.audit.service.AuditService;
import com.finance.dashboard.common.exception.ApiException;
import com.finance.dashboard.common.model.PageResponse;
import com.finance.dashboard.record.dto.*;
import com.finance.dashboard.record.entity.CategoryEntity;
import com.finance.dashboard.record.entity.FinancialRecordEntity;
import com.finance.dashboard.record.repository.CategoryRepository;
import com.finance.dashboard.record.repository.RecordRepository;
import com.finance.dashboard.record.repository.RecordSpecification;
import com.finance.dashboard.user.entity.UserEntity;
import com.finance.dashboard.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Financial record business logic.
 *
 * Invariants:
 * <ol>
 *   <li>Amount must be &gt; 0</li>
 *   <li>Currency must be 3-char uppercase (validated by DTO, enforced here too)</li>
 *   <li>Deleted records can neither be read nor mutated</li>
 *   <li>Idempotency-Key header prevents duplicate record creation</li>
 *   <li>@Version field enforces optimistic locking on updates/deletes</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecordService {

    private static final String IDEMPOTENCY_KEY_PREFIX = "idempotency:record:";
    private static final Duration IDEMPOTENCY_TTL      = Duration.ofHours(24);

    private final RecordRepository      recordRepository;
    private final CategoryRepository    categoryRepository;
    private final UserRepository        userRepository;
    private final AuditService          auditService;
    private final RedisTemplate<String, String> redisTemplate;

    // ── Create (with Idempotency Key) ─────────────────────────────────────

    /**
     * Create a financial record.
     * If an {@code Idempotency-Key} is provided (UUID string),
     * a duplicate request returns the original response within 24 hours.
     */
    @Transactional
    public RecordResponse createRecord(CreateRecordRequest request) {
        return createRecord(request, null);
    }

    @Transactional
    public RecordResponse createRecord(CreateRecordRequest request, String idempotencyKey) {
        // Idempotency guard — atomic SETNX to prevent race condition on duplicate requests
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String redisKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;

            // Try to atomically reserve this key (returns false if already set)
            Boolean wasAbsent = redisTemplate.opsForValue()
                    .setIfAbsent(redisKey, "PENDING", IDEMPOTENCY_TTL);

            if (Boolean.FALSE.equals(wasAbsent)) {
                // Key already exists — find and return the existing record
                String existingId = redisTemplate.opsForValue().get(redisKey);
                if (existingId != null && !existingId.equals("PENDING")) {
                    log.info("Idempotency hit for key {}: returning record {}", idempotencyKey, existingId);
                    return RecordResponse.from(findActiveOrThrow(UUID.fromString(existingId)));
                }
                // Still PENDING (extremely rare): treat as duplicate, return 409
                throw ApiException.conflict(
                        "A request with Idempotency-Key '" + idempotencyKey + "' is already being processed.");
            }
        }

        validateAmount(request.getAmount());
        UserEntity actor = currentUser();
        CategoryEntity category = resolveCategory(request.getCategoryId());

        FinancialRecordEntity record = FinancialRecordEntity.builder()
                .recordDate(request.getRecordDate())
                .type(request.getType())
                .category(category)
                .amount(request.getAmount().setScale(2))
                .currency(request.getCurrency().toUpperCase())
                .notes(request.getNotes())
                .createdBy(actor)
                .build();

        record = recordRepository.save(record);

        // Update the Redis key from PENDING → actual record ID
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            redisTemplate.opsForValue().set(
                    IDEMPOTENCY_KEY_PREFIX + idempotencyKey,
                    record.getId().toString(),
                    IDEMPOTENCY_TTL);
        }

        auditService.log("RECORD_CREATED", "FINANCIAL_RECORD", record.getId(),
                Map.of("type", record.getType().name(),
                       "amount", record.getAmount().toPlainString(),
                       "currency", record.getCurrency()));

        log.info("Record {} created by {}", record.getId(), actor.getEmail());
        return RecordResponse.from(record);
    }

    // ── Read (filtered + paged) ────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<RecordResponse> listRecords(RecordFilterParams filter) {
        validatePageSize(filter);

        Sort sort = buildSort(filter.getSort(), filter.getDirection());
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);

        Page<FinancialRecordEntity> page = recordRepository.findAll(
                RecordSpecification.fromFilter(filter), pageable);

        List<RecordResponse> items = page.getContent().stream()
                .map(RecordResponse::from)
                .collect(Collectors.toList());

        return PageResponse.of(items, filter.getPage(), filter.getSize(), page.getTotalElements());
    }

    // ── Read by ID ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public RecordResponse getRecord(UUID id) {
        return RecordResponse.from(findActiveOrThrow(id));
    }

    // ── Update (PUT — full replacement) ────────────────────────

    @Transactional
    public RecordResponse updateRecord(UUID id, UpdateRecordRequest request, Long ifMatchVersion) {
        FinancialRecordEntity record = findActiveOrThrow(id);
        validateAmount(request.getAmount());

        // Optimistic locking: If-Match version check
        if (ifMatchVersion != null && !ifMatchVersion.equals(record.getVersion())) {
            throw ApiException.conflict(
                    "Record was modified by another request. Fetch the latest version and retry.");
        }

        CategoryEntity category = resolveCategory(request.getCategoryId());

        record.setRecordDate(request.getRecordDate());
        record.setType(request.getType());
        record.setCategory(category);
        record.setAmount(request.getAmount().setScale(2));
        record.setCurrency(request.getCurrency().toUpperCase());
        record.setNotes(request.getNotes());

        record = recordRepository.save(record);

        auditService.log("RECORD_UPDATED", "FINANCIAL_RECORD",
                record.getId(), Map.of("amount", record.getAmount().toPlainString()));

        return RecordResponse.from(record);
    }

    // ── Delete (soft) ──────────────────────────────────────────

    @Transactional
    public void deleteRecord(UUID id, Long ifMatchVersion) {
        FinancialRecordEntity record = findActiveOrThrow(id);

        // Optimistic locking: If-Match version check
        if (ifMatchVersion != null && !ifMatchVersion.equals(record.getVersion())) {
            throw ApiException.conflict(
                    "Record was modified by another request. Fetch the latest version and retry.");
        }

        record.softDelete();
        recordRepository.save(record);

        String actorEmail = currentUser().getEmail();
        auditService.log("RECORD_DELETED", "FINANCIAL_RECORD", id,
                Map.of("deletedBy", actorEmail));

        log.info("Record {} soft-deleted by {}", id, actorEmail);
    }

    // ── Internal helpers ───────────────────────────────────────

    private FinancialRecordEntity findActiveOrThrow(UUID id) {
        return recordRepository.findActiveById(id)
                .orElseThrow(() -> ApiException.notFound("Financial record", id));
    }

    private CategoryEntity resolveCategory(UUID categoryId) {
        if (categoryId == null) return null;
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> ApiException.notFound("Category", categoryId));
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw ApiException.badRequest("Amount must be greater than zero");
        }
    }

    private void validatePageSize(RecordFilterParams filter) {
        if (filter.getSize() > 100) {
            filter.setSize(100);
        }
        if (filter.getSize() < 1) {
            filter.setSize(1);
        }
        if (filter.getPage() < 0) {
            filter.setPage(0);
        }
    }

    private Sort buildSort(String field, String direction) {
        Sort.Direction dir = "asc".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        String safeField = List.of("recordDate", "amount", "createdAt", "type")
                .contains(field) ? field : "recordDate";
        return Sort.by(dir, safeField);
    }

    private UserEntity currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmailWithRoles(email)
                .orElseThrow(() -> ApiException.unauthorized("Authenticated user not found"));
    }

    // ── Bulk Create (atomic — all-or-nothing) ──────────────────

    @Transactional
    public BulkCreateResponse bulkCreate(List<CreateRecordRequest> items) {
        // Validate all first (fail-fast)
        items.forEach(item -> validateAmount(item.getAmount()));

        List<RecordResponse> created = items.stream()
                .map(item -> createRecord(item, null))
                .collect(Collectors.toList());

        return BulkCreateResponse.builder()
                .createdCount(created.size())
                .items(created)
                .build();
    }

    // ── CSV Export ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public void exportToCsv(RecordFilterParams filter, PrintWriter writer) {
        // Remove pagination for full export
        filter.setSize(Integer.MAX_VALUE);
        filter.setPage(0);

        Sort sort = buildSort(filter.getSort(), filter.getDirection());
        Pageable pageable = PageRequest.of(0, 10_000, sort);

        List<FinancialRecordEntity> records = recordRepository
                .findAll(RecordSpecification.fromFilter(filter), pageable)
                .getContent();

        // CSV header
        writer.println("id,recordDate,type,category,amount,currency,notes,createdAt");

        // CSV rows
        records.forEach(r -> writer.printf("%s,%s,%s,%s,%s,%s,\"%s\",%s%n",
                r.getId(),
                r.getRecordDate(),
                r.getType().name(),
                r.getCategory() != null ? r.getCategory().getName() : "",
                r.getAmount().toPlainString(),
                r.getCurrency(),
                r.getNotes() != null ? r.getNotes().replace("\"", "\"\"") : "",
                r.getCreatedAt()));

        writer.flush();
    }
}
