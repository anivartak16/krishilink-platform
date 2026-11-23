package com.krishilink.outbox;

import com.krishilink.entity.Notification;
import com.krishilink.entity.OutboxEvent;
import com.krishilink.entity.User;
import com.krishilink.exception.ResourceNotFoundException;
import com.krishilink.repository.NotificationRepository;
import com.krishilink.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/** Turns trade events into in-app notifications. */
@Component
@RequiredArgsConstructor
public class TradeNotificationHandler implements OutboxEventHandler {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    public Set<String> eventTypes() {
        return Set.of(TradeEvents.OFFER_PLACED, TradeEvents.FPO_PROPOSAL_SENT,
                TradeEvents.OFFER_ACCEPTED, TradeEvents.PAYMENT_SECURED);
    }

    @Override
    public void handle(OutboxEvent event, Map<String, Object> p) {
        switch (event.getEventType()) {
            case TradeEvents.OFFER_PLACED -> notify(p.get("sellerId"), "NEW_OFFER",
                    "New offer from " + p.get("buyerName") + " for " + p.get("quantity") + " kg " + p.get("commodity"));
            case TradeEvents.FPO_PROPOSAL_SENT -> notify(p.get("buyerId"), "NEW_OFFER",
                    "Direct FPO Supply Proposal from " + p.get("fpoName") + " for " + p.get("commodity"));
            case TradeEvents.OFFER_ACCEPTED -> notify(p.get("buyerId"), "OFFER_ACCEPTED",
                    "Your offer for " + p.get("quantity") + " kg " + p.get("commodity") + " was accepted!");
            case TradeEvents.PAYMENT_SECURED -> notify(p.get("sellerId"), "PAYMENT_RECEIVED",
                    "₹" + p.get("amount") + " secured in KrishiLink Escrow for " + p.get("commodity")
                            + ". You can now arrange dispatch.");
            default -> throw new IllegalArgumentException("Unsupported event " + event.getEventType());
        }
    }

    private void notify(Object userId, String type, String message) {
        Long id = ((Number) userId).longValue();
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        notificationRepository.save(Notification.builder()
                .user(user)
                .type(type)
                .message(message)
                .read(false)
                .build());
    }
}
