package com.docqueue.ai.service;

import com.docqueue.ai.dto.AiRequest;
import com.docqueue.ai.dto.AiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    @Value("${openai.api-key:}")
    private String openAiApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public AiResponse generateResponse(AiRequest request) {
        log.info("Processing AI generation request: {}", request.getQuery());
        
        if (openAiApiKey == null || openAiApiKey.isBlank() || openAiApiKey.equals("${OPENAI_API_KEY}")) {
            return AiResponse.builder()
                .reply("I am the DocQueue AI Assistant. You asked: '" + request.getQuery() 
                     + "'. (Operating in placeholder mode because no OPENAI_API_KEY is configured).")
                .build();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openAiApiKey);

            Map<String, Object> body = Map.of(
                "model", "gpt-3.5-turbo",
                "messages", List.of(
                    Map.of("role", "system", "content", "You are a helpful virtual assistant for a medical clinic called DocQueue. Be concise and polite."),
                    Map.of("role", "user", "content", request.getQuery())
                ),
                "max_tokens", 150
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                "https://api.openai.com/v1/chat/completions",
                entity,
                Map.class
            );

            if (response != null && response.containsKey("choices")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String replyText = (String) message.get("content");
                    return AiResponse.builder().reply(replyText.trim()).build();
                }
            }

            return AiResponse.builder().reply("Sorry, I could not process that request.").build();

        } catch (Exception e) {
            log.error("Error communicating with OpenAI: {}", e.getMessage(), e);
            return AiResponse.builder().reply("Sorry, an error occurred while connecting to the AI service.").build();
        }
    }
}
