package com.codereview.model.enums;

import com.codereview.client.GeminiClient;
import com.codereview.client.LLMClient;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public enum LLMRegistry {
  GEMINI(GeminiModel.class);

  private static final Map<String, LLMRegistry> LOOKUP =
      Arrays.stream(values())
          .flatMap(
              llm ->
                  Arrays.stream(llm.modelClass.getEnumConstants()).map(e -> Map.entry(e.id(), llm)))
          .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

  private final Class<? extends LLMModel> modelClass;

  <E extends Enum<E> & LLMModel> LLMRegistry(Class<E> modelClass) {
    this.modelClass = modelClass;
  }

  public static Optional<LLMRegistry> getLLMRegistryFromModelId(String modelId) {
    return Optional.ofNullable(LOOKUP.get(modelId));
  }

  public static Set<String> allModels() {
    return LOOKUP.keySet();
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public Optional<LLMModel> getModel(String id) {
    return (Optional) LLMModel.fromId((Class) modelClass, id);
  }

  public static boolean isValidModel(String id) {
    return LOOKUP.containsKey(id);
  }
}
