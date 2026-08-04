package com.codereview.factory;

import com.codereview.client.LLMClient;
import com.codereview.model.enums.GeminiModel;
import com.codereview.model.enums.LLMRegistry;

import static com.codereview.model.enums.LLMRegistry.GEMINI;

public class LLMClientFactory {

  private LLMClientFactory() {}

  public static LLMClient build(String modelId) {
    try {
      LLMRegistry registry =
          LLMRegistry.getLLMRegistryFromModelId(modelId)
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "Invalid model id '"
                              + modelId
                              + "'. Supported models: "
                              + LLMRegistry.allModels()));
      return switch (registry) {
        case GEMINI -> createGeminiClient(modelId);
      };
    } catch (Exception e) {
      throw new RuntimeException("Failed to create LLM client for model '" + modelId + "'", e);
    }
  }

  private static LLMClient createGeminiClient(String modelId) throws ReflectiveOperationException {
    String apiKey = System.getenv("GEMINI_API_KEY");
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalStateException("Environment variable GEMINI_API_KEY is not set or is blank");
    }
    GeminiModel model =
        (GeminiModel)
            GEMINI
                .getModel(modelId)
                .orElseThrow(
                    () -> new IllegalArgumentException("Unknown Gemini model: " + modelId));
    return GEMINI
        .getClientClass()
        .getDeclaredConstructor(GeminiModel.class, String.class)
        .newInstance(model, apiKey);
  }
}
