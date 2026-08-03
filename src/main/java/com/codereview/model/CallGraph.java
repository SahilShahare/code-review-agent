package com.codereview.model;

import com.codereview.model.signatures.MethodSignature;

import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;

public class CallGraph {
    private final Map<String, Set<String>> calless = new HashMap<>();
    private final Map<String, Set<String>> callers = new HashMap<>();
    private final Map<String, MethodSignature> signatureById = new HashMap<> ();
    private final Map<String, String> sourceById = new HashMap<> ();

    public void registerMethod(MethodSignature sig, String source) {
        signatureById.put(sig.canonicalId(), sig);
        sourceById.put(sig.canonicalId(), source);
    }

    public void addCall(MethodSignature caller, MethodSignature callee) {
        calless.computeIfAbsent(caller.canonicalId(), id -> new HashSet<>()).add(callee.canonicalId());
        callers.computeIfAbsent(callee.canonicalId(), id ->new HashSet<>()).add(caller.canonicalId());

        signatureById.putIfAbsent(callee.canonicalId(), callee);
        signatureById.putIfAbsent(caller.canonicalId(), caller);
    }

    public Set<MethodSignature> getCallees(MethodSignature sig) {
        return calless.getOrDefault(sig.canonicalId(), Set.of())
                .stream()
                .map(signatureById::get)
                .collect(Collectors.toUnmodifiableSet());
    }

    public Set<MethodSignature> getCallers(MethodSignature sig) {
        return callers.getOrDefault(sig.canonicalId(), Set.of())
                .stream()
                .map(signatureById::get)
                .collect(Collectors.toUnmodifiableSet());
    }

    public String getSource(MethodSignature sig) {
        return sourceById.get(sig.canonicalId());
    }

    public Set<MethodSignature> allMethods() {
        return Set.copyOf(signatureById.values());
    }

    public void debugPrint() {
        List<MethodSignature> sorted = new ArrayList<>(allMethods());
        sorted.sort(Comparator.comparing(MethodSignature::canonicalId));

        System.out.println("CallGraph: " + sorted.size() + " method(s)");
        for (MethodSignature sig : sorted) {
            System.out.println(sig.canonicalId());

            Set<MethodSignature> callees = getCallees(sig);
            if (!callees.isEmpty()) {
                System.out.println("  Calls:");
                callees.stream()
                        .map(ms -> String.format("%s (%s)", ms.canonicalId(), ms.filePath()))
                        .sorted()
                        .forEach(id -> System.out.println(" -> " + id));
            }

            Set<MethodSignature> callers = getCallers(sig);
            if (!callers.isEmpty()) {
                System.out.println("  Called by:");
                callers.stream()
                        .map(ms -> String.format("%s (%s)", ms.canonicalId(), ms.filePath()))
                        .sorted()
                        .forEach(id -> System.out.println(" -> " + id));
            }
        }
    }
}
