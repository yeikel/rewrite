/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.marker;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import io.github.classgraph.*;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.openrewrite.Tree;
import org.openrewrite.family.c.tree.CType;
import org.openrewrite.family.c.tree.Flag;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Marker;

import java.nio.file.Path;
import java.util.*;

import static java.util.Collections.emptyList;

@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
@With
public class JavaSourceSet implements Marker {
    @EqualsAndHashCode.Include
    UUID id;

    String name;
    Set<CType.FullyQualified> classpath;

    public static JavaSourceSet build(String sourceSetName, Iterable<Path> classpath) {
        Set<CType.FullyQualified> fqns = new HashSet<>();
        if (classpath.iterator().hasNext()) {
            for (ClassInfo classInfo : new ClassGraph()
                    .overrideClasspath(classpath)
                    .enableAnnotationInfo()
                    .enableMemoryMapping()
                    .enableClassInfo()
                    .enableMethodInfo()
                    .enableFieldInfo()
                    .scan()
                    .getAllClasses()) {
                fqns.add(fromClassGraph(classInfo, new Stack<>()));
            }

            for (ClassInfo classInfo : new ClassGraph()
                    .enableMemoryMapping()
                    .enableAnnotationInfo()
                    .enableClassInfo()
                    .enableMethodInfo()
                    .enableFieldInfo()
                    .enableSystemJarsAndModules()
                    .acceptPackages("java")
                    .scan()
                    .getAllClasses()) {
                fqns.add(fromClassGraph(classInfo, new Stack<>()));
            }
        }

        return new JavaSourceSet(Tree.randomId(), sourceSetName, fqns);
    }

    private static CType.FullyQualified fromClassGraph(ClassInfo aClass, Stack<ClassInfo> stack) {
        CType.Class existing = CType.Class.find(aClass.getName());
        if (existing != null) {
            return existing;
        }

        if (stack.contains(aClass)) {
            return new CType.ShallowClass(aClass.getName());
        }

        stack.add(aClass);

        Set<Flag> flags = Flag.bitMapToFlags(aClass.getModifiers());

        CType.Class.Kind kind;
        if (aClass.isInterface()) {
            kind = CType.Class.Kind.Interface;
        } else if (aClass.isEnum()) {
            kind = CType.Class.Kind.Enum;
        } else if (aClass.isAnnotation()) {
            kind = CType.Class.Kind.Annotation;
        } else {
            kind = CType.Class.Kind.Class;
        }

        List<CType.Variable> variables = fromFieldInfo(aClass.getFieldInfo());
        List<CType.Method> methods = fromMethodInfo(aClass.getMethodInfo(), stack);

        return CType.Class.build(
                Flag.flagsToBitMap(flags),
                aClass.getName(),
                kind,
                variables,
                new ArrayList<>(),
                methods,
                null,
                null,
                new ArrayList<>(),
                false);
    }

    private static List<CType.Variable> fromFieldInfo(@Nullable FieldInfoList fieldInfos) {
        if (fieldInfos != null) {
            List<CType.Variable> variables = new ArrayList<>(fieldInfos.size());
            for (FieldInfo fieldInfo : fieldInfos) {
                Set<Flag> flags = Flag.bitMapToFlags(fieldInfo.getModifiers());
                CType.Variable variable = CType.Variable.build(fieldInfo.getName(), CType.buildType(fieldInfo.getTypeDescriptor().toString()), Flag.flagsToBitMap(flags));
                variables.add(variable);
            }
            return variables;
        }
        return emptyList();
    }

    private static List<CType.Method> fromMethodInfo(MethodInfoList methodInfos, Stack<ClassInfo> stack) {
        List<CType.Method> methods = new ArrayList<>(methodInfos.size());
        for (MethodInfo methodInfo : methodInfos) {
            Set<Flag> flags = Flag.bitMapToFlags(methodInfo.getModifiers());
            List<CType> parameterTypes = new ArrayList<>(methodInfo.getParameterInfo().length);
            for (MethodParameterInfo methodParameterInfo : methodInfo.getParameterInfo()) {
                parameterTypes.add(CType.buildType(methodParameterInfo.getTypeDescriptor().toString()));
            }

            CType.Method.Signature signature = new CType.Method.Signature(CType.buildType(methodInfo.getTypeDescriptor().getResultType().toString()), parameterTypes);

            List<String> methodParams = new ArrayList<>(methodInfo.getParameterInfo().length);
            for (MethodParameterInfo methodParameterInfo : methodInfo.getParameterInfo()) {
                methodParams.add(methodParameterInfo.getName());
            }

            List<CType.FullyQualified> thrownExceptions = new ArrayList<>(methodInfo.getTypeDescriptor()
                    .getThrowsSignatures().size());
            for (ClassRefOrTypeVariableSignature throwsSignature : methodInfo.getTypeDescriptor().getThrowsSignatures()) {
                if (throwsSignature instanceof ClassRefTypeSignature) {
                    thrownExceptions.add(fromClassGraph(((ClassRefTypeSignature) throwsSignature).getClassInfo(), stack));
                }
            }

            List<CType.FullyQualified> annotations = new ArrayList<>(methodInfo.getAnnotationInfo().size());
            for (AnnotationInfo annotationInfo : methodInfo.getAnnotationInfo()) {
                annotations.add(fromClassGraph(annotationInfo.getClassInfo(), stack));
            }

            methods.add(CType.Method.build(
                    flags,
                    CType.Class.build(methodInfo.getClassName()),
                    methodInfo.getName(),
                    null,
                    signature,
                    methodParams,
                    thrownExceptions,
                    annotations));
        }
        return methods;
    }
}
