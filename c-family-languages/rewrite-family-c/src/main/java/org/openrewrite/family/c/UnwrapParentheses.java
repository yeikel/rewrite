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
import org.openrewrite.family.c.tree.C;

public class UnwrapParentheses<P> extends CVisitor<P> {
    private final C.Parentheses<?> scope;

    public UnwrapParentheses(C.Parentheses<?> scope) {
        this.scope = scope;
    }

    @Override
    public <T extends C> C visitParentheses(C.Parentheses<T> parens, P p) {
        return scope.isScope(parens) && isUnwrappable(getCursor()) ?
                parens.getTree().withPrefix(parens.getPrefix()) :
                super.visitParentheses(parens, p);
    }

    public static boolean isUnwrappable(Cursor parensScope) {
        if (!(parensScope.getValue() instanceof C.Parentheses)) {
            return false;
        }
        C parent = parensScope.dropParentUntil(C.class::isInstance).getValue();
        if (parent instanceof C.If ||
                parent instanceof C.Switch ||
                parent instanceof C.Synchronized ||
                parent instanceof C.Try.Catch ||
                parent instanceof C.TypeCast ||
                parent instanceof C.WhileLoop) {
            return false;
        } else if (parent instanceof C.DoWhileLoop) {
            return !(parensScope.getValue() == ((C.DoWhileLoop) parent).getWhileCondition());
        }
        return true;
    }
}

