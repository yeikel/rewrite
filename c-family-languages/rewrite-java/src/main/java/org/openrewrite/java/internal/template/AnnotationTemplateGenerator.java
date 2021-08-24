/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.internal.template;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.openrewrite.Cursor;
import org.openrewrite.family.c.tree.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.newSetFromMap;

@RequiredArgsConstructor
public class AnnotationTemplateGenerator {
    private static final String TEMPLATE_COMMENT = "__TEMPLATE_cfcc2025-6662__";

    private final Set<String> imports;

    public String template(Cursor cursor, String template) {
        //noinspection ConstantConditions
        return Timer.builder("rewrite.template.generate.statement")
                .register(Metrics.globalRegistry)
                .record(() -> {
                    StringBuilder before = new StringBuilder();
                    StringBuilder after = new StringBuilder();

                    template(next(cursor), cursor.getValue(), before, after, newSetFromMap(new IdentityHashMap<>()));

                    C j = cursor.getValue();
                    if (j instanceof C.MethodDeclaration) {
                        after.insert(0, " void $method() {}");
                    } else if (j instanceof C.VariableDeclarations) {
                        after.insert(0, " int $variable;");
                    } else if (j instanceof C.ClassDeclaration) {
                        after.insert(0, "static class $Clazz {}");
                    }

                    if (cursor.getParentOrThrow().getValue() instanceof C.ClassDeclaration &&
                            cursor.getParentOrThrow().getParentOrThrow().getValue() instanceof J.CompilationUnit) {
                        after.append("class $Template {}");
                    }

                    return before + "/*" + TEMPLATE_COMMENT + "*/" + template + "\n" + after;
                });
    }

    public List<C.Annotation> listAnnotations(J.CompilationUnit cu) {
        List<C.Annotation> annotations = new ArrayList<>();

        new JavaIsoVisitor<Integer>() {
            @Nullable
            private Comment filterTemplateComment(Comment comment) {
                return comment instanceof TextComment && ((TextComment) comment).getText().equals(TEMPLATE_COMMENT) ?
                        null : comment;
            }

            @Override
            public C.Annotation visitAnnotation(C.Annotation annotation, Integer integer) {
                C.Annotation withoutTemplateComment = annotation.withComments(
                        ListUtils.concatAll(
                                ListUtils.map(getCursor().getParentOrThrow().<J>getValue().getComments(), this::filterTemplateComment),
                                ListUtils.map(annotation.getComments(), this::filterTemplateComment)
                        ));
                annotations.add(withoutTemplateComment);
                return annotation;
            }
        }.visit(cu, 0);

        return annotations;
    }

    private void template(Cursor cursor, C prior, StringBuilder before, StringBuilder after, Set<C> templated) {
        templated.add(cursor.getValue());
        C j = cursor.getValue();
        if (j instanceof J.CompilationUnit) {
            J.CompilationUnit cu = (J.CompilationUnit) j;
            for (J.Import anImport : cu.getImports()) {
                before.insert(0, J.printTrimmed(anImport.withPrefix(Space.EMPTY)) + ";\n");
            }
            for (String anImport : imports) {
                before.insert(0, anImport);
            }

            if (cu.getPackageDeclaration() != null) {
                before.insert(0, J.printTrimmed(cu.getPackageDeclaration().withPrefix(Space.EMPTY)) + ";\n");
            }
            List<C.ClassDeclaration> classes = cu.getClasses();
            if (!classes.get(classes.size() - 1).getName().getSimpleName().equals("$Placeholder")) {
                after.append("@interface $Placeholder {}");
            }
            return;
        }
        if (j instanceof C.Block) {
            C parent = next(cursor).getValue();
            if (parent instanceof C.ClassDeclaration) {
                classDeclaration(before, (C.ClassDeclaration) parent, templated);
                after.append('}');
            } else if (parent instanceof C.MethodDeclaration) {
                C.MethodDeclaration m = (C.MethodDeclaration) parent;

                // variable declarations up to the point of insertion
                assert m.getBody() != null;
                for (Statement statement : m.getBody().getStatements()) {
                    if (statement == prior) {
                        break;
                    } else if (statement instanceof C.VariableDeclarations) {
                        before.insert(0, "\n" +
                                variable((C.VariableDeclarations) statement) +
                                ";\n");
                    }
                }

                if (m.getReturnTypeExpression() != null && !CType.Primitive.Void
                        .equals(m.getReturnTypeExpression().getType())) {
                    after.append("return ")
                            .append(valueOfType(m.getReturnTypeExpression().getType()))
                            .append(";\n");
                }

                before.insert(0, J.printTrimmed(m.withBody(null)
                        .withLeadingAnnotations(emptyList())
                        .withPrefix(Space.EMPTY)) + '{');
                after.append('}');
            } else if (parent instanceof C.Block) {
                C.Block b = (C.Block) j;

                // variable declarations up to the point of insertion
                for (Statement statement : b.getStatements()) {
                    if (statement == prior) {
                        break;
                    } else if (statement instanceof C.VariableDeclarations) {
                        C.VariableDeclarations v = (C.VariableDeclarations) statement;
                        if (v.hasModifier(C.Modifier.Type.Final)) {
                            before.insert(0, "\n" + variable(v) + ";\n");
                        }
                    }
                }

                before.insert(0, "{\n");
                if (b.isStatic()) {
                    before.insert(0, "static");
                }
                after.append('}');
            }
        } else if (j instanceof C.VariableDeclarations) {
            C.VariableDeclarations v = (C.VariableDeclarations) j;
            if (v.hasModifier(C.Modifier.Type.Final)) {
                before.insert(0, variable((C.VariableDeclarations) j) + '=');
            }
        } else if (j instanceof C.NewClass) {
            C.NewClass n = (C.NewClass) j;
            n = n.withBody(null).withPrefix(Space.EMPTY);
            before.insert(0, '{');
            before.insert(0, J.printTrimmed(n).trim());
            after.append("};");
        }

        template(next(cursor), j, before, after, templated);
    }

    private void classDeclaration(StringBuilder before, C.ClassDeclaration parent, Set<C> templated) {
        C.ClassDeclaration c = parent;
        for (Statement statement : c.getBody().getStatements()) {
            if (templated.contains(statement)) {
                continue;
            }

            if (statement instanceof C.VariableDeclarations) {
                C.VariableDeclarations v = (C.VariableDeclarations) statement;
                if (v.hasModifier(C.Modifier.Type.Final) && v.hasModifier(C.Modifier.Type.Static)) {
                    before.insert(0, variable((C.VariableDeclarations) statement) + ";\n");
                }
            } else if (statement instanceof C.ClassDeclaration) {
                // this is a sibling class. we need declarations for all variables and methods.
                // setting prior to null will cause them all to be written.
                before.insert(0, '}');
                classDeclaration(before, (C.ClassDeclaration) statement, templated);
            }
        }
        c = c.withBody(null).withLeadingAnnotations(null).withPrefix(Space.EMPTY);
        before.insert(0, J.printTrimmed(c).trim() + '{');
    }

    private String variable(C.VariableDeclarations variable) {
        StringBuilder varBuilder = new StringBuilder();
        if (variable.getTypeExpression() != null) {
            for (C.Modifier modifier : variable.getModifiers()) {
                varBuilder.append(modifier.getType().toString().toLowerCase()).append(' ');
            }
            varBuilder.append(J.printTrimmed((C) variable.getTypeExpression().withPrefix(Space.EMPTY)))
                    .append(' ');
        }

        List<C.VariableDeclarations.NamedVariable> variables = variable.getVariables();
        for (int i = 0, variablesSize = variables.size(); i < variablesSize; i++) {
            C.VariableDeclarations.NamedVariable nv = variables.get(i);
            varBuilder.append(nv.getSimpleName());

            if (i < variables.size() - 1) {
                varBuilder.append(',');
            }
        }

        return varBuilder.toString();
    }

    private String valueOfType(@Nullable CType type) {
        CType.Primitive primitive = TypeUtils.asPrimitive(type);
        if (primitive != null) {
            switch (primitive) {
                case Boolean:
                    return "true";
                case Byte:
                case Char:
                case Int:
                case Double:
                case Float:
                case Long:
                case Short:
                    return "0";
                case String:
                case Null:
                    return "null";
                case None:
                case Wildcard:
                case Void:
                default:
                    return "";
            }
        }

        return "null";
    }

    private Cursor next(Cursor c) {
        return c.dropParentUntil(C.class::isInstance);
    }
}
