package com.docqueue.queue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Serializable DTO for queue entries returned over REST.
 * Avoids lazy loading and Jackson circular references from the JPA entity.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueEntryDto {
    private Long          id;
    private Long          appointmentId;
    private int           tokenNumber;
    private String        patientName;
    private int           queuePosition;
    private String        status;           // WAITING | IN_PROGRESS | COMPLETED | SKIPPED | CANCELLED
    private Integer       estimatedWait;    // minutes
    private LocalDateTime scheduledAt;
    private LocalDateTime enteredAt;
    private LocalDateTime calledAt;
    private LocalDateTime completedAt;
    // For QueueTracker — patient needs to know the doctor context
    private Long          doctorId;
    private String        doctorName;
}
