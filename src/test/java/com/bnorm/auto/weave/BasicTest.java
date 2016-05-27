package com.bnorm.auto.weave;

import javax.tools.JavaFileObject;

import com.bnorm.auto.weave.internal.AutoWeaveProcessor;
import com.google.testing.compile.JavaFileObjects;
import com.google.testing.compile.JavaSourcesSubject;

import junit.framework.TestCase;

public class BasicTest extends TestCase {

    public void test() throws Exception {
        // @formatter:off
        JavaFileObject javaFileObject = JavaFileObjects.forSourceLines(
                "com.bnorm.auto.weave.Target",
                "package com.bnorm.auto.weave;",
                "",
                "@AutoWeave",
                "public abstract class Target {",
                "",
                "    @Trace",
                "    public String method() {",
                "        return \"Method!\";",
                "    }",
                "}");

        JavaFileObject traceAnnotation = JavaFileObjects.forSourceLines(
                "com.bnorm.auto.weave.Trace",
                "package com.bnorm.auto.weave;",
                "",
                "public @interface Trace {",
                "}");

        JavaFileObject traceAspect = JavaFileObjects.forSourceLines(
                "com.bnorm.auto.weave.TraceAspect",
                "package com.bnorm.auto.weave;",
                "",
                "public class TraceAspect {",
                "    @AutoAdvice(Trace.class)",
                "    public Object around(AroundJoinPoint point) {",
                "        return point.proceed();",
                "    }",
//                "    @AutoAdvice(Trace.class)",
//                "    public void before(BeforeJoinPoint point) {}",
                "}");

        JavaFileObject expectedExtensionOutput = JavaFileObjects.forSourceLines(
                "com.bnorm.auto.weave.AutoWeave_Target",
                "package com.bnorm.auto.weave;",
                "",
                "import com.bnorm.auto.weave.internal.Pointcut;",
                "import com.bnorm.auto.weave.internal.StaticPointcut;",
                "import com.bnorm.auto.weave.internal.chain.AroundChain;",
                "import com.bnorm.auto.weave.internal.chain.Chain;",
                "import com.bnorm.auto.weave.internal.chain.MethodChain;",
                "import com.bnorm.auto.weave.internal.chain.MethodException;",
                "import java.lang.AssertionError;",
                "import java.lang.Error;",
                "import java.lang.Object;",
                "import java.lang.Override;",
                "import java.lang.RuntimeException;",
                "import java.lang.String;",
                "import java.lang.Throwable;",
                "import java.util.Arrays;",
                "",
                "final class AutoWeave_Target extends Target {",
                "",
                "    private static final StaticPointcut methodPointcut = StaticPointcut.create(\"method\", Target.class, java.lang.String.class, Arrays.asList());",
                "",
                "    private final TraceAspect traceAspect = new TraceAspect();",
                "",
                "    @Override",
                "    @Trace",
                "    public String method() {",
                "        Pointcut pointcut = Pointcut.create(this, Arrays.asList(), methodPointcut);",
                "        Chain chain;",
                "        chain = new MethodChain() {",
                "            @Override",
                "            public Object method() throws Throwable {",
                "                return AutoWeave_Target.super.method();",
                "            }",
                "        };",
                "        chain = new AroundChain(chain, pointcut) {",
                "            @Override",
                "            public Object around(AroundJoinPoint joinPoint) {",
                "                return traceAspect.around(joinPoint);",
                "            }",
                "        };",
                "        try {",
                "            return (String) chain.call();",
                "        } catch (MethodException e) {",
                "            if (e.getCause() instanceof Error) {",
                "                throw (Error) e.getCause();",
                "            } else if (e.getCause() instanceof RuntimeException) {",
                "                throw (RuntimeException) e.getCause();",
                "            } else {",
                "                throw new AssertionError(\"Please contact the library developer\", e.getCause());",
                "            }",
                "        }",
                "    }",
                "}");
        // @formatter:on

        JavaSourcesSubject.assertThat(javaFileObject, traceAnnotation, traceAspect)
                          .processedWith(new AutoWeaveProcessor())
                          .compilesWithoutError()
                          .and()
                          .generatesSources(expectedExtensionOutput);
    }
}
