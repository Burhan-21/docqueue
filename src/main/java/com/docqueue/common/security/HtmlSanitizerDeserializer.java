package com.docqueue.common.security;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import java.io.IOException;

/**
 * Global Jackson deserializer for Strings.
 * Automatically sanitizes all incoming JSON string payloads to prevent XSS.
 */
public class HtmlSanitizerDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();
        if (value == null) {
            return null;
        }
        // Use JSoup to strip all HTML tags
        return Jsoup.clean(value, Safelist.none());
    }
}
