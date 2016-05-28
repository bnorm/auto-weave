package com.bnorm.auto.weave;

import javax.tools.JavaFileObject;

import com.bnorm.auto.weave.internal.AutoWeaveProcessor;
import com.google.testing.compile.JavaFileObjects;
import com.google.testing.compile.JavaSourcesSubject;

import junit.framework.TestCase;

public class BasicTest extends TestCase {

    public void test() throws Exception {
        // @formatter:off
        JavaFileObject javaFileObject = JavaFileObjects.forResource("good/Target.java");

        JavaFileObject traceAnnotation = JavaFileObjects.forResource("support/Trace.java");

        JavaFileObject traceAspect = JavaFileObjects.forResource("good/TraceAspect.java");

        JavaFileObject expectedExtensionOutput = JavaFileObjects.forResource("expected/AutoWeave_Target.java");
        // @formatter:on

        JavaSourcesSubject.assertThat(javaFileObject, traceAnnotation, traceAspect)
                          .processedWith(new AutoWeaveProcessor())
                          .compilesWithoutError()
                          .and()
                          .generatesSources(expectedExtensionOutput);
    }
}
