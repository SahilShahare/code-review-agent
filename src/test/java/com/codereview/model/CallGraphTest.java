package com.codereview.model;

import com.codereview.model.records.JavaMethodSignature;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CallGraphTest {

  private final JavaMethodSignature caller =
      new JavaMethodSignature("com.example.Foo.bar()", "Foo.java");
  private final JavaMethodSignature callee =
      new JavaMethodSignature("com.example.Baz.qux()", "Baz.java");

  @Test
  void getCalleesReturnsEmptySetForUnknownMethod() {
    CallGraph graph = new CallGraph();
    assertTrue(graph.getCallees(caller).isEmpty());
  }

  @Test
  void getCallersReturnsEmptySetForUnknownMethod() {
    CallGraph graph = new CallGraph();
    assertTrue(graph.getCallers(caller).isEmpty());
  }

  @Test
  void getSourceReturnsNullWhenMethodNeverRegistered() {
    CallGraph graph = new CallGraph();
    assertNull(graph.getSource(caller));
  }

  @Test
  void addCallLinksCallerToCalleeAndBack() {
    CallGraph graph = new CallGraph();
    graph.addCall(caller, callee);

    assertEquals(Set.of(callee), graph.getCallees(caller));
    assertEquals(Set.of(caller), graph.getCallers(callee));
  }

  @Test
  void addCallRegistersBothSignaturesEvenWithoutExplicitRegisterMethod() {
    CallGraph graph = new CallGraph();
    graph.addCall(caller, callee);

    assertEquals(Set.of(caller, callee), graph.allMethods());
  }

  @Test
  void registerMethodAssociatesSourceCode() {
    CallGraph graph = new CallGraph();
    graph.registerMethod(caller, "void bar() { qux(); }");

    assertEquals("void bar() { qux(); }", graph.getSource(caller));
  }

  @Test
  void addCallDoesNotOverwriteSourceAlreadyRegistered() {
    CallGraph graph = new CallGraph();
    graph.registerMethod(caller, "original source");
    graph.addCall(caller, callee);

    assertEquals("original source", graph.getSource(caller));
  }

  @Test
  void multipleCalleesForSameCallerAreAllTracked() {
    CallGraph graph = new CallGraph();
    JavaMethodSignature calleeTwo =
        new JavaMethodSignature("com.example.Other.method()", "Other.java");

    graph.addCall(caller, callee);
    graph.addCall(caller, calleeTwo);

    assertEquals(Set.of(callee, calleeTwo), graph.getCallees(caller));
  }

  @Test
  void multipleCallersForSameCalleeAreAllTracked() {
    CallGraph graph = new CallGraph();
    JavaMethodSignature callerTwo =
        new JavaMethodSignature("com.example.Other.method()", "Other.java");

    graph.addCall(caller, callee);
    graph.addCall(callerTwo, callee);

    assertEquals(Set.of(caller, callerTwo), graph.getCallers(callee));
  }

  @Test
  void allMethodsIncludesMethodsRegisteredWithSourceOnly() {
    CallGraph graph = new CallGraph();
    graph.registerMethod(caller, "source");

    assertEquals(Set.of(caller), graph.allMethods());
  }
}
