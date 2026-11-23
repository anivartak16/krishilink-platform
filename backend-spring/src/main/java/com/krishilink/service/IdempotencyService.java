package com.krishilink.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishilink.entity.IdempotencyKey;
import com.krishilink.entity.User;
import com.krishilink.exception.BadRequestException;
import com.krishilink.exception.ConflictException;
import com.krishilink.exception.IdempotencyKeyReusedException;
import com.krishilink.repository.IdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Optional Idempotency-Key support: the first successful response for a (user, key) pair is stored and
 * replayed for retries with the same request. Callers must hold a lock that serializes duplicate requests
 * (e.g. the transaction row lock) so the lookup and the save happen in one critical section.
 */
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    public static final String HEADER = "Idempotency-Key";
    public static final String REPLAYED_HEADER = "Idempotent-Replayed";
    private static final int MAX_KEY_LENGTH = 100;

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ObjectMapper objectMapper;

    @Value("${krishilink.idempotency.ttl-hours:24}")
    private long ttlHours;

    public String requestHash(String operation, Map<String, Object> body) {
        try {
            String canonical = operation + "\n" + objectMapper.writeValueAsString(
                    body == null ? Map.of() : new TreeMap<>(body));
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Cannot hash request", e);
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<ResponseEntity<String>> findReplay(User user, String key, String requestHash) {
        if (key == null) {
            return Optional.empty();
        }
        validate(key);
        return idempotencyKeyRepository.findByUserIdAndIdemKey(user.getId(), key).map(stored -> {
            if (!stored.getRequestHash().equals(requestHash)) {
                throw new IdempotencyKeyReusedException();
            }
            return ResponseEntity.status(stored.getResponseStatus())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(REPLAYED_HEADER, "true")
                    .body(stored.getResponseBody());
        });
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void remember(User user, String key, String requestHash, ResponseEntity<?> response) {
        if (key == null || !response.getStatusCode().is2xxSuccessful()) {
            return;
        }
        String body;
        try {
            body = objectMapper.writeValueAsString(response.getBody());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot store idempotent response", e);
        }
        try {
            idempotencyKeyRepository.saveAndFlush(IdempotencyKey.builder()
                    .userId(user.getId())
                    .idemKey(key)
                    .requestHash(requestHash)
                    .responseStatus(response.getStatusCode().value())
                    .responseBody(body)
                    .build());
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Another request with this Idempotency-Key is in progress");
        }
    }

    @Scheduled(cron = "${krishilink.idempotency.purge-cron:0 15 3 * * *}")
    @Transactional
    public void purgeExpired() {
        idempotencyKeyRepository.deleteCreatedBefore(LocalDateTime.now().minusHours(ttlHours));
    }

    private static void validate(String key) {
        if (key.isBlank() || key.length() > MAX_KEY_LENGTH) {
            throw new BadRequestException("Idempotency-Key must be 1-" + MAX_KEY_LENGTH + " characters");
        }
    }
}
