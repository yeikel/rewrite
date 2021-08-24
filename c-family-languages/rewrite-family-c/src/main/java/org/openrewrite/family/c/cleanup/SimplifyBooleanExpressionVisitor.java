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
package org.openrewrite.family.c.cleanup;

import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.family.c.CIsoVisitor;
import org.openrewrite.family.c.CVisitor;
import org.openrewrite.family.c.tree.C;
import org.openrewrite.family.c.tree.Expression;
import org.openrewrite.family.c.tree.Space;
import org.openrewrite.internal.lang.Nullable;

@Incubating(since = "7.0.0")
public class SimplifyBooleanExpressionVisitor<P> extends CVisitor<P> {
    private static final String MAYBE_AUTO_FORMAT_ME = "MAYBE_AUTO_FORMAT_ME";

    @Override
    public C visitCompilationUnit(C.CompilationUnit cu, P p) {
        C.CompilationUnit c = visitAndCast(cu, p, super::visitCompilationUnit);
        if (c != cu) {
            doAfterVisit(new SimplifyBooleanExpressionVisitor<>());
        }
        return c;
    }

    @Override
    public C visitBinary(C.Binary binary, P p) {
        C j = super.visitBinary(binary, p);
        C.Binary asBinary = (C.Binary) j;

        if (asBinary.getOperator() == C.Binary.Type.And) {
            if (isLiteralFalse(asBinary.getLeft())) {
                maybeUnwrapParentheses();
                j = asBinary.getLeft();
            } else if (isLiteralFalse(asBinary.getRight())) {
                maybeUnwrapParentheses();
                j = asBinary.getRight();
            } else if (removeAllSpace(asBinary.getLeft()).printTrimmed()
                    .equals(removeAllSpace(asBinary.getRight()).printTrimmed())) {
                maybeUnwrapParentheses();
                j = asBinary.getLeft();
            }
        } else if (asBinary.getOperator() == C.Binary.Type.Or) {
            if (isLiteralTrue(asBinary.getLeft())) {
                maybeUnwrapParentheses();
                j = asBinary.getLeft();
            } else if (isLiteralTrue(asBinary.getRight())) {
                maybeUnwrapParentheses();
                j = asBinary.getRight();
            } else if (removeAllSpace(asBinary.getLeft()).printTrimmed()
                    .equals(removeAllSpace(asBinary.getRight()).printTrimmed())) {
                maybeUnwrapParentheses();
                j = asBinary.getLeft();
            }
        } else if (asBinary.getOperator() == C.Binary.Type.Equal) {
            if (isLiteralTrue(asBinary.getLeft())) {
                maybeUnwrapParentheses();
                j = asBinary.getRight();
            } else if (isLiteralTrue(asBinary.getRight())) {
                maybeUnwrapParentheses();
                j = asBinary.getLeft();
            }
        } else if (asBinary.getOperator() == C.Binary.Type.NotEqual) {
            if (isLiteralFalse(asBinary.getLeft())) {
                maybeUnwrapParentheses();
                j = asBinary.getRight();
            } else if (isLiteralFalse(asBinary.getRight())) {
                maybeUnwrapParentheses();
                j = asBinary.getLeft();
            }
        }
        if (asBinary != j) {
            getCursor().dropParentUntil(C.class::isInstance).putMessage(MAYBE_AUTO_FORMAT_ME, "");
        }
        return j;
    }

    @Nullable
    @Override
    public J postVisit(J tree, P p) {
        J j = super.postVisit(tree, p);
        if (getCursor().pollMessage(MAYBE_AUTO_FORMAT_ME) != null) {
            j = new AutoFormatVisitor<>().visit(j, p, getCursor());
        }
        return j;
    }

    @Override
    public J visitUnary(C.Unary unary, P p) {
        J j = super.visitUnary(unary, p);
        C.Unary asUnary = (C.Unary) j;

        if (asUnary.getOperator() == C.Unary.Type.Not) {
            if (isLiteralTrue(asUnary.getExpression())) {
                maybeUnwrapParentheses();
                j = ((C.Literal) asUnary.getExpression()).withValue(false).withValueSource("false");
            } else if (isLiteralFalse(asUnary.getExpression())) {
                maybeUnwrapParentheses();
                j = ((C.Literal) asUnary.getExpression()).withValue(true).withValueSource("true");
            } else if (asUnary.getExpression() instanceof C.Unary && ((C.Unary) asUnary.getExpression()).getOperator() == C.Unary.Type.Not) {
                maybeUnwrapParentheses();
                j = ((C.Unary) asUnary.getExpression()).getExpression();
            }
        }
        if (asUnary != j) {
            getCursor().dropParentUntil(C.class::isInstance).putMessage(MAYBE_AUTO_FORMAT_ME, "");
        }
        return j;
    }

    /**
     * Specifically for removing immediately-enclosing parentheses on Identifiers and Literals.
     * This queues a potential unwrap operation for the next visit. After unwrapping something, it's possible
     * there are more Simplifications this recipe can identify and perform, which is why visitCompilationUnit
     * checks for any changes to the entire Compilation Unit, and if so, queues up another SimplifyBooleanExpression
     * recipe call. This convergence loop eventually reconciles any remaining Boolean Expression Simplifications
     * the recipe can perform.
     */
    private void maybeUnwrapParentheses() {
        Cursor c = getCursor().getParentOrThrow().dropParentUntil(C.class::isInstance);
        if (c.getValue() instanceof C.Parentheses) {
            doAfterVisit(new UnwrapParentheses<>(c.getValue()));
        }
    }

    private boolean isLiteralTrue(@Nullable Expression expression) {
        return expression instanceof C.Literal && ((C.Literal) expression).getValue() == Boolean.valueOf(true);
    }

    private boolean isLiteralFalse(@Nullable Expression expression) {
        return expression instanceof C.Literal && ((C.Literal) expression).getValue() == Boolean.valueOf(false);
    }

    private C removeAllSpace(C t) {
        return new CIsoVisitor<Integer>() {
            @Override
            public Space visitSpace(Space space, Space.Location loc, Integer integer) {
                return Space.EMPTY;
            }
        }.visitNonNull(t, 0);
    }
}
