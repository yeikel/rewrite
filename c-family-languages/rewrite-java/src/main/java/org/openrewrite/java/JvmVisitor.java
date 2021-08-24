package org.openrewrite.java;

import org.openrewrite.family.c.CVisitor;
import org.openrewrite.family.c.tree.Comment;
import org.openrewrite.family.c.tree.Space;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.Javadoc;

public class JvmVisitor<P> extends CVisitor<P> {
    protected JavadocVisitor<P> javadocVisitor = new JavadocVisitor<>(this);

    public final void maybeAddImport(String fullyQualifiedName, String statik) {
        AddImport<P> op = new AddImport<>(fullyQualifiedName, statik, true);
        if (!getAfterVisit().contains(op)) {
            doAfterVisit(op);
        }
    }

    @SuppressWarnings("unused")
    public Space visitSpace(Space space, Space.Location loc, P p) {
        Space s = space;
        s = s.withComments(ListUtils.map(s.getComments(), comment -> {
            if(comment instanceof Javadoc) {
                return (Comment) javadocVisitor.visit((Javadoc) comment, p);
            }
            return comment;
        }));
        return s;
    }
}
