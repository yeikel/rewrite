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
package org.openrewrite.family.c.format;

import org.openrewrite.Tree;
import org.openrewrite.family.c.CIsoVisitor;
import org.openrewrite.family.c.tree.C;
import org.openrewrite.family.c.tree.CSourceFile;
import org.openrewrite.family.c.tree.Space;
import org.openrewrite.family.c.tree.Statement;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.family.c.style.WrappingAndBracesStyle;

import java.util.List;

public class WrappingAndBracesVisitor<P> extends CIsoVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final WrappingAndBracesStyle style;

    public WrappingAndBracesVisitor(WrappingAndBracesStyle style) {
        this(style, null);
    }

    public WrappingAndBracesVisitor(WrappingAndBracesStyle style, @Nullable Tree stopAfter) {
        this.style = style;
        this.stopAfter = stopAfter;
    }

    @Override
    public Statement visitStatement(Statement statement, P p) {
        Statement c = super.visitStatement(statement, p);
        C parentTree = getCursor().dropParentUntil(C.class::isInstance).getValue();
        if (parentTree instanceof C.Block) {
            if (!c.getPrefix().getWhitespace().contains("\n")) {
                c = c.withPrefix(withNewline(c.getPrefix()));
            }
        }

        return c;
    }

    @Override
    public C.VariableDeclarations visitVariableDeclarations(C.VariableDeclarations multiVariable, P p) {

        C.VariableDeclarations variableDeclarations = super.visitVariableDeclarations(multiVariable, p);
        if (getCursor().getParent() != null && getCursor().getParent().firstEnclosing(C.class) instanceof C.Block) {

            variableDeclarations = variableDeclarations.withLeadingAnnotations(withNewlines(variableDeclarations.getLeadingAnnotations()));

            if (!variableDeclarations.getLeadingAnnotations().isEmpty()) {
                if (!variableDeclarations.getModifiers().isEmpty()) {
                    variableDeclarations = variableDeclarations.withModifiers(withNewline(variableDeclarations.getModifiers()));
                } else if (variableDeclarations.getTypeExpression() != null &&
                        !variableDeclarations.getTypeExpression().getPrefix().getWhitespace().contains("\n")) {
                    variableDeclarations = variableDeclarations.withTypeExpression(
                            variableDeclarations.getTypeExpression().withPrefix(withNewline(variableDeclarations.getTypeExpression().getPrefix()))
                    );
                }
            }
        }
        return variableDeclarations;
    }

    @Override
    public C.MethodDeclaration visitMethodDeclaration(C.MethodDeclaration method, P p) {
        C.MethodDeclaration m = super.visitMethodDeclaration(method, p);
        // TODO make annotation wrapping configurable
        m = m.withLeadingAnnotations(withNewlines(m.getLeadingAnnotations()));
        if (!m.getLeadingAnnotations().isEmpty()) {
            if (!m.getModifiers().isEmpty()) {
                m = m.withModifiers(withNewline(m.getModifiers()));
            } else if (m.getAnnotations().getTypeParameters() != null) {
                if (!m.getAnnotations().getTypeParameters().getPrefix().getWhitespace().contains("\n")) {
                    m = m.getAnnotations().withTypeParameters(
                            m.getAnnotations().getTypeParameters().withPrefix(
                                    withNewline(m.getAnnotations().getTypeParameters().getPrefix())
                            )
                    );
                }
            } else if (m.getReturnTypeExpression() != null) {
                if (!m.getReturnTypeExpression().getPrefix().getWhitespace().contains("\n")) {
                    m = m.withReturnTypeExpression(
                            m.getReturnTypeExpression().withPrefix(
                                    withNewline(m.getReturnTypeExpression().getPrefix())
                            )
                    );
                }
            } else {
                if (!m.getName().getPrefix().getWhitespace().contains("\n")) {
                    m = m.withName(
                            m.getName().withPrefix(
                                    withNewline(m.getName().getPrefix())
                            )
                    );
                }
            }
        }
        return m;
    }

    @Override
    public C.ClassDeclaration visitClassDeclaration(C.ClassDeclaration classDecl, P p) {
        C.ClassDeclaration j = super.visitClassDeclaration(classDecl, p);
        // TODO make annotation wrapping configurable
        j = j.withLeadingAnnotations(withNewlines(j.getLeadingAnnotations()));
        if (!j.getLeadingAnnotations().isEmpty()) {
            if (!j.getModifiers().isEmpty()) {
                j = j.withModifiers(withNewline(j.getModifiers()));
            } else {
                C.ClassDeclaration.Kind kind = j.getAnnotations().getKind();
                if (!kind.getPrefix().getWhitespace().contains("\n")) {
                    j = j.getAnnotations().withKind(kind.withPrefix(
                            kind.getPrefix().withWhitespace("\n" + kind.getPrefix().getWhitespace())
                    ));
                }
            }

        }
        return j;
    }

    private List<C.Annotation> withNewlines(List<C.Annotation> annotations) {
        if (annotations.isEmpty()) {
            return annotations;
        }
        return ListUtils.map(annotations, (index, a) -> {
            if (index != 0 && !a.getPrefix().getWhitespace().contains("\n")) {
                a = a.withPrefix(withNewline(a.getPrefix()));
            }
            return a;
        });
    }

    @Override
    public C.Block visitBlock(C.Block block, P p) {
        C.Block b = super.visitBlock(block, p);
        if(!b.getEnd().getWhitespace().contains("\n")) {
            b = b.withEnd(withNewline(b.getEnd()));
        }
        return b;
    }

    private Space withNewline(Space space) {
        if (space.getComments().isEmpty()) {
            space = space.withWhitespace("\n" + space.getWhitespace());
        } else if (space.getComments().get(space.getComments().size()-1).isMultiline()) {
            space = space.withComments(ListUtils.mapLast(space.getComments(), c -> c.withSuffix("\n")));
        }

        return space;
    }

    private List<C.Modifier> withNewline(List<C.Modifier> modifiers) {
        C.Modifier firstModifier = modifiers.iterator().next();
        if (!firstModifier.getPrefix().getWhitespace().contains("\n")) {
            return ListUtils.mapFirst(modifiers,
                    mod -> mod.withPrefix(
                            withNewline(mod.getPrefix())
                    )
            );
        }
        return modifiers;
    }

    @Nullable
    @Override
    public C postVisit(C tree, P p) {
        if (stopAfter != null && stopAfter.isScope(tree)) {
            getCursor().putMessageOnFirstEnclosing(CSourceFile.class, "stop", true);
        }
        return super.postVisit(tree, p);
    }

    @Nullable
    @Override
    public C visit(@Nullable Tree tree, P p) {
        if (getCursor().getNearestMessage("stop") != null) {
            return (C) tree;
        }
        return super.visit(tree, p);
    }
}
