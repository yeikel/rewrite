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
package org.openrewrite.groovy;

import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.TreePrinter;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaPrinter;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;

public class GroovyPrinter<P> extends GroovyVisitor<P> {
    private final TreePrinter<P> treePrinter;
    private final GroovyJavaPrinter delegate;

    public GroovyPrinter(TreePrinter<P> treePrinter) {
        this.treePrinter = treePrinter;
        this.delegate = new GroovyJavaPrinter(treePrinter);
    }

    public String print(J j, P p) {
        setCursor(new Cursor(null, "EPSILON"));
        visit(j, p);
        return delegate.getPrinter().toString();
    }

    @Override
    public G visitCompilationUnit(JavaSourceFile cu, P p) {
        return super.visitCompilationUnit(cu, p);
    }

    @Override
    @Nullable
    public J visit(@Nullable Tree tree, P p) {
        if (tree == null) {
            return defaultValue(null, p);
        }

        if(!(tree instanceof G)) {
            return delegate.visit(tree, p);
        }

        StringBuilder printerAcc = delegate.getPrinter();
        treePrinter.doBefore(tree, printerAcc, p);
        tree = super.visit(tree, p);
        if (tree != null) {
            treePrinter.doAfter(tree, printerAcc, p);
        }
        return (J) tree;
    }

    private class GroovyJavaPrinter extends JavaPrinter<P> {
        public GroovyJavaPrinter(TreePrinter<P> treePrinter) {
            super(treePrinter);
        }

        @Override
        public StringBuilder getPrinter() {
            return super.getPrinter();
        }
    }
}
