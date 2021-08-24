package org.openrewrite;

import org.openrewrite.internal.lang.Nullable;

import static org.openrewrite.Tree.randomId;

public class RandomizeIdVisitor<T extends Tree, P> extends TreeVisitor<T, P> {
    @Nullable
    @Override
    public T postVisit(T tree, P p) {
        return tree.withId(randomId());
    }
}
