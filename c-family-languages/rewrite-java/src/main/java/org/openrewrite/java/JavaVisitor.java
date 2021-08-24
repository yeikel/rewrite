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
package org.openrewrite.java;

import org.openrewrite.family.c.tree.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.J;

public class JavaVisitor<P> extends JvmVisitor<P> {

    @Override
    public C visitSourceFile(CSourceFile sourceFile, P p) {
        return visitCompilationUnit((J.CompilationUnit) sourceFile, p);
    }

    public J visitCompilationUnit(J.CompilationUnit cu, P p) {
        J.CompilationUnit c = cu;
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.COMPILATION_UNIT_PREFIX, p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        if (c.getPadding().getPackageDeclaration() != null) {
            c = c.getPadding().withPackageDeclaration(visitRightPadded(c.getPadding().getPackageDeclaration(), CRightPadded.Location.PACKAGE, p));
        }
        c = c.getPadding().withImports(ListUtils.map(c.getPadding().getImports(), t -> visitRightPadded(t, CRightPadded.Location.IMPORT, p)));
        c = c.withClasses(ListUtils.map(c.getClasses(), e -> visitAndCast(e, p)));
        c = c.withEof(visitSpace(c.getEof(), Space.Location.COMPILATION_UNIT_EOF, p));
        return c;
    }

    public J visitImport(J.Import impoort, P p) {
        J.Import i = impoort;
        i = i.withPrefix(visitSpace(i.getPrefix(), Space.Location.IMPORT_PREFIX, p));
        i = i.withMarkers(visitMarkers(i.getMarkers(), p));
        i = i.getPadding().withStatic(visitLeftPadded(i.getPadding().getStatic(), CLeftPadded.Location.STATIC_IMPORT, p));
        i = i.withQualid(visitAndCast(i.getQualid(), p));
        return i;
    }

    public J visitPackage(J.Package pkg, P p) {
        J.Package pa = pkg;
        pa = pa.withPrefix(visitSpace(pa.getPrefix(), Space.Location.PACKAGE_PREFIX, p));
        pa = pa.withMarkers(visitMarkers(pa.getMarkers(), p));
        pa = pa.withExpression(visitAndCast(pa.getExpression(), p));
        pa = pa.withAnnotations(ListUtils.map(pa.getAnnotations(), a -> visitAndCast(a, p)));
        return pa;
    }
}
