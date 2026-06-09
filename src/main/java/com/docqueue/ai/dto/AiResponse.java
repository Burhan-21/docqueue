package com.docqueue.ai.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiResponse {
    private String reply;
}
