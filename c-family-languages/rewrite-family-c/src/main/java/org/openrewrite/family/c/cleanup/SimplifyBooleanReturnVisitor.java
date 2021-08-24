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
import org.openrewrite.family.c.CVisitor;
import org.openrewrite.family.c.DeleteStatement;
import org.openrewrite.family.c.tree.C;
import org.openrewrite.family.c.tree.Expression;
import org.openrewrite.family.c.tree.Statement;
import org.openrewrite.internal.lang.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class SimplifyBooleanReturnVisitor<P> extends CVisitor<P> {
    private final JavaTemplate notIfConditionReturn = JavaTemplate
            .builder(this::getCursor, "return !(#{any(boolean)});")
            .build();

    @Override
    public C visitIf(C.If iff, P p) {
        C.If i = visitAndCast(iff, p, super::visitIf);

        Cursor parent = getCursor().dropParentUntil(C.class::isInstance);

        if (parent.getValue() instanceof C.Block &&
                parent.getParentOrThrow().getValue() instanceof C.MethodDeclaration &&
                thenHasOnlyReturnStatement(iff) &&
                elseWithOnlyReturn(i)) {
            List<Statement> followingStatements = followingStatements();
            Optional<Expression> singleFollowingStatement = Optional.ofNullable(followingStatements.isEmpty() ? null : followingStatements.get(0))
                    .flatMap(stat -> Optional.ofNullable(stat instanceof C.Return ? (C.Return) stat : null))
                    .map(J.Return::getExpression);

            if (followingStatements.isEmpty() || singleFollowingStatement.map(r -> isLiteralFalse(r) || isLiteralTrue(r)).orElse(false)) {
                C.Return retrn = getReturnIfOnlyStatementInThen(iff).orElse(null);
                assert retrn != null;

                Expression ifCondition = i.getIfCondition().getTree();

                if (isLiteralTrue(retrn.getExpression())) {
                    if (singleFollowingStatement.map(this::isLiteralFalse).orElse(false) && i.getElsePart() == null) {
                        doAfterVisit(new DeleteStatement<>(followingStatements().get(0)));
                        return maybeAutoFormat(retrn, retrn.withExpression(ifCondition), p, parent);
                    } else if (!singleFollowingStatement.isPresent() &&
                            getReturnExprIfOnlyStatementInElseThen(i).map(this::isLiteralFalse).orElse(false)) {
                        if (i.getElsePart() != null) {
                            doAfterVisit(new DeleteStatement<>(i.getElsePart().getBody()));
                        }
                        return maybeAutoFormat(retrn, retrn.withExpression(ifCondition), p, parent);
                    }
                } else if (isLiteralFalse(retrn.getExpression())) {
                    boolean returnThenPart = false;

                    if (singleFollowingStatement.map(this::isLiteralTrue).orElse(false) && i.getElsePart() == null) {
                        doAfterVisit(new DeleteStatement<>(followingStatements().get(0)));
                        returnThenPart = true;
                    } else if (!singleFollowingStatement.isPresent() && getReturnExprIfOnlyStatementInElseThen(i)
                            .map(this::isLiteralTrue).orElse(false)) {
                        if (i.getElsePart() != null) {
                            doAfterVisit(new DeleteStatement<>(i.getElsePart().getBody()));
                        }
                        returnThenPart = true;
                    }

                    if (returnThenPart) {
                        // we need to NOT the expression inside the if condition
                        return i.withTemplate(notIfConditionReturn, i.getCoordinates().replace(), ifCondition);
                    }
                }
            }
        }

        return i;
    }

    private boolean elseWithOnlyReturn(C.If i) {
        return i.getElsePart() == null || !(i.getElsePart().getBody() instanceof C.If);
    }

    private boolean thenHasOnlyReturnStatement(C.If iff) {
        return getReturnIfOnlyStatementInThen(iff)
                .map(retrn -> isLiteralFalse(retrn.getExpression()) || isLiteralTrue(retrn.getExpression()))
                .orElse(false);
    }

    private List<Statement> followingStatements() {
        C.Block block = getCursor().dropParentUntil(J.class::isInstance).getValue();
        AtomicBoolean dropWhile = new AtomicBoolean(false);
        return block.getStatements().stream()
                .filter(s -> {
                    dropWhile.set(dropWhile.get() || s == getCursor().getValue());
                    return dropWhile.get();
                })
                .skip(1)
                .collect(Collectors.toList());
    }

    private boolean isLiteralTrue(@Nullable C tree) {
        return tree instanceof C.Literal && ((C.Literal) tree).getValue() == Boolean.valueOf(true);
    }

    private boolean isLiteralFalse(@Nullable C tree) {
        return tree instanceof C.Literal && ((C.Literal) tree).getValue() == Boolean.valueOf(false);
    }

    private Optional<C.Return> getReturnIfOnlyStatementInThen(C.If iff) {
        if (iff.getThenPart() instanceof C.Return) {
            return Optional.of((C.Return) iff.getThenPart());
        }
        if (iff.getThenPart() instanceof C.Block) {
            C.Block then = (C.Block) iff.getThenPart();
            if (then.getStatements().size() == 1 && then.getStatements().get(0) instanceof C.Return) {
                return Optional.of((C.Return) then.getStatements().get(0));
            }
        }
        return Optional.empty();
    }

    private Optional<Expression> getReturnExprIfOnlyStatementInElseThen(C.If iff2) {
        if (iff2.getElsePart() == null) {
            return Optional.empty();
        }

        Statement elze = iff2.getElsePart().getBody();
        if (elze instanceof C.Return) {
            return Optional.ofNullable(((C.Return) elze).getExpression());
        }

        if (elze instanceof C.Block) {
            List<Statement> statements = ((C.Block) elze).getStatements();
            if (statements.size() == 1) {
                C statement = statements.get(0);
                if (statement instanceof C.Return) {
                    return Optional.ofNullable(((C.Return) statement).getExpression());
                }
            }
        }

        return Optional.empty();
    }
}
