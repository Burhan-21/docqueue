package com.docqueue.queue.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * WebSocket broadcast payload for queue state changes.
 * Published to /topic/queue/{doctorId} on every queue mutation.
 */
@Getter
@Builder
public class QueueUpdateEvent {
    private Long          doctorId;
    private int           currentToken;
    private int           patientsWaiting;
    private int           estimatedWaitMinutes;
    private LocalDateTime updatedAt;
}
