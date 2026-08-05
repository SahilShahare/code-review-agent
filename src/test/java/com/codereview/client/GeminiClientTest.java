package com.codereview.client;

import com.codereview.model.enums.GeminiModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GeminiClientTest {

  @Mock private HttpClient httpClient;
  @Mock private HttpResponse<String> httpResponse;

  private void stubResponse(int statusCode, String body) throws Exception {
    when(httpResponse.statusCode()).thenReturn(statusCode);
    when(httpResponse.body()).thenReturn(body);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(httpResponse);
  }

  @Test
  void reviewReturnsCandidateTextOnSuccessfulResponse() throws Exception {
    stubResponse(
        200,
        "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Looks good, no findings.\"}]}}]}");

    GeminiClient client =
        new GeminiClient(GeminiModel.GEMINI_3_5_FLASH, "test-api-key", httpClient);

    String result = client.review("review this diff");

    assertEquals("Looks good, no findings.", result);
  }

  @Test
  void reviewSendsPromptAndApiKeyInTheOutgoingRequest() throws Exception {
    stubResponse(200, "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"ok\"}]}}]}");

    GeminiClient client = new GeminiClient(GeminiModel.GEMINI_3_5_FLASH, "secret-key", httpClient);
    client.review("check this code");

    ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
    verify(httpClient).send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));

    HttpRequest sentRequest = requestCaptor.getValue();
    assertEquals("secret-key", sentRequest.headers().firstValue("x-goog-api-key").orElse(null));
    assertTrue(sentRequest.uri().toString().contains(GeminiModel.GEMINI_3_5_FLASH.id()));
    assertEquals("POST", sentRequest.method());
  }

  @Test
  void reviewThrowsRuntimeExceptionWhenApiReturnsNonSuccessStatus() throws Exception {
    stubResponse(429, "Rate limit exceeded");

    GeminiClient client =
        new GeminiClient(GeminiModel.GEMINI_3_5_FLASH, "test-api-key", httpClient);

    RuntimeException ex = assertThrows(RuntimeException.class, () -> client.review("prompt"));

    assertTrue(ex.getMessage().contains("429"));
    assertTrue(ex.getMessage().contains("Rate limit exceeded"));
  }

  @Test
  void reviewThrowsRuntimeExceptionWhenResponseWasBlocked() throws Exception {
    stubResponse(200, "{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}");

    GeminiClient client =
        new GeminiClient(GeminiModel.GEMINI_3_5_FLASH, "test-api-key", httpClient);

    RuntimeException ex = assertThrows(RuntimeException.class, () -> client.review("prompt"));

    assertTrue(ex.getMessage().contains("blocked (SAFETY)"));
  }

  @Test
  void reviewThrowsRuntimeExceptionWhenNoCandidatesAndNoBlockReasonArePresent() throws Exception {
    stubResponse(200, "{}");

    GeminiClient client =
        new GeminiClient(GeminiModel.GEMINI_3_5_FLASH, "test-api-key", httpClient);

    RuntimeException ex = assertThrows(RuntimeException.class, () -> client.review("prompt"));

    assertTrue(ex.getMessage().contains("no candidates returned"));
  }
}
