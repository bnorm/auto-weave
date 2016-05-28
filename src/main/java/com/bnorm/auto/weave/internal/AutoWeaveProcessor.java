package com.bnorm.auto.weave.internal;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.NoType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleElementVisitor6;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import com.bnorm.auto.weave.AutoAdvice;
import com.bnorm.auto.weave.AutoWeave;
import com.bnorm.auto.weave.internal.chain.Chain;
import com.bnorm.auto.weave.internal.chain.MethodChain;
import com.bnorm.auto.weave.internal.chain.MethodException;
import com.bnorm.auto.weave.internal.chain.VoidMethodChain;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import static javax.tools.Diagnostic.Kind.ERROR;

@AutoService(Processor.class)
public class AutoWeaveProcessor extends AbstractProcessor {

    private Messager messager;
    private Elements elements;
    private Types types;

    public AutoWeaveProcessor() {
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return ImmutableSet.of(AutoWeave.class.getName(), AutoAdvice.class.getName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        messager = processingEnv.getMessager();
        elements = processingEnv.getElementUtils();
        types = processingEnv.getTypeUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        ImmutableSet<WeaveDescriptor> weave = weave(roundEnv, advice(roundEnv));
        writeWeave(weave);
        return !weave.isEmpty();
    }

    private void writeWeave(ImmutableSet<WeaveDescriptor> weave) {
        for (WeaveDescriptor weaveDescriptor : weave) {
            TypeElement type = weaveDescriptor.element();
            String typeName = type.getSimpleName().toString();
            String autoWeaveTypeName = "AutoWeave_" + typeName;
            TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(autoWeaveTypeName);
            typeBuilder.addOriginatingElement(type);
            typeBuilder.superclass(TypeName.get(type.asType()));
            typeBuilder.addModifiers(Modifier.FINAL);

            for (AspectDescriptor aspectDescriptor : weaveDescriptor.aspects()) {
                TypeName aspectType = TypeName.get(aspectDescriptor.element().asType());
                String aspectFieldName = Names.classToVariable(aspectDescriptor.name());

                // todo(bnorm) should this be static?
                FieldSpec.Builder aspectBuilder = FieldSpec.builder(aspectType, aspectFieldName);
                aspectBuilder.addModifiers(Modifier.PRIVATE, Modifier.FINAL);
                aspectBuilder.initializer("new $T()", aspectType);
                typeBuilder.addField(aspectBuilder.build());
            }

            List<ExecutableElement> constructors = ElementFilter.constructorsIn(
                    weaveDescriptor.element().getEnclosedElements());
            // todo(bnorm) make sure there is at least one non-private constructor
            for (ExecutableElement constructor : constructors) {
                if (!constructor.getModifiers().contains(Modifier.PRIVATE)) {
                    MethodSpec.Builder constructorBuilder = inherit(constructor);
                    typeBuilder.addMethod(constructorBuilder.build());
                }
            }

            for (WeaveMethodDescriptor weaveMethodDescriptor : weaveDescriptor.methods()) {
                ExecutableElement method = weaveMethodDescriptor.element();
                String methodName = weaveMethodDescriptor.name();
                List<? extends VariableElement> parameters = method.getParameters();
                StringBuilder methodParameters = new StringBuilder();
                for (int i = 0, len = parameters.size(); i < len; i++) {
                    if (i != 0) {
                        methodParameters.append(", ");
                    }
                    methodParameters.append(parameters.get(i).toString());
                }
                boolean returns = !(method.getReturnType() instanceof NoType);

                FieldSpec staticPointcut = StaticPointcut.spec(weaveDescriptor, weaveMethodDescriptor);
                typeBuilder.addField(staticPointcut);

                MethodSpec.Builder methodBuilder = overriding(method);
                methodBuilder.addStatement("$T pointcut = $T.create(this, $T.<Object>asList($L), $L)", Pointcut.class,
                                           Pointcut.class, Arrays.class, methodParameters, staticPointcut.name);
                methodBuilder.addStatement("$T chain", Chain.class);
                methodBuilder.addCode("");

                TypeSpec.Builder superBuilder = TypeSpec.anonymousClassBuilder("");
                superBuilder.addSuperinterface(returns ? MethodChain.class : VoidMethodChain.class);
                superBuilder.addMethod(MethodSpec.methodBuilder(methodName)
                                                 .addAnnotation(Override.class)
                                                 .addModifiers(Modifier.PUBLIC)
                                                 .addException(Throwable.class)
                                                 .returns(returns ? Object.class : void.class)
                                                 .addStatement((returns ? "return " : "") + "$N.super.$N($L)",
                                                               autoWeaveTypeName, methodName, methodParameters)
                                                 .build());

                methodBuilder.addStatement("chain = $L", superBuilder.build());

                for (AdviceDescriptor adviceDescriptor : weaveMethodDescriptor.advice()) {
                    String aspectFieldName = adviceDescriptor.aspect().fieldName();
                    String aspectMethodName = adviceDescriptor.name();

                    methodBuilder.addStatement("chain = $L",
                                               adviceDescriptor.crosscut().getChain(aspectFieldName, aspectMethodName));
                }

                CodeBlock.Builder callBuilder = CodeBlock.builder();
                callBuilder.beginControlFlow("try");
                {
                    if (returns) {
                        callBuilder.addStatement("return ($T) chain.call()", TypeName.get(method.getReturnType()));
                    } else {
                        callBuilder.addStatement("chain.call()");
                    }
                }

                callBuilder.nextControlFlow("catch ($T e)", MethodException.class);
                {
                    boolean begin = true;
                    List<? extends TypeMirror> thrownTypes = method.getThrownTypes();
                    for (int i = 0, len = thrownTypes.size(); i < len; i++) {
                        TypeName thrownTypeName = TypeName.get(thrownTypes.get(i));
                        beginOrNext(callBuilder, thrownTypeName, begin);
                        callBuilder.addStatement("throw ($T) e.getCause()", thrownTypeName);
                        begin = false;
                    }
                    if (!thrownTypes.contains(elements.getTypeElement(Error.class.getCanonicalName()).asType())) {
                        beginOrNext(callBuilder, Error.class, begin);
                        callBuilder.addStatement("throw ($T) e.getCause()", Error.class);
                        begin = false;
                    }
                    if (!thrownTypes.contains(
                            elements.getTypeElement(RuntimeException.class.getCanonicalName()).asType())) {
                        beginOrNext(callBuilder, RuntimeException.class, begin);
                        callBuilder.addStatement("throw ($T) e.getCause()", RuntimeException.class);
                    }
                    callBuilder.nextControlFlow("else");
                    callBuilder.addStatement("throw new $T($S, e.getCause())", AssertionError.class,
                                             "Please contact the library developer");

                    callBuilder.endControlFlow();
                }
                callBuilder.endControlFlow();
                methodBuilder.addCode(callBuilder.build());

                typeBuilder.addMethod(methodBuilder.build());
            }

            PackageElement packageElement = (PackageElement) weaveDescriptor.element().getEnclosingElement();
            JavaFile javaFile = JavaFile.builder(packageElement.getQualifiedName().toString(), typeBuilder.build())
                                        .build();
            writeSourceFile(javaFile, weaveDescriptor.element());
        }
    }

    private void beginOrNext(CodeBlock.Builder callBuilder, Object thrownTypeName, boolean begin) {
        if (begin) {
            callBuilder.beginControlFlow("if (e.getCause() instanceof $T)", thrownTypeName);
        } else {
            callBuilder.nextControlFlow("else if (e.getCause() instanceof $T)", thrownTypeName);
        }
    }

    private MethodSpec.Builder overriding(ExecutableElement method) {
        Set<Modifier> modifiers = method.getModifiers();
        if (modifiers.contains(Modifier.PRIVATE) || modifiers.contains(Modifier.FINAL) || modifiers.contains(
                Modifier.STATIC)) {
            throw new IllegalArgumentException("cannot override method with modifiers: " + modifiers);
        }

        String methodName = method.getSimpleName().toString();
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName);

        methodBuilder.addAnnotation(ClassName.get(Override.class));
        for (AnnotationMirror mirror : method.getAnnotationMirrors()) {
            AnnotationSpec annotationSpec = AnnotationSpec.get(mirror);
            if (annotationSpec.type.equals(ClassName.get(Override.class))) {
                continue;
            }
            methodBuilder.addAnnotation(annotationSpec);
        }

        modifiers = new LinkedHashSet<>(modifiers);
        modifiers.remove(Modifier.ABSTRACT);
        methodBuilder.addModifiers(modifiers);

        for (TypeParameterElement typeParameterElement : method.getTypeParameters()) {
            TypeVariable var = (TypeVariable) typeParameterElement.asType();
            methodBuilder.addTypeVariable(TypeVariableName.get(var));
        }

        methodBuilder.returns(TypeName.get(method.getReturnType()));

        List<? extends VariableElement> parameters = method.getParameters();
        for (VariableElement parameter : parameters) {
            TypeName type = TypeName.get(parameter.asType());
            String name = parameter.getSimpleName().toString();
            Set<Modifier> parameterModifiers = parameter.getModifiers();
            ParameterSpec.Builder parameterBuilder = ParameterSpec.builder(type, name)
                                                                  .addModifiers(parameterModifiers.toArray(
                                                                          new Modifier[parameterModifiers.size()]));
            parameterBuilder.addModifiers(Modifier.FINAL);
            for (AnnotationMirror mirror : parameter.getAnnotationMirrors()) {
                parameterBuilder.addAnnotation(AnnotationSpec.get(mirror));
            }
            methodBuilder.addParameter(parameterBuilder.build());
        }
        methodBuilder.varargs(method.isVarArgs());

        for (TypeMirror thrownType : method.getThrownTypes()) {
            methodBuilder.addException(TypeName.get(thrownType));
        }

        return methodBuilder;
    }

    private MethodSpec.Builder inherit(ExecutableElement method) {
        // todo(bnorm) validate that this is in fact a constructor

        Set<Modifier> modifiers = method.getModifiers();
        if (modifiers.contains(Modifier.PRIVATE) || modifiers.contains(Modifier.FINAL) || modifiers.contains(
                Modifier.STATIC) || modifiers.contains(Modifier.ABSTRACT)) {
            throw new IllegalArgumentException("cannot override method with modifiers: " + modifiers);
        }

        String methodName = method.getSimpleName().toString();
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName);

        for (AnnotationMirror mirror : method.getAnnotationMirrors()) {
            AnnotationSpec annotationSpec = AnnotationSpec.get(mirror);
            methodBuilder.addAnnotation(annotationSpec);
        }

        modifiers = new LinkedHashSet<>(modifiers);
        methodBuilder.addModifiers(modifiers);

        for (TypeParameterElement typeParameterElement : method.getTypeParameters()) {
            TypeVariable var = (TypeVariable) typeParameterElement.asType();
            methodBuilder.addTypeVariable(TypeVariableName.get(var));
        }

        List<? extends VariableElement> parameters = method.getParameters();
        StringBuilder paramStr = new StringBuilder();
        for (int i = 0, len = parameters.size(); i < len; i++) {
            final VariableElement parameter = parameters.get(i);
            TypeName type = TypeName.get(parameter.asType());
            String name = parameter.getSimpleName().toString();
            Set<Modifier> parameterModifiers = parameter.getModifiers();
            ParameterSpec.Builder parameterBuilder = ParameterSpec.builder(type, name)
                                                                  .addModifiers(parameterModifiers.toArray(
                                                                          new Modifier[parameterModifiers.size()]));
            for (AnnotationMirror mirror : parameter.getAnnotationMirrors()) {
                parameterBuilder.addAnnotation(AnnotationSpec.get(mirror));
            }
            methodBuilder.addParameter(parameterBuilder.build());

            if (i != 0) {
                paramStr.append(", ");
            }
            paramStr.append(parameters.get(i).toString());
        }
        methodBuilder.varargs(method.isVarArgs());

        for (TypeMirror thrownType : method.getThrownTypes()) {
            methodBuilder.addException(TypeName.get(thrownType));
        }

        methodBuilder.addStatement("super($L)", paramStr);

        return methodBuilder;
    }


    private void writeSourceFile(JavaFile javaFile, TypeElement originatingType) {
        try {
            JavaFileObject sourceFile = processingEnv.getFiler()
                                                     .createSourceFile(javaFile.typeSpec.name, originatingType);
            Writer writer = sourceFile.openWriter();
            try {
                javaFile.writeTo(writer);
            } finally {
                writer.close();
            }
        } catch (IOException e) {
            // This should really be an error, but we make it a warning in the hope of resisting Eclipse
            // bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=367599. If that bug manifests, we may get
            // invoked more than once for the same file, so ignoring the ability to overwrite it is the
            // right thing to do. If we are unable to write for some other reason, we should get a compile
            // error later because user code will have a reference to the code we were supposed to
            // generate (new AutoValue_Foo() or whatever) and that reference will be undefined.
            processingEnv.getMessager()
                         .printMessage(Diagnostic.Kind.WARNING,
                                       "Could not write generated class " + javaFile.typeSpec.name + ": " + e);
        }
    }

    private ImmutableSet<AdviceDescriptor> advice(RoundEnvironment roundEnv) {
        final Map<TypeElement, Set<ExecutableElement>> adviceMap = new HashMap<>();
        for (Element advice : roundEnv.getElementsAnnotatedWith(AutoAdvice.class)) {
            advice.accept(new SimpleElementVisitor6<Object, Object>() {
                @Override
                protected Object defaultAction(Element e, Object o) {
                    throw new AssertionError("AutoAdvice cannot be applied to anything but a method!");
                }

                @Override
                public Object visitExecutable(ExecutableElement e, Object o) {
                    boolean error = false;
                    // todo(bnorm) first parameter should extend JoinPoint but not be JoinPoint
                    // todo(bnorm) class should have a default constructor

                    List<? extends VariableElement> parameters = e.getParameters();
                    if (parameters.size() != 1) {
                        messager.printMessage(ERROR,
                                              "AutoAdvice can only be applied to a method with a single JoinPoint parameter.",
                                              e);
                        error = true;
                    }

                    // todo(bnorm) check first (and only) parameter extends but is not JoinPoint

                    if (error) {
                        return null;
                    }

                    TypeElement type = (TypeElement) e.getEnclosingElement();
                    Set<ExecutableElement> aspectAdvice = adviceMap.get(type);
                    if (aspectAdvice == null) {
                        adviceMap.put(type, aspectAdvice = new HashSet<>());
                    }
                    aspectAdvice.add(e);
                    return null;
                }
            }, null);
        }

        ImmutableSet.Builder<AdviceDescriptor> adviceDescriptorBuilder = ImmutableSet.builder();
        for (Map.Entry<TypeElement, Set<ExecutableElement>> entry : adviceMap.entrySet()) {
            AspectDescriptor aspectDescriptor = AspectDescriptor.create(entry.getKey());
            for (ExecutableElement element : entry.getValue()) {
                CrosscutEnum crosscut = getCrosscut(element);
                AutoAdvice autoAdvice = element.getAnnotation(AutoAdvice.class);
                ImmutableSet<TypeMirror> targets = ImmutableSet.copyOf(valueFrom(autoAdvice));
                adviceDescriptorBuilder.add(AdviceDescriptor.create(aspectDescriptor, element, crosscut, targets));
            }
        }
        return adviceDescriptorBuilder.build();
    }

    private ImmutableSet<WeaveDescriptor> weave(RoundEnvironment roundEnv, ImmutableSet<AdviceDescriptor> advice) {
        final Map<TypeMirror, Set<AdviceDescriptor>> annotationMap = new HashMap<>();
        for (AdviceDescriptor descriptor : advice) {
            for (TypeMirror target : descriptor.targets()) {
                Set<AdviceDescriptor> adviceDescriptors = annotationMap.get(target);
                if (adviceDescriptors == null) {
                    annotationMap.put(target, adviceDescriptors = new HashSet<>());
                }
                adviceDescriptors.add(descriptor);
            }
        }

        final ImmutableSet.Builder<WeaveDescriptor> weaveDescriptorBuilder = ImmutableSet.builder();
        for (Element weave : roundEnv.getElementsAnnotatedWith(AutoWeave.class)) {
            weave.accept(new SimpleElementVisitor6<Object, Object>() {
                @Override
                protected Object defaultAction(Element e, Object o) {
                    throw new AssertionError("AutoWeave cannot be applied to anything but a class!");
                }

                @Override
                public Object visitType(TypeElement e, Object o) {
                    // boolean error = false;
                    // todo(bnorm) make sure it's not an interface or abstract class
                    ImmutableList.Builder<WeaveMethodDescriptor> weaveMethodDescriptorBuilder = ImmutableList.builder();
                    process(e, weaveMethodDescriptorBuilder);
                    weaveDescriptorBuilder.add(WeaveDescriptor.create(e, weaveMethodDescriptorBuilder.build()));
                    return null;
                }

                private void process(TypeElement e,
                                     ImmutableList.Builder<WeaveMethodDescriptor> weaveMethodDescriptorBuilder) {
                    TypeMirror superclass = e.getSuperclass();
                    if (!(superclass instanceof NoType)) {
                        process((TypeElement) types.asElement(superclass), weaveMethodDescriptorBuilder);
                    }

                    // todo(bnorm) how do override methods fit in?
                    for (ExecutableElement element : ElementFilter.methodsIn(e.getEnclosedElements())) {
                        ImmutableList.Builder<AdviceDescriptor> adviceDescriptorBuilder = ImmutableList.builder();
                        for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
                            Set<AdviceDescriptor> adviceDescriptors = annotationMap.get(
                                    annotationMirror.getAnnotationType());
                            if (adviceDescriptors != null) {
                                adviceDescriptorBuilder.addAll(adviceDescriptors);
                            }
                        }

                        ImmutableList<AdviceDescriptor> adviceDescriptors = adviceDescriptorBuilder.build();
                        if (!adviceDescriptors.isEmpty()) {
                            weaveMethodDescriptorBuilder.add(WeaveMethodDescriptor.create(element, adviceDescriptors));
                        }
                    }
                }
            }, null);
        }
        return weaveDescriptorBuilder.build();
    }

    private CrosscutEnum getCrosscut(ExecutableElement element) {
        String name = element.getParameters().get(0).asType().toString();
        return CrosscutEnum.crosscutMap.get(name);
    }

    private List<? extends TypeMirror> valueFrom(AutoAdvice autoAdvice) {
        List<? extends TypeMirror> typeMirrors = null;
        try {
            autoAdvice.value();
        } catch (MirroredTypesException e) {
            typeMirrors = e.getTypeMirrors();
        }
        assert typeMirrors != null;
        return typeMirrors;
    }
}
