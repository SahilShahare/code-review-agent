package com.codereview.client;

public interface LLMClient {

  String review(String prompt) throws Exception;
  
}
