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

import org.openrewrite.family.c.tree.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.marker.Markers;

import static org.openrewrite.Tree.randomId;
import static org.openrewrite.family.c.tree.Space.format;

public class ImplementInterface<P> extends CIsoVisitor<P> {
    private final C.ClassDeclaration scope;
    private final CType.FullyQualified interfaceType;

    public ImplementInterface(C.ClassDeclaration scope, CType.FullyQualified interfaceType) {
        this.scope = scope;
        this.interfaceType = interfaceType;
    }

    public ImplementInterface(C.ClassDeclaration scope, String interfaze) {
        this(scope, CType.Class.build(interfaze));
    }

    @Override
    public C.ClassDeclaration visitClassDeclaration(C.ClassDeclaration classDecl, P p) {
        C.ClassDeclaration c = super.visitClassDeclaration(classDecl, p);
        if (c.isScope(scope) && (c.getImplements() == null || c.getImplements().stream()
                .noneMatch(f -> interfaceType.equals(f.getType())))) {
            maybeAddImport(interfaceType);

            c = c.withImplements(ListUtils.concat(c.getImplements(), C.Identifier.build(
                    randomId(),
                    format(" "),
                    Markers.EMPTY,
                    interfaceType.getClassName(),
                    interfaceType
            )));

            CContainer<TypeTree> anImplements = c.getPadding().getImplements();
            assert anImplements != null;
            if (anImplements.getBefore().getWhitespace().isEmpty()) {
                c = c.getPadding().withImplements(anImplements.withBefore(Space.format(" ")));
            }
        }

        return c;
    }
}
