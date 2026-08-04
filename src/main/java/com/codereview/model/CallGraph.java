package com.codereview.model;

import com.codereview.model.records.MethodSignature;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CallGraph {

  private final Map<String, Set<String>> callees = new HashMap<>();
  private final Map<String, Set<String>> callers = new HashMap<>();
  private final Map<String, MethodSignature> signatureById = new HashMap<>();
  private final Map<String, String> sourceById = new HashMap<>();

  public void registerMethod(MethodSignature sig, String source) {
    signatureById.put(sig.canonicalId(), sig);
    sourceById.put(sig.canonicalId(), source);
  }

  public void addCall(MethodSignature caller, MethodSignature callee) {
    callees.computeIfAbsent(caller.canonicalId(), id -> new HashSet<>()).add(callee.canonicalId());
    callers.computeIfAbsent(callee.canonicalId(), id -> new HashSet<>()).add(caller.canonicalId());

    signatureById.putIfAbsent(callee.canonicalId(), callee);
    signatureById.putIfAbsent(caller.canonicalId(), caller);
  }

  public Set<MethodSignature> getCallees(MethodSignature sig) {
    return callees.getOrDefault(sig.canonicalId(), Set.of()).stream()
        .map(signatureById::get)
        .collect(Collectors.toUnmodifiableSet());
  }

  public Set<MethodSignature> getCallers(MethodSignature sig) {
    return callers.getOrDefault(sig.canonicalId(), Set.of()).stream()
        .map(signatureById::get)
        .collect(Collectors.toUnmodifiableSet());
  }

  public String getSource(MethodSignature sig) {
    return sourceById.get(sig.canonicalId());
  }

  public Set<MethodSignature> allMethods() {
    return Set.copyOf(signatureById.values());
  }
}
