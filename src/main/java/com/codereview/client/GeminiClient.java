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

import static com.codereview.constants.Constants.GEMINI_ENDPOINT;
import static com.codereview.constants.Constants.LLM_API_CONNECTION_TIMEOUT;
import static com.codereview.constants.Constants.LLM_API_REQUEST_TIMEOUT;

public class GeminiClient implements LLMClient {

  private static final String CONTENTS = "contents";
  private static final String PARTS = "parts";
  private static final String TEXT = "text";
  private static final String CANDIDATE_TEXT_PATH = "/candidates/0/content/parts/0/text";
  private static final String BLOCK_REASON_PATH = "/promptFeedback/blockReason";

  private final HttpClient client;
  private final GeminiModel model;
  private final String apiKey;
  private final ObjectMapper mapper = new ObjectMapper();

  public GeminiClient(GeminiModel model, String apiKey) {
    this(
        model,
        apiKey,
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(LLM_API_CONNECTION_TIMEOUT))
            .build());
  }

  //For Injecting HTTP Client while testing
  GeminiClient(GeminiModel model, String apiKey, HttpClient client) {
    this.model = model;
    this.apiKey = apiKey;
    this.client = client;
  }

  @Override
  public String review(String prompt) throws Exception {
    ObjectNode root = mapper.createObjectNode();

    var contents = root.putArray(CONTENTS);
    var content = contents.addObject();
    var parts = content.putArray(PARTS);

    parts.addObject().put(TEXT, prompt);

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(String.format(GEMINI_ENDPOINT, model.id())))
            .header("Content-Type", "application/json")
            .header("x-goog-api-key", apiKey)
            .timeout(Duration.ofSeconds(LLM_API_REQUEST_TIMEOUT))
            .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(root)))
            .build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() != 200) {
      throw new RuntimeException(
          "Gemini API error " + response.statusCode() + ": " + response.body());
    }

    JsonNode body = mapper.readTree(response.body());
    JsonNode textNode = body.at(CANDIDATE_TEXT_PATH);

    if (textNode.isMissingNode()) {
      JsonNode blockReason = body.at(BLOCK_REASON_PATH);

      String reason =
          blockReason.isMissingNode()
              ? "no candidates returned"
              : "blocked (" + blockReason.asText() + ")";

      throw new RuntimeException("Gemini returned no usable response: " + reason);
    }

    return textNode.asText();
  }
}
