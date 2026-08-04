package com.codereview.model.enums;

public enum GeminiModel implements LLMModel {
  GEMINI_3_6_FLASH("gemini-3.6-flash"),
  GEMINI_3_5_FLASH("gemini-3.5-flash"),
  GEMINI_3_1_PRO_PREVIEW("gemini-3.1-pro-preview"),
  GEMINI_3_5_FLASH_LITE("gemini-3.5-flash-lite"),
  GEMINI_3_1_FLASH_LITE("gemini-3.1-flash-lite"),
  GEMINI_3_FLASH_PREVIEW("gemini-3-flash-preview"),
  GEMINI_2_5_PRO("gemini-2.5-pro"),
  GEMINI_2_5_FLASH_LITE("gemini-2.5-flash-lite");

  private final String id;

  GeminiModel(String id) {
    this.id = id;
  }

  @Override
  public String id() {
    return this.id;
  }
}
