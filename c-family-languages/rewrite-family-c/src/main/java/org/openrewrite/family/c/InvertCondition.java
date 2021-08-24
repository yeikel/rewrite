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

import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.family.c.cleanup.SimplifyBooleanExpressionVisitor;
import org.openrewrite.family.c.tree.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;

import static org.openrewrite.Tree.randomId;

public class InvertCondition<P> extends CVisitor<P> {

    @SuppressWarnings("unchecked")
    public static <C2 extends C> C.ControlParentheses<C2> invert(C.ControlParentheses<C2> controlParentheses, Cursor cursor) {
        //noinspection ConstantConditions
        return (C.ControlParentheses<C2>) new InvertCondition<Integer>()
                .visit(controlParentheses, 0, cursor.getParentOrThrow());
    }

    @Nullable
    @Override
    public C visit(@Nullable Tree tree, P p) {
        C t;
        if (tree instanceof Expression && !(tree instanceof C.ControlParentheses) && !(tree instanceof C.Binary)) {
            Expression expression = (Expression) tree;
            t = new C.Unary(randomId(), expression.getPrefix(), Markers.EMPTY,
                    CLeftPadded.build(C.Unary.Type.Not), expression.withPrefix(Space.EMPTY), expression.getType());
        } else {
            t = super.visit(tree, p);
        }

        return new SimplifyBooleanExpressionVisitor<>().visit(t, p, getCursor().getParentOrThrow());
    }

    @Override
    public C visitBinary(C.Binary binary, P p) {
        switch (binary.getOperator()) {
            case LessThan:
                return binary.withOperator(C.Binary.Type.GreaterThanOrEqual);
            case GreaterThan:
                return binary.withOperator(C.Binary.Type.LessThanOrEqual);
            case LessThanOrEqual:
                return binary.withOperator(C.Binary.Type.GreaterThan);
            case GreaterThanOrEqual:
                return binary.withOperator(C.Binary.Type.LessThan);
            case Equal:
                return binary.withOperator(C.Binary.Type.NotEqual);
            case NotEqual:
                return binary.withOperator(C.Binary.Type.Equal);
        }

        return new C.Unary(
                randomId(),
                binary.getPrefix(),
                Markers.EMPTY,
                CLeftPadded.build(C.Unary.Type.Not),
                new C.Parentheses<>(
                        randomId(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        CRightPadded.build(binary.withPrefix(Space.EMPTY))
                ),
                binary.getType());
    }
}
