package com.codereview.model.enums;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GeminiModelTest {

  @Test
  void llmModelFromIdFindsMatchingConstant() {
    Optional<GeminiModel> model = LLMModel.fromId(GeminiModel.class, "gemini-2.5-flash-lite");

    assertTrue(model.isPresent());
    assertEquals(GeminiModel.GEMINI_2_5_FLASH_LITE, model.get());
  }

  @Test
  void llmModelFromIdIsEmptyWhenNoMatch() {
    assertTrue(LLMModel.fromId(GeminiModel.class, "does-not-exist").isEmpty());
  }
}
