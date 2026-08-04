package com.codereview.model.enums;

import com.codereview.client.GeminiClient;
import com.codereview.client.LLMClient;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public enum LLMRegistry {
  GEMINI(GeminiModel.class, GeminiClient.class);

  private static final Map<String, LLMRegistry> LOOKUP =
      Arrays.stream(values())
          .flatMap(
              llm ->
                  Arrays.stream(llm.modelClass.getEnumConstants()).map(e -> Map.entry(e.id(), llm)))
          .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

  private final Class<? extends LLMModel> modelClass;
  private final Class<? extends LLMClient> clientClass;

  <E extends Enum<E> & LLMModel> LLMRegistry(
      Class<E> modelClass, Class<? extends LLMClient> clientClass) {
    this.modelClass = modelClass;
    this.clientClass = clientClass;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public Optional<LLMModel> getModel(String id) {
    return (Optional) LLMModel.fromId((Class) modelClass, id);
  }

  public Class<? extends LLMClient> getClientClass() {
    return this.clientClass;
  }

  public static Optional<LLMRegistry> getLLMRegistryFromModelId(String modelId) {
    return Optional.ofNullable(LOOKUP.get(modelId));
  }

  public static Set<String> allModels() {
    return LOOKUP.keySet();
  }
}
