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
package org.openrewrite.family.c;

import org.openrewrite.Cursor;
import org.openrewrite.family.c.tree.*;
import org.openrewrite.internal.lang.Nullable;

import java.util.concurrent.atomic.AtomicReference;

public class ChangeFieldName<P> extends CIsoVisitor<P> {
    private final CType.Class classType;
    private final String hasName;
    private final String toName;

    public ChangeFieldName(CType.Class classType, String hasName, String toName) {
        this.classType = classType;
        this.hasName = hasName;
        this.toName = toName;
    }

    @Override
    public C.VariableDeclarations.NamedVariable visitVariable(C.VariableDeclarations.NamedVariable variable, P p) {
        C.VariableDeclarations.NamedVariable v = variable;
        C.ClassDeclaration enclosingClass = getCursor().firstEnclosingOrThrow(C.ClassDeclaration.class);
        if (variable.isField(getCursor()) && matchesClass(enclosingClass.getType()) &&
                variable.getSimpleName().equals(hasName)) {
            v = v.withName(v.getName().withName(toName));
        }
        if (variable.getPadding().getInitializer() != null) {
            v = v.getPadding().withInitializer(visitLeftPadded(variable.getPadding().getInitializer(),
                    CLeftPadded.Location.VARIABLE_INITIALIZER, p));
        }
        return v;
    }

    @Override
    public C.FieldAccess visitFieldAccess(C.FieldAccess fieldAccess, P p) {
        C.FieldAccess f = super.visitFieldAccess(fieldAccess, p);
        if (matchesClass(fieldAccess.getTarget().getType()) &&
                fieldAccess.getSimpleName().equals(hasName)) {
            f = f.getPadding().withName(f.getPadding().getName().withElement(f.getPadding().getName().getElement().withName(toName)));
        }
        return f;
    }

    @Override
    public C.Identifier visitIdentifier(C.Identifier ident, P p) {
        C.Identifier i = super.visitIdentifier(ident, p);
        if (ident.getSimpleName().equals(hasName) && isFieldReference(ident) &&
                isThisReferenceToClassType()) {
            i = i.withName(toName);
        }
        return i;
    }

    private boolean matchesClass(@Nullable CType test) {
        CType.FullyQualified testClassType = TypeUtils.asFullyQualified(test);
        return testClassType != null && testClassType.getFullyQualifiedName().equals(classType.getFullyQualifiedName());
    }

    private boolean isThisReferenceToClassType() {
        C.FieldAccess fieldAccess = getCursor().firstEnclosing(C.FieldAccess.class);
        if (fieldAccess == null) {
            return true;
        }
        while(fieldAccess.getType() == null && fieldAccess.getTarget() instanceof C.FieldAccess) {
            fieldAccess = (C.FieldAccess) fieldAccess.getTarget();
        }
        return classType.equals(fieldAccess.getTarget().getType());
    }

    private boolean isFieldReference(C.Identifier ident) {
        AtomicReference<Cursor> nearest = new AtomicReference<>();
        new FindVariableDefinition(ident, getCursor()).visit(getCursor().firstEnclosing(CSourceFile.class), nearest);
        return nearest.get() != null && nearest.get()
                .dropParentUntil(C.class::isInstance) // maybe J.VariableDecls
                .dropParentUntil(C.class::isInstance) // maybe J.Block
                .dropParentUntil(C.class::isInstance) // maybe J.ClassDecl
                .getValue() instanceof C.ClassDeclaration;
    }

    private static class FindVariableDefinition extends CIsoVisitor<AtomicReference<Cursor>> {
        private final C.Identifier ident;
        private final Cursor referenceScope;

        public FindVariableDefinition(C.Identifier ident, Cursor referenceScope) {
            this.ident = ident;
            this.referenceScope = referenceScope;
        }

        @Override
        public C.VariableDeclarations.NamedVariable visitVariable(C.VariableDeclarations.NamedVariable variable, AtomicReference<Cursor> ctx) {
            if (variable.getSimpleName().equalsIgnoreCase(ident.getSimpleName()) && isInSameNameScope(referenceScope)) {
                // the definition will be the "closest" cursor, i.e. the one with the longest path in the same name scope
                ctx.accumulateAndGet(getCursor(), (r1, r2) -> {
                    if (r1 == null) {
                        return r2;
                    }
                    return r1.getPathAsStream().count() > r2.getPathAsStream().count() ? r1 : r2;
                });
            }
            return super.visitVariable(variable, ctx);
        }
    }
}
