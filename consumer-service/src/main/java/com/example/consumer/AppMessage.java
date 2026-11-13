package com.example.consumer;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AppMessage(
    @JsonProperty("id") String id,
    @JsonProperty("sender") String sender,
    @JsonProperty("content") @JsonAlias({"message", "text", "body"}) String content,
    @JsonProperty("priority") String priority,
    @JsonProperty("timestamp") Long timestamp
) {}
