package com.bnorm.auto.weave;

import com.bnorm.auto.weave.internal.AutoWeaveProcessor;
import com.google.testing.compile.JavaFileObjects;
import com.google.testing.compile.JavaSourcesSubject;

import junit.framework.TestCase;

public class BasicTest extends TestCase {

    public void test() throws Exception {
        JavaSourcesSubject.assertThat(JavaFileObjects.forResource("good/Target.java"),
                                      JavaFileObjects.forResource("support/Trace.java"),
                                      JavaFileObjects.forResource("good/TraceAspect.java"))
                          .processedWith(new AutoWeaveProcessor())
                          .compilesWithoutError()
                          .and()
                          .generatesSources(JavaFileObjects.forResource("expected/AutoWeave_Target.java"));
    }
}
