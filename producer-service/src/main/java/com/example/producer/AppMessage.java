package com.example.producer;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AppMessage(
    @JsonProperty("id") String id,
    @JsonProperty("sender") String sender,
    @JsonProperty("content") @JsonAlias({"message", "text", "body"}) String content,
    @JsonProperty("priority") String priority,
    @JsonProperty("timestamp") Long timestamp
) {
    public AppMessage {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString().substring(0, 8);
        }
        if (sender == null || sender.isBlank()) {
            sender = "anonymous";
        }
        if (priority == null || priority.isBlank()) {
            priority = "NORMAL";
        }
        if (timestamp == null || timestamp == 0L) {
            timestamp = System.currentTimeMillis();
        }
    }

    public static AppMessage of(String content) {
        return new AppMessage(null, "anonymous", content, "NORMAL", System.currentTimeMillis());
    }
}
