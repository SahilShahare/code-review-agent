package com.codereview.model.enums;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LLMRegistryTest {

  @Test
  void getLLMRegistryFromModelIdFindsGeminiForKnownModel() {
    Optional<LLMRegistry> registry =
        LLMRegistry.getLLMRegistryFromModelId(GeminiModel.GEMINI_2_5_PRO.id());

    assertTrue(registry.isPresent());
    assertEquals(LLMRegistry.GEMINI, registry.get());
  }

  @Test
  void getLLMRegistryFromModelIdIsEmptyForUnknownModel() {
    assertTrue(LLMRegistry.getLLMRegistryFromModelId("not-a-real-model").isEmpty());
  }

  @Test
  void isValidModelReflectsRegisteredModelIds() {
    assertTrue(LLMRegistry.isValidModel(GeminiModel.GEMINI_3_5_FLASH.id()));
    assertFalse(LLMRegistry.isValidModel("gpt-5"));
  }

  @Test
  void allModelsContainsEveryGeminiModelId() {
    for (GeminiModel model : GeminiModel.values()) {
      assertTrue(
          LLMRegistry.allModels().contains(model.id()),
          () -> "Expected allModels() to contain " + model.id());
    }
  }

  @Test
  void getModelResolvesCorrectEnumConstant() {
    Optional<LLMModel> model = LLMRegistry.GEMINI.getModel(GeminiModel.GEMINI_3_1_PRO_PREVIEW.id());

    assertTrue(model.isPresent());
    assertEquals(GeminiModel.GEMINI_3_1_PRO_PREVIEW, model.get());
  }

  @Test
  void getModelIsEmptyForUnknownId() {
    assertTrue(LLMRegistry.GEMINI.getModel("unknown-model-id").isEmpty());
  }
}
