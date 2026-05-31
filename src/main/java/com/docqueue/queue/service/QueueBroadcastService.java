package com.docqueue.queue.service;

import com.docqueue.queue.dto.QueueUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * WebSocket broadcast service.
 * Publishes queue updates to STOMP topics.
 *
 * Clients subscribe to:
 *  - /topic/queue/{doctorId}   → full queue update
 *  - /topic/patient/{userId}   → personal notification
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueueBroadcastService {

    private static final String QUEUE_TOPIC   = "/topic/queue/";
    private static final String PATIENT_TOPIC = "/topic/patient/";

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcastToDoctor(Long doctorId, QueueUpdateEvent event) {
        String destination = QUEUE_TOPIC + doctorId;
        messagingTemplate.convertAndSend(destination, event);
        log.debug("Broadcast queue update to {}: {} waiting", destination, event.getPatientsWaiting());
    }

    public void notifyPatient(Long userId, Object payload) {
        String destination = PATIENT_TOPIC + userId;
        messagingTemplate.convertAndSend(destination, payload);
        log.debug("Sent personal notification to user {}", userId);
    }
}
