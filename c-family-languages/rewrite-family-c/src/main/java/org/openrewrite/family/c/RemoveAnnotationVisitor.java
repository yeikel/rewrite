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
package org.openrewrite.family.c;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.family.c.tree.C;
import org.openrewrite.family.c.tree.Space;
import org.openrewrite.family.c.tree.TypeUtils;

import java.util.ArrayList;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = true)
public class RemoveAnnotationVisitor extends CIsoVisitor<ExecutionContext> {
    AnnotationMatcher annotationMatcher;

    @Override
    public C.ClassDeclaration visitClassDeclaration(C.ClassDeclaration classDecl, ExecutionContext ctx) {
        C.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);
        C.Annotation annotationRemoved = getCursor().pollMessage("annotationRemoved");

        List<C.Annotation> leadingAnnotations = classDecl.getLeadingAnnotations();
        if (annotationRemoved != null && !leadingAnnotations.isEmpty()) {
            if (leadingAnnotations.get(0) == annotationRemoved && leadingAnnotations.size() == 1) {
                if (!c.getModifiers().isEmpty()) {
                    c = c.withModifiers(Space.formatFirstPrefix(c.getModifiers(), Space.firstPrefix(c.getModifiers()).withWhitespace("")));
                } else if (c.getPadding().getTypeParameters() != null) {
                    c = c.getPadding().withTypeParameters(c.getPadding().getTypeParameters().withBefore(c.getPadding().getTypeParameters().getBefore().withWhitespace("")));
                } else {
                    c = c.getAnnotations().withKind(c.getAnnotations().getKind().withPrefix(c.getAnnotations().getKind().getPrefix().withWhitespace("")));
                }
            } else {
                List<C.Annotation> newLeadingAnnotations = removeAnnotationOrEmpty(leadingAnnotations, annotationRemoved);
                if (!newLeadingAnnotations.isEmpty()) {
                    c = c.withLeadingAnnotations(newLeadingAnnotations);
                }
            }
        }
        return c;
    }

    @Override
    public C.MethodDeclaration visitMethodDeclaration(C.MethodDeclaration method, ExecutionContext ctx) {
        C.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
        C.Annotation annotationRemoved = getCursor().pollMessage("annotationRemoved");

        List<C.Annotation> leadingAnnotations = method.getLeadingAnnotations();
        if (annotationRemoved != null && !leadingAnnotations.isEmpty()) {
            if (leadingAnnotations.get(0) == annotationRemoved && leadingAnnotations.size() == 1) {
                if (!m.getModifiers().isEmpty()) {
                    m = m.withModifiers(Space.formatFirstPrefix(m.getModifiers(), Space.firstPrefix(m.getModifiers()).withWhitespace("")));
                } else if (m.getPadding().getTypeParameters() != null) {
                    m = m.getPadding().withTypeParameters(m.getPadding().getTypeParameters().withPrefix(m.getPadding().getTypeParameters().getPrefix().withWhitespace("")));
                } else if (m.getReturnTypeExpression() != null) {
                    m = m.withReturnTypeExpression(m.getReturnTypeExpression().withPrefix(m.getReturnTypeExpression().getPrefix().withWhitespace("")));
                } else {
                    m = m.withName(m.getName().withPrefix(m.getName().getPrefix().withWhitespace("")));
                }
            } else {
                List<C.Annotation> newLeadingAnnotations = removeAnnotationOrEmpty(leadingAnnotations, annotationRemoved);
                if (!newLeadingAnnotations.isEmpty()) {
                    m = m.withLeadingAnnotations(newLeadingAnnotations);
                }
            }
        }
        return m;
    }

    @Override
    public C.VariableDeclarations visitVariableDeclarations(C.VariableDeclarations multiVariable, ExecutionContext ctx) {
        C.VariableDeclarations v = super.visitVariableDeclarations(multiVariable, ctx);
        C.Annotation annotationRemoved = getCursor().pollMessage("annotationRemoved");

        List<C.Annotation> leadingAnnotations = multiVariable.getLeadingAnnotations();
        if (annotationRemoved != null && !leadingAnnotations.isEmpty()) {
            if (leadingAnnotations.get(0) == annotationRemoved && leadingAnnotations.size() == 1) {
                if (!v.getModifiers().isEmpty()) {
                    v = v.withModifiers(Space.formatFirstPrefix(v.getModifiers(), Space.firstPrefix(v.getModifiers()).withWhitespace("")));
                } else if (v.getTypeExpression() != null) {
                    v = v.withTypeExpression(v.getTypeExpression().withPrefix(v.getTypeExpression().getPrefix().withWhitespace("")));
                }
            } else {
                List<C.Annotation> newLeadingAnnotations = removeAnnotationOrEmpty(leadingAnnotations, annotationRemoved);
                if (!newLeadingAnnotations.isEmpty()) {
                    v = v.withLeadingAnnotations(newLeadingAnnotations);
                }
            }
        }

        return v;
    }

    @Override
    public C.Annotation visitAnnotation(C.Annotation annotation, ExecutionContext ctx) {
        if (annotationMatcher.matches(annotation)) {
            getCursor().getParentOrThrow().putMessage("annotationRemoved", annotation);
            maybeRemoveImport(TypeUtils.asFullyQualified(annotation.getType()));
            //noinspection ConstantConditions
            return null;
        }
        return super.visitAnnotation(annotation, ctx);
    }

    /* Returns a list of leading annotations with the target removed or an empty list if no changes are necessary.
     * A prefix only needs to change if the index == 0 and the prefixes of the target annotation and next annotation are not equal.
     */
    private List<C.Annotation> removeAnnotationOrEmpty(List<C.Annotation> leadingAnnotations, C.Annotation targetAnnotation) {
        int index = leadingAnnotations.indexOf(targetAnnotation);
        List<C.Annotation> newLeadingAnnotations = new ArrayList<>();
        if (index == 0) {
            C.Annotation nextAnnotation = leadingAnnotations.get(1);
            if (!nextAnnotation.getPrefix().equals(targetAnnotation.getPrefix())) {
                newLeadingAnnotations.add(nextAnnotation.withPrefix(targetAnnotation.getPrefix()));
                for (int i = 2; i < leadingAnnotations.size(); ++i) {
                    newLeadingAnnotations.add(leadingAnnotations.get(i));
                }
            }
        }
        return newLeadingAnnotations;
    }
}
