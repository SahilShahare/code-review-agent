package com.codereview.client;

import com.codereview.model.enums.GeminiModel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class GeminiClient implements LLMClient {
    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(60))
            .build();

    private final GeminiModel model;
    private final String apiKey;
    private final ObjectMapper mapper = new ObjectMapper();

    public GeminiClient(GeminiModel model, String apiKey) {
        this.model = model;
        this.apiKey = apiKey;
    }

    @Override
    public String review(String prompt) throws Exception {
        ObjectNode root = mapper.createObjectNode();
        var contents = root.putArray("contents");
        var content = contents.addObject();
        var parts = content.putArray("parts");
        parts.addObject().put("text", prompt);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format(ENDPOINT, model.id(), apiKey)))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(root)))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Gemini API error " + response.statusCode() + ": " + response.body());
        }

        JsonNode body = mapper.readTree(response.body());
        return body.at("/candidates/0/content/parts/0/text").asText();
    }
}
