package com.docqueue.config;

import com.docqueue.common.security.HtmlSanitizerDeserializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            // Apply HTML sanitization globally to all incoming Strings
            builder.deserializerByType(String.class, new HtmlSanitizerDeserializer());
        };
    }
}
