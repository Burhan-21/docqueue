package com.docqueue.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

/**
 * WebSocket + STOMP configuration.
 *
 * Broker prefixes:
 *  - /topic/** → broadcast (subscribe)
 *  - /app/**   → application handlers (send)
 *
 * Clients subscribe to:
 *  - /topic/queue/{doctorId}   → live queue updates
 *  - /topic/patient/{userId}   → personal notifications
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // In-memory broker for MVP; swap to Redis STOMP broker for multi-instance
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();  // SockJS fallback for older browsers
    }
}
