package com.codereview.model.enums;

import java.util.Arrays;
import java.util.Optional;

public interface LLMModel {

  static <E extends Enum<E> & LLMModel> Optional<E> fromId(Class<E> enumClass, String id) {
    return Arrays.stream(enumClass.getEnumConstants())
        .filter(model -> model.id().equals(id))
        .findFirst();
  }

  String id();
}
