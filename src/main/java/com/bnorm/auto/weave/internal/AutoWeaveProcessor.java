package com.bnorm.auto.weave.internal;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.NoType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import com.bnorm.auto.weave.AutoAspect;
import com.bnorm.auto.weave.AutoWeave;
import com.bnorm.auto.weave.internal.chain.Chain;
import com.bnorm.auto.weave.internal.chain.MethodChain;
import com.bnorm.auto.weave.internal.chain.VoidMethodChain;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

@AutoService(Processor.class)
public class AutoWeaveProcessor extends AbstractProcessor {

    private Types typeUtils;

    public AutoWeaveProcessor() {
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return ImmutableSet.of(AutoWeave.class.getName(), AutoAspect.class.getName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        typeUtils = processingEnv.getTypeUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        List<AutoWeaveType> types = getAutoWeaveTypes(roundEnv);

        for (AutoWeaveType autoWeaveType : types) {
            TypeElement type = autoWeaveType.type();
            String typeName = type.getSimpleName().toString();
            String autoWeaveTypeName = "AutoWeave_" + typeName;
            TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(autoWeaveTypeName);
            typeBuilder.addOriginatingElement(type);
            typeBuilder.superclass(TypeName.get(type.asType()));
            for (Modifier modifier : type.getModifiers()) {
                if (modifier != Modifier.ABSTRACT) {
                    typeBuilder.addModifiers(modifier);
                }
            }
            typeBuilder.addModifiers(Modifier.FINAL);

            for (TypeElement typeElement : autoWeaveType.aspects()) {
                TypeName aspectType = TypeName.get(typeElement.asType());
                String aspectTypeName = typeElement.getSimpleName().toString();
                String aspectFieldName = Character.toLowerCase(aspectTypeName.charAt(0)) + aspectTypeName.substring(1);

                // todo(bnorm) should this be static?
                FieldSpec.Builder aspectBuilder = FieldSpec.builder(aspectType, aspectFieldName);
                aspectBuilder.addModifiers(Modifier.PRIVATE, Modifier.FINAL);
                aspectBuilder.initializer("new $T()", aspectType);
                typeBuilder.addField(aspectBuilder.build());
            }

            for (AutoWeaveMethod autoWeaveMethod : autoWeaveType.methods()) {
                ExecutableElement method = autoWeaveMethod.method();
                String methodName = method.getSimpleName().toString();
                String pointcutName = methodName + "Pointcut";
                boolean returns = !(method.getReturnType() instanceof NoType);

                FieldSpec.Builder pointcutBuilder = FieldSpec.builder(Pointcut.class, pointcutName);
                pointcutBuilder.addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL);
                pointcutBuilder.initializer("$T.create($S)", Pointcut.class, methodName);
                typeBuilder.addField(pointcutBuilder.build());

                MethodSpec.Builder methodBuilder = MethodSpec.overriding(method);
                methodBuilder.addStatement("$T chain", Chain.class);
                methodBuilder.addCode("");

                TypeSpec.Builder superBuilder = TypeSpec.anonymousClassBuilder("");
                superBuilder.addSuperinterface(returns ? MethodChain.class : VoidMethodChain.class);
                superBuilder.addMethod(MethodSpec.methodBuilder(methodName)
                                                 .addAnnotation(Override.class)
                                                 .addModifiers(Modifier.PUBLIC)
                                                 .addException(Throwable.class)
                                                 .returns(returns ? Object.class : void.class)
                                                 .addStatement((returns ? "return " : "") + "$N.super.$N()",
                                                               autoWeaveTypeName, methodName)
                                                 .build());

                methodBuilder.addStatement("chain = $L", superBuilder.build());

                for (AutoAspectMethod aspectMethod : autoWeaveMethod.aspectMethods()) {
                    String aspectFieldName = aspectMethod.aspect().fieldName();
                    String aspectMethodName = aspectMethod.name();

                    methodBuilder.addStatement("chain = $L", aspectMethod.crosscut()
                                                                         .getChain(pointcutName, aspectFieldName,
                                                                                   aspectMethodName));
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
                callBuilder.nextControlFlow("catch ($T e)", Throwable.class);
                {
                    callBuilder.beginControlFlow("if (e instanceof $T)", Error.class);
                    {
                        callBuilder.addStatement("throw ($T) e", Error.class);
                    }
                    callBuilder.nextControlFlow("else if (e instanceof $T)", RuntimeException.class);
                    {
                        callBuilder.addStatement("throw ($T) e", RuntimeException.class);
                    }
                    callBuilder.nextControlFlow("else");
                    {
                        callBuilder.addStatement("throw new $T($S, e)", AssertionError.class,
                                                 "Please contact the library developer");
                    }
                    callBuilder.endControlFlow();
                }
                callBuilder.endControlFlow();
                methodBuilder.addCode(callBuilder.build());

                typeBuilder.addMethod(methodBuilder.build());
            }

            PackageElement packageElement = (PackageElement) autoWeaveType.type().getEnclosingElement();
            JavaFile javaFile = JavaFile.builder(packageElement.getQualifiedName().toString(), typeBuilder.build())
                                        .build();
            writeSourceFile(javaFile, autoWeaveType.type());
        }
        return !types.isEmpty();
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

    private List<AutoWeaveType> getAutoWeaveTypes(RoundEnvironment roundEnv) {
        Map<AutoAspect, ExecutableElement> aspects = new HashMap<>();
        for (Element aspect : roundEnv.getElementsAnnotatedWith(AutoAspect.class)) {
            // todo(bnorm) should be an executable element
            // todo(bnorm) first parameter should extend JoinPoint but not be JoinPoint
            // todo(bnorm) class should have a default constructor
            AutoAspect annotation = aspect.getAnnotation(AutoAspect.class);
            assert annotation != null;
            aspects.put(annotation, (ExecutableElement) aspect);
        }


        // These are the classes to weave
        Collection<? extends Element> weaved = roundEnv.getElementsAnnotatedWith(AutoWeave.class);

        // These are the classes to instantiate at the top of the class
        Map<TypeElement, Set<TypeElement>> weavedAspects = new HashMap<>(weaved.size());
        for (Element element : weaved) {
            AutoWeave annotation = element.getAnnotation(AutoWeave.class);
            assert annotation != null;
            weavedAspects.put((TypeElement) element, new HashSet<TypeElement>());
        }

        // These are the methods that need to be overriden
        Map<TypeElement, Map<ExecutableElement, Set<AutoAspectMethod>>> weavedMethods = new HashMap<>(weaved.size());
        for (Element element : weaved) {
            AutoWeave annotation = element.getAnnotation(AutoWeave.class);
            assert annotation != null;
            weavedMethods.put((TypeElement) element, new HashMap<ExecutableElement, Set<AutoAspectMethod>>());
        }


        for (Element aspect : roundEnv.getElementsAnnotatedWith(AutoAspect.class)) {
            AutoAspect autoAspect = aspect.getAnnotation(AutoAspect.class);
            List<? extends TypeMirror> typeMirrors = valueFrom(autoAspect);
            for (TypeMirror targetWeave : typeMirrors) {
                TypeElement annotation = (TypeElement) processingEnv.getTypeUtils().asElement(targetWeave);
                for (Element weaveMethod : roundEnv.getElementsAnnotatedWith(annotation)) {
                    if (!(weaveMethod instanceof ExecutableElement)) {
                        throw new IllegalArgumentException("Weaving anything but a method is not allowed");
                    }

                    TypeElement weavedClass = (TypeElement) weaveMethod.getEnclosingElement();
                    AutoWeave autoWeave = weavedClass.getAnnotation(AutoWeave.class);
                    if (autoWeave == null) {
                        // todo(bnorm) this is okay, we just won't weave
                        throw new IllegalStateException("Cannot weave class " + weaveMethod);
                    }

                    // make sure containing class has default constructor

                    Map<ExecutableElement, Set<AutoAspectMethod>> methods = weavedMethods.get(weavedClass);
                    assert methods != null;

                    Set<TypeElement> classes = weavedAspects.get(weavedClass);
                    assert classes != null;

                    ExecutableElement aspectMethod = (ExecutableElement) aspect;
                    TypeElement aspectType = (TypeElement) aspectMethod.getEnclosingElement();
                    classes.add(aspectType);

                    Set<AutoAspectMethod> methodAspects = methods.get(weaveMethod);
                    if (methodAspects == null) {
                        methodAspects = new LinkedHashSet<>();
                        methods.put((ExecutableElement) weaveMethod, methodAspects);
                    }


                    List<? extends VariableElement> parameters = aspectMethod.getParameters();
                    if (parameters.size() != 1) {
                        throw new IllegalStateException("AutoAspect can only be on method with a single parameter");
                    }
                    String name = parameters.get(0).asType().toString();
                    CrosscutEnum crosscut = CrosscutEnum.crosscutMap.get(name);

                    AutoAspectType autoAspectType = AutoAspectType.create(aspectType);
                    AutoAspectMethod autoAspectMethod = AutoAspectMethod.create(aspectMethod, crosscut, autoAspectType);
                    methodAspects.add(autoAspectMethod);
                }
            }
        }

        List<AutoWeaveType> types = new ArrayList<>(weavedMethods.size());
        for (Map.Entry<TypeElement, Map<ExecutableElement, Set<AutoAspectMethod>>> typeElement : weavedMethods.entrySet()) {
            List<AutoWeaveMethod> methods = new ArrayList<>(typeElement.getValue().size());
            for (Map.Entry<ExecutableElement, Set<AutoAspectMethod>> methodElement : typeElement.getValue()
                                                                                                .entrySet()) {
                AutoWeaveMethod method = AutoWeaveMethod.create(methodElement.getKey(),
                                                                new ArrayList<>(methodElement.getValue()));
                methods.add(method);
            }
            AutoWeaveType type = AutoWeaveType.create(typeElement.getKey(),
                                                      new ArrayList<>(weavedAspects.get(typeElement.getKey())),
                                                      methods);
            types.add(type);
        }
        return types;
    }

    private List<? extends TypeMirror> valueFrom(AutoAspect autoAspect) {
        List<? extends TypeMirror> typeMirrors = null;
        try {
            autoAspect.value();
        } catch (MirroredTypesException e) {
            typeMirrors = e.getTypeMirrors();
        }
        assert typeMirrors != null;
        return typeMirrors;
    }
}
