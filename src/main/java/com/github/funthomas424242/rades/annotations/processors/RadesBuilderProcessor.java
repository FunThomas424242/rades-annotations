package com.github.funthomas424242.rades.annotations.processors;

import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SupportedAnnotationTypes("com.github.funthomas424242.rades.annotations.RadesBuilder")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class RadesBuilderProcessor extends AbstractProcessor {

    protected ProcessingEnvironment processingEnvironment;

    @Override
    public synchronized void init(final ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.processingEnvironment = processingEnv;
    }

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        final Types types = this.processingEnvironment.getTypeUtils();

        for (TypeElement annotation : annotations) {
            final Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);
            for (final Element annotatedElement : annotatedElements) {
                final TypeElement typeElement = (TypeElement) annotatedElement;
                final Map<Name, Name> mapName2Type = new HashMap<>();
                final List<? extends Element> classMembers = annotatedElement.getEnclosedElements();
                for (final Element classMember : classMembers) {
                    if (classMember.getKind().isField()) {
                        final Set<Modifier> fieldModifiers = classMember.getModifiers();
                        if (fieldModifiers.contains(Modifier.PUBLIC) || fieldModifiers.contains(Modifier.PROTECTED)) {
                            final Name fieldName = classMember.getSimpleName();
                            final TypeMirror fieldTypeMirror = classMember.asType();
                            final Element fieldTypeElement = types.asElement(fieldTypeMirror);

                            mapName2Type.put(fieldName, fieldTypeElement.getSimpleName());
                        }
                    }
                }

                final Name className = typeElement.getQualifiedName();
                try {
                    writeBuilderFile(className, mapName2Type);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


//            final Map<Boolean, List<Element>> annotatedClasses = annotatedElements.stream().collect(
//                    Collectors.partitioningBy(element ->
//                            ((ExecutableType) element.asType()).getParameterTypes().size() == 1
//                                    && element.getSimpleName().toString().startsWith("set")));
//
//            final List<Element> setters = annotatedClasses.get(true);
//            final List<Element> otherMethods = annotatedClasses.get(false);
//
//
//            otherMethods.forEach(element ->
//                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
//                            "@RadesBuilder must be applied to a setXxx method "
//                                    + "with a single argument", element));
//            if (setters.isEmpty()) {
//                continue;
//            }
//
//            final String className = ((TypeElement) setters.get(0)
//                    .getEnclosingElement()).getQualifiedName().toString();
//
//
//            final Map<String, String> setterMap = setters.stream().collect(Collectors.toMap(
//                    setter -> setter.getSimpleName().toString(),
//                    setter -> ((ExecutableType) setter.asType())
//                            .getParameterTypes().get(0).toString()
//            ));
//            try {
//                writeBuilderFile(className,setterMap);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }


        }

        return true;
    }

    private void writeBuilderFile(final Name typeName, Map<Name, Name> mapFieldName2Type)
            throws IOException {

        final String className = typeName.toString();

        String packageName = null;
        int lastDot = className.lastIndexOf('.');
        if (lastDot > 0) {
            packageName = className.substring(0, lastDot);
        }

        String simpleClassName = className.substring(lastDot + 1);
        String builderClassName = className + "Builder";
        String builderSimpleClassName = builderClassName
                .substring(lastDot + 1);

        JavaFileObject builderFile = processingEnv.getFiler()
                .createSourceFile(builderClassName);

        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {

            if (packageName != null) {
                out.print("package ");
                out.print(packageName);
                out.println(";");
                out.println();
            }

            out.print("public class ");
            out.print(builderSimpleClassName);
            out.println(" {");
            out.println();

            out.print("    private ");
            out.print(simpleClassName);
            out.print(" object = new ");
            out.print(simpleClassName);
            out.println("();");
            out.println();

            out.print("    public ");
            out.print(simpleClassName);
            out.println(" build() {");
            out.println("        return object;");
            out.println("    }");
            out.println();

            mapFieldName2Type.entrySet().forEach(fields -> {
                String fieldName = fields.getKey().toString();
                String methodName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                String argumentType = fields.getValue().toString();

                out.print("    public ");
                out.print(builderSimpleClassName);
                out.print(" ");
                out.print(methodName);

                out.print("(");

                out.print(argumentType);
                out.println(" value) {");
                out.print("        object.");
                out.print(methodName);
                out.println("(value);");
                out.println("        return this;");
                out.println("    }");
                out.println();
            });

            out.println("}");
        }
    }

}
