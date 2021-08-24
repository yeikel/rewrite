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
package org.openrewrite.family.c.search;

import org.openrewrite.Incubating;
import org.openrewrite.family.c.CVisitor;
import org.openrewrite.family.c.tree.Expression;
import org.openrewrite.family.c.tree.C;
import org.openrewrite.family.c.tree.CType;
import org.openrewrite.family.c.tree.NameTree;

import java.util.List;
import java.util.Objects;

/*
 * Recursively checks the equality of each element of two ASTs to determine if two trees are semantically equal.
 */
@Incubating(since = "6.0.0")
public class SemanticallyEqual {

    private SemanticallyEqual() {
    }

    public static boolean areEqual(C firstElem, C secondElem) {
        SemanticallyEqualVisitor sep = new SemanticallyEqualVisitor();
        sep.visit(firstElem, secondElem); // returns null, but changes value of class variable isEqual
        return sep.isEqual;
    }

    /**
     * Note: The following visit methods extend JavaVisitor in order to inherit access to the
     * visitor pattern set up there; however, the necessity to return a J did not fit the purposes of
     * SemanticallyEqualVisitor, so while the equality is tracked in isEqual, all the visitors return null.
     */
    @SuppressWarnings("ConstantConditions")
    private static class SemanticallyEqualVisitor extends CVisitor<C> {
        boolean isEqual;

        SemanticallyEqualVisitor() {
            isEqual = true;
        }

        @Override
        public C visitAnnotation(C.Annotation firstAnnotation, C second) {
            if (!(second instanceof C.Annotation)) {
                isEqual = false;
                return null;
            }
            C.Annotation secondAnnotation = (C.Annotation) second;

            if (firstAnnotation.getArguments() != null && secondAnnotation.getArguments() != null) {
                if (firstAnnotation.getArguments().size() == secondAnnotation.getArguments().size()) {

                    List<Expression> firstArgs = firstAnnotation.getArguments();
                    List<Expression> secondArgs = secondAnnotation.getArguments();

                    for (int i = 0; i < firstArgs.size(); i++) {
                        this.visit(firstArgs.get(i), secondArgs.get(i));
                    }
                } else {
                    isEqual = false;
                    return null;
                }
            }
            this.visitTypeName(firstAnnotation.getAnnotationType(), secondAnnotation.getAnnotationType());
            return null;
        }

        @Override
        public C visitIdentifier(C.Identifier firstIdent, C second) {
            if (!(second instanceof C.Identifier)) {
                isEqual = false;
                return null;
            }
            C.Identifier secondIdent = (C.Identifier) second;

            isEqual = isEqual && typeEquals(firstIdent.getType(), secondIdent.getType()) &&
                    firstIdent.getSimpleName().equals(secondIdent.getSimpleName());

            return null;
        }

        @Override
        public C visitFieldAccess(C.FieldAccess firstFieldAccess, C second) {
            if (!(second instanceof C.FieldAccess)) {
                isEqual = false;
                return null;
            }
            C.FieldAccess secondFieldAccess = (C.FieldAccess) second;

            // Class literals are the only kind of FieldAccess which can appear within annotations
            // Functionality to correctly determine semantic equality of other kinds of field access will come later
            if (firstFieldAccess.getSimpleName().equals("class")) {
                if (!secondFieldAccess.getSimpleName().equals("class")) {
                    isEqual = false;
                    return null;
                } else {
                    isEqual = isEqual &&
                            typeEquals(firstFieldAccess.getType(), secondFieldAccess.getType()) &&
                            typeEquals(firstFieldAccess.getTarget().getType(), secondFieldAccess.getTarget().getType());
                }
            }

            return null;
        }

        @Override
        public C visitAssignment(C.Assignment firstAssignment, C second) {
            if (!(second instanceof C.Assignment)) {
                isEqual = false;
                return null;
            }
            C.Assignment secondAssignment = (C.Assignment) second;

            isEqual = isEqual &&
                    Objects.equals(firstAssignment.getType(), secondAssignment.getType()) &&
                    SemanticallyEqual.areEqual(firstAssignment.getVariable(), secondAssignment.getVariable()) &&
                    SemanticallyEqual.areEqual(firstAssignment.getAssignment(), secondAssignment.getAssignment());

            return null;
        }

        @Override
        public C visitLiteral(C.Literal firstLiteral, C second) {
            if (!(second instanceof C.Literal)) {
                isEqual = false;
                return null;
            }
            C.Literal secondLiteral = (C.Literal) second;

            isEqual = isEqual && Objects.equals(firstLiteral.getValue(), secondLiteral.getValue());

            return null;
        }

        @Override
        public <N extends NameTree> N visitTypeName(N firstTypeName, C second) {
            if (!(second instanceof NameTree)) {
                isEqual = false;
                return null;
            }
            isEqual = isEqual && typeEquals(firstTypeName.getType(), ((NameTree) second).getType());
            return null;
        }

        private static boolean typeEquals(CType thisType, CType otherType) {
            if (thisType == null) {
                return otherType == null;
            }
            if (thisType instanceof CType.FullyQualified) {
                if (!(otherType instanceof CType.FullyQualified)) {
                    return false;
                }
                return ((CType.FullyQualified) thisType).getFullyQualifiedName().equals(((CType.FullyQualified) otherType).getFullyQualifiedName());
            }

            return thisType.deepEquals(otherType);
        }
    }
}
