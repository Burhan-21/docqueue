package com.docqueue.queue.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class QueueStateDto {
    private Long          doctorId;
    private String        doctorName;
    private int           currentToken;
    private int           patientsWaiting;
    private int           estimatedWaitMinutes;
    private LocalDateTime updatedAt;
}
