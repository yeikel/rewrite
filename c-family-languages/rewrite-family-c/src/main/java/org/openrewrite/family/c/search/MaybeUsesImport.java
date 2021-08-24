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
import org.openrewrite.family.c.CIsoVisitor;
import org.openrewrite.family.c.marker.JavaSearchResult;
import org.openrewrite.family.c.tree.Expression;
import org.openrewrite.family.c.tree.C;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import static org.openrewrite.Tree.randomId;

/**
 * Acts as a sort of bloom filter for the presence of an import for a particular type in a {@link C.CompilationUnit},
 * i.e. it may falsely report the presence of an import, but would never negatively report when the type is in present.
 */
@Incubating(since = "7.4.0")
public class MaybeUsesImport<P> extends CIsoVisitor<P> {
    @SuppressWarnings("ConstantConditions")
    private static final Markers FOUND_TYPE = Markers.build(Collections.singletonList(
            new JavaSearchResult(randomId(), null, null)));

    private final List<String> fullyQualifiedTypeSegments;

    public MaybeUsesImport(String fullyQualifiedType) {
        Scanner scanner = new Scanner(fullyQualifiedType);
        scanner.useDelimiter("\\.");
        this.fullyQualifiedTypeSegments = new ArrayList<>();
        while (scanner.hasNext()) {
            fullyQualifiedTypeSegments.add(scanner.next());
        }
    }

    @Override
    public C.Import visitImport(C.Import _import, P p) {
        C.Import i = super.visitImport(_import, p);
        if (matchesType(i)) {
            i = i.withMarkers(FOUND_TYPE);
        }
        return i;
    }

    private boolean matchesType(C.Import i) {
        Expression prior = null;
        for (String segment : fullyQualifiedTypeSegments) {
            for (Expression expr = i.getQualid(); expr != prior; ) {
                if (expr instanceof C.Identifier) {
                    // this can only be the first segment
                    prior = expr;
                    if (!((C.Identifier) expr).getSimpleName().equals(segment)) {
                        return false;
                    }
                } else {
                    C.FieldAccess fa = (C.FieldAccess) expr;
                    if (fa.getTarget() == prior) {
                        String simpleName = fa.getSimpleName();
                        if (!segment.equals("*") && !simpleName.equals(segment) && !simpleName.equals("*")) {
                            return false;
                        }
                        prior = fa;
                        continue;
                    }
                    expr = fa.getTarget();
                }
            }
        }

        for (Expression expr = i.getQualid(); expr != prior; ) {
            if (!(expr instanceof C.FieldAccess)) {
                return false; // don't think this can ever happen
            }

            C.FieldAccess fa = (C.FieldAccess) expr;
            if (Character.isLowerCase(fa.getSimpleName().charAt(0))) {
                return fa == i.getQualid() && i.isStatic();
            }
            expr = fa.getTarget();
        }

        return true;
    }

    @Override
    public C.Annotation visitAnnotation(C.Annotation annotation, P p) {
        return annotation;
    }

    @Override
    public C.ClassDeclaration visitClassDeclaration(C.ClassDeclaration classDecl, P p) {
        return classDecl;
    }
}
