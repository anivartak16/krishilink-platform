package com.krishilink.outbox;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishilink.entity.OutboxEvent;
import com.krishilink.repository.OutboxEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Polls unpublished outbox events and hands each one to its handler. Each event is claimed with
 * SELECT ... FOR UPDATE SKIP LOCKED and marked published in the same transaction as its handler's writes.
 * A failing event is retried on later polls until max-attempts is reached.
 */
@Component
@Slf4j
public class OutboxRelay {

    private final OutboxEventRepository outboxEventRepository;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;
    private final Map<String, OutboxEventHandler> handlers = new HashMap<>();

    @Value("${krishilink.outbox.relay-enabled:true}")
    private boolean relayEnabled;

    @Value("${krishilink.outbox.batch-size:50}")
    private int batchSize;

    @Value("${krishilink.outbox.max-attempts:5}")
    private int maxAttempts;

    public OutboxRelay(OutboxEventRepository outboxEventRepository,
                       PlatformTransactionManager transactionManager,
                       ObjectMapper objectMapper,
                       List<OutboxEventHandler> eventHandlers) {
        this.outboxEventRepository = outboxEventRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.objectMapper = objectMapper;
        for (OutboxEventHandler handler : eventHandlers) {
            handler.eventTypes().forEach(type -> handlers.put(type, handler));
        }
    }

    @Scheduled(fixedDelayString = "${krishilink.outbox.poll-interval-ms:2000}")
    public void poll() {
        if (relayEnabled) {
            relayPending();
        }
    }

    /** Relays one batch of pending events and returns how many were published. */
    public int relayPending() {
        int published = 0;
        for (Long id : outboxEventRepository.findPendingIds(maxAttempts, PageRequest.of(0, batchSize))) {
            if (relayOne(id)) {
                published++;
            }
        }
        return published;
    }

    private boolean relayOne(Long id) {
        try {
            return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
                OutboxEvent event = outboxEventRepository.claimPending(id).orElse(null);
                if (event == null) {
                    return false;
                }
                OutboxEventHandler handler = handlers.get(event.getEventType());
                if (handler == null) {
                    throw new IllegalStateException("No handler for event type " + event.getEventType());
                }
                handler.handle(event, readPayload(event));
                event.setAttempts(event.getAttempts() + 1);
                event.setPublishedAt(LocalDateTime.now());
                event.setLastError(null);
                return true;
            }));
        } catch (RuntimeException ex) {
            log.warn("Outbox event {} failed: {}", id, ex.getMessage());
            String error = String.valueOf(ex.getMessage());
            transactionTemplate.executeWithoutResult(status ->
                    outboxEventRepository.recordFailure(id, error.length() > 1000 ? error.substring(0, 1000) : error));
            return false;
        }
    }

    private Map<String, Object> readPayload(OutboxEvent event) {
        try {
            return objectMapper.readValue(event.getPayload(), new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new IllegalStateException("Unreadable payload for outbox event " + event.getId(), e);
        }
    }
}
