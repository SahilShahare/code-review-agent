package com.codereview.model.records;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class JavaMethodSignatureTest {
    @Test
    void canonicalIdPrefixesQualifiedNameWithLanguage() {
        JavaMethodSignature sig = new JavaMethodSignature("com.example.Foo.bar()", "Foo.java");

        assertEquals("Java:com.example.Foo.bar()", sig.canonicalId());
    }

    @Test
    void accessorsReturnConstructorValues() {
        JavaMethodSignature sig = new JavaMethodSignature("com.example.Foo.bar()", "src/Foo.java");

        assertEquals("com.example.Foo.bar()", sig.qualifiedName());
        assertEquals("src/Foo.java", sig.filePath());
    }

    @Test
    void equalityIsBasedOnQualifiedNameAndFilePath() {
        JavaMethodSignature a = new JavaMethodSignature("com.example.Foo.bar()", "Foo.java");
        JavaMethodSignature b = new JavaMethodSignature("com.example.Foo.bar()", "Foo.java");
        JavaMethodSignature c = new JavaMethodSignature("com.example.Foo.baz()", "Foo.java");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }
}
