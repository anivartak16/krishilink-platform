package com.krishilink.outbox;

import com.krishilink.entity.OutboxEvent;

import java.util.Map;
import java.util.Set;

/** Consumes relayed outbox events. Runs inside the relay transaction that marks the event published. */
public interface OutboxEventHandler {

    Set<String> eventTypes();

    void handle(OutboxEvent event, Map<String, Object> payload);
}
