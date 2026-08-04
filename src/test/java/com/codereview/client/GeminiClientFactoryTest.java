package com.codereview.client;

import com.codereview.factory.LLMClientFactory;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GeminiClientFactoryTest {
  @Test
  void unknownModelIdThrowsRuntimeExceptionWrappingIllegalArgumentException() {
    RuntimeException ex =
        assertThrows(RuntimeException.class, () -> LLMClientFactory.build("not-a-real-model"));

    assertTrue(ex.getMessage().contains("Failed to create LLM client"));
    assertInstanceOf(IllegalArgumentException.class, ex.getCause());
    assertTrue(ex.getCause().getMessage().contains("Invalid model id"));
  }

  @Test
  void validModelIdWithoutApiKeyEnvVarThrowsRuntimeExceptionWrappingIllegalStateException() {
    // This assumes the sandbox running these tests doesn't already have GEMINI_API_KEY set.
    // If it is set, the factory would succeed in constructing a client, so skip rather than fail.
    Assumptions.assumeTrue(
        System.getenv("GEMINI_API_KEY") == null || System.getenv("GEMINI_API_KEY").isBlank(),
        "GEMINI_API_KEY is set in this environment; skipping missing-key test");

    RuntimeException ex =
        assertThrows(RuntimeException.class, () -> LLMClientFactory.build("gemini-3.5-flash"));

    assertInstanceOf(IllegalStateException.class, ex.getCause());
    assertTrue(ex.getCause().getMessage().contains("GEMINI_API_KEY"));
  }
}
