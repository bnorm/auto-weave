package com.bnorm.auto.weave.internal;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
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
import com.bnorm.auto.weave.AutoAspect;
import com.bnorm.auto.weave.AutoWeave;
import com.bnorm.auto.weave.internal.advice.Advice;
import com.bnorm.auto.weave.internal.advice.Chain;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.NameAllocator;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import static javax.tools.Diagnostic.Kind.ERROR;

@AutoService(Processor.class)
public class AutoWeaveProcessor extends AbstractProcessor {

    private static final LinkedHashSet<String> SUPPORTED = new LinkedHashSet<>(
            Arrays.asList(AutoWeave.class.getName(), AutoAdvice.class.getName()));

    private Messager messager;
    private Elements elements;
    private Types types;

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return SUPPORTED;
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
        Set<WeaveDescriptor> weave = weave(roundEnv, advice(roundEnv));
        writeWeave(weave);
        return !weave.isEmpty();
    }

    private void writeWeave(Set<WeaveDescriptor> weave) {
        NameAllocator nameAllocator = new NameAllocator();

        for (WeaveDescriptor weaveDescriptor : weave) {
            TypeElement type = weaveDescriptor.element();
            String autoWeaveTypeName = classNameOf(type);
            TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(autoWeaveTypeName);
            typeBuilder.addOriginatingElement(type);
            typeBuilder.superclass(TypeName.get(type.asType()));
            typeBuilder.addModifiers(Modifier.FINAL);

            for (AspectDescriptor aspectDescriptor : weaveDescriptor.aspects()) {
                TypeName aspectType = TypeName.get(aspectDescriptor.element().asType());
                String aspectFieldName = Names.classToVariable(aspectDescriptor.name());

                FieldSpec.Builder aspectBuilder = FieldSpec.builder(aspectType, aspectFieldName);
                aspectBuilder.addModifiers(Modifier.PRIVATE, Modifier.FINAL);
                if (aspectDescriptor.initialization() == AutoAspect.Initialization.CLASS) {
                    aspectBuilder.addModifiers(Modifier.STATIC);
                }
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

                FieldSpec staticPointcut = StaticPointcut.spec(nameAllocator, types, weaveDescriptor,
                                                               weaveMethodDescriptor);
                typeBuilder.addField(staticPointcut);

                // ****************************************************************************
                // todo(bnorm) method advice arrays

                List<AdviceDescriptor> adviceDescriptors = weaveMethodDescriptor.advice();
                String adviceArrayName = nameAllocator.newName(methodName + "Advice");
                FieldSpec.Builder methodAdviceBuilder = FieldSpec.builder(Advice[].class, adviceArrayName);
                methodAdviceBuilder.addModifiers(Modifier.PRIVATE, Modifier.FINAL);

                StringBuilder format = new StringBuilder();
                Object[] args = new Object[1 + adviceDescriptors.size()];
                args[0] = Advice.class;

                format.append("new $T[] {");
                for (int i = 0, len = adviceDescriptors.size(); i < len; i++) {
                    if (i != 0) {
                        format.append(", ");
                    }
                    format.append('\n').append("$L");

                    AdviceDescriptor adviceDescriptor = adviceDescriptors.get(i);
                    String aspectFieldName = adviceDescriptor.aspect().fieldName();
                    String aspectMethodName = adviceDescriptor.name();
                    args[i + 1] = adviceDescriptor.crosscut().getAdvice(aspectFieldName, aspectMethodName);
                }
                format.append('\n').append("}");

                methodAdviceBuilder.initializer(format.toString(), args);
                FieldSpec adviceArray = methodAdviceBuilder.build();
                typeBuilder.addField(adviceArray);

                // todo(bnorm) method advice arrays
                // ****************************************************************************

                TypeSpec.Builder superBuilder = TypeSpec.anonymousClassBuilder("$L, this, $L, $T.<Object>asList($L)",
                                                                               adviceArrayName, staticPointcut.name,
                                                                               Arrays.class, methodParameters);
                superBuilder.addSuperinterface(Chain.class);
                MethodSpec.Builder superMethodBuilder = MethodSpec.methodBuilder("call")
                                                                  .addAnnotation(Override.class)
                                                                  .addModifiers(Modifier.PUBLIC)
                                                                  .addException(Throwable.class)
                                                                  .returns(Object.class);
                if (returns) {
                    superMethodBuilder.addStatement("return $N.super.$N($L)", autoWeaveTypeName, methodName,
                                                    methodParameters);
                } else {
                    superMethodBuilder.addStatement("$N.super.$N($L)", autoWeaveTypeName, methodName, methodParameters);
                    superMethodBuilder.addStatement("return null");
                }
                superBuilder.addMethod(superMethodBuilder.build());
                TypeSpec chain = superBuilder.build();

                MethodSpec.Builder methodBuilder = overriding(method);
                if (returns) {
                    methodBuilder.addStatement("return ($T) $L.proceed()", TypeName.get(method.getReturnType()), chain);
                } else {
                    methodBuilder.addStatement("$L.proceed()", chain);
                }
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


    private String fqClassNameOf(TypeElement type) {
        String pkg = packageNameOf(type);
        String dot = pkg.isEmpty() ? "" : ".";
        return pkg + dot + classNameOf(type);
    }


    private String classNameOf(TypeElement type) {
        return "AutoWeave_" + type.getSimpleName().toString();
    }

    static String packageNameOf(TypeElement type) {
        while (true) {
            Element enclosing = type.getEnclosingElement();
            if (enclosing instanceof PackageElement) {
                return ((PackageElement) enclosing).getQualifiedName().toString();
            }
            type = (TypeElement) enclosing;
        }
    }


    private void writeSourceFile(JavaFile javaFile, TypeElement originatingType) {
        try {
            JavaFileObject sourceFile = processingEnv.getFiler()
                                                     .createSourceFile(fqClassNameOf(originatingType), originatingType);
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

    private Set<AdviceDescriptor> advice(RoundEnvironment roundEnv) {
        final Map<TypeElement, Set<ExecutableElement>> adviceMap = new LinkedHashMap<>();
        for (Element advice : roundEnv.getElementsAnnotatedWith(AutoAdvice.class)) {
            advice.accept(new SimpleElementVisitor6<Object, Object>() {
                @Override
                protected Object defaultAction(Element e, Object o) {
                    throw new AssertionError("AutoAdvice cannot be applied to anything but a method!");
                }

                @Override
                public Object visitExecutable(ExecutableElement e, Object o) {
                    boolean error = false;

                    List<? extends VariableElement> parameters = e.getParameters();
                    if (parameters.size() != 1) {
                        messager.printMessage(ERROR,
                                              "AutoAdvice can only be applied to a method with a single JoinPoint parameter.",
                                              e);
                        error = true;
                    }

                    // todo(bnorm) check first (and only) parameter extends but is not JoinPoint
                    // todo(bnorm) class should have a default constructor

                    if (error) {
                        return null;
                    }

                    TypeElement type = (TypeElement) e.getEnclosingElement();
                    Set<ExecutableElement> aspectAdvice = adviceMap.get(type);
                    if (aspectAdvice == null) {
                        adviceMap.put(type, aspectAdvice = new LinkedHashSet<>());
                    }
                    aspectAdvice.add(e);
                    return null;
                }
            }, null);
        }

        Set<AdviceDescriptor> adviceDescriptors = new LinkedHashSet<>();
        for (Map.Entry<TypeElement, Set<ExecutableElement>> entry : adviceMap.entrySet()) {
            TypeElement aspect = entry.getKey();
            AutoAspect autoAspect = aspect.getAnnotation(AutoAspect.class);
            AutoAspect.Initialization initialization = autoAspect != null ? autoAspect.init() : AutoAspect.Initialization.INSTANCE;
            AspectDescriptor aspectDescriptor = AspectDescriptor.create(aspect, initialization);
            for (ExecutableElement advice : entry.getValue()) {
                CrosscutEnum crosscut = getCrosscut(advice);
                AutoAdvice autoAdvice = advice.getAnnotation(AutoAdvice.class);
                Set<TypeMirror> targets = new LinkedHashSet<>(valueFrom(autoAdvice));
                adviceDescriptors.add(AdviceDescriptor.create(aspectDescriptor, advice, crosscut, targets));
            }
        }
        return adviceDescriptors;
    }

    private Set<WeaveDescriptor> weave(RoundEnvironment roundEnv, Set<AdviceDescriptor> advice) {
        final Map<TypeMirror, Set<AdviceDescriptor>> annotationMap = new LinkedHashMap<>();
        for (AdviceDescriptor descriptor : advice) {
            for (TypeMirror target : descriptor.targets()) {
                Set<AdviceDescriptor> adviceDescriptors = annotationMap.get(target);
                if (adviceDescriptors == null) {
                    annotationMap.put(target, adviceDescriptors = new LinkedHashSet<>());
                }
                adviceDescriptors.add(descriptor);
            }
        }

        final Set<WeaveDescriptor> weaveDescriptorBuilder = new LinkedHashSet<>();
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
                    List<WeaveMethodDescriptor> weaveMethodDescriptorBuilder = new ArrayList<>();
                    process(e, weaveMethodDescriptorBuilder);
                    weaveDescriptorBuilder.add(WeaveDescriptor.create(e, weaveMethodDescriptorBuilder));
                    return null;
                }

                private void process(TypeElement e, List<WeaveMethodDescriptor> weaveMethodDescriptorBuilder) {
                    TypeMirror superclass = e.getSuperclass();
                    if (!(superclass instanceof NoType)) {
                        process((TypeElement) types.asElement(superclass), weaveMethodDescriptorBuilder);
                    }

                    // todo(bnorm) how do override methods fit in?
                    for (ExecutableElement element : ElementFilter.methodsIn(e.getEnclosedElements())) {
                        Set<AdviceDescriptor> adviceDescriptors = new LinkedHashSet<>();
                        for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
                            Set<AdviceDescriptor> descriptors = annotationMap.get(annotationMirror.getAnnotationType());
                            if (descriptors != null) {
                                adviceDescriptors.addAll(descriptors);
                            }
                        }

                        if (!adviceDescriptors.isEmpty()) {
                            weaveMethodDescriptorBuilder.add(
                                    WeaveMethodDescriptor.create(element, new ArrayList<>(adviceDescriptors)));
                        }
                    }
                }
            }, null);
        }
        return weaveDescriptorBuilder;
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
