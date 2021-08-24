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

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.family.c.CIsoVisitor;
import org.openrewrite.family.c.marker.JavaSearchResult;
import org.openrewrite.family.c.tree.Flag;
import org.openrewrite.family.c.tree.C;
import org.openrewrite.family.c.tree.CType;
import org.openrewrite.family.c.tree.TypeUtils;
import org.openrewrite.marker.Marker;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import static org.openrewrite.Tree.randomId;

public class UsesType<P> extends CIsoVisitor<P> {
    @SuppressWarnings("ConstantConditions")
    private static final Marker FOUND_TYPE = new JavaSearchResult(randomId(), null, null);

    private final CType.FullyQualified fullyQualifiedType;
    private final List<String> fullyQualifiedTypeSegments;

    public UsesType(String fullyQualifiedType) {
        this.fullyQualifiedType = CType.Class.build(fullyQualifiedType);

        Scanner scanner = new Scanner(fullyQualifiedType);
        scanner.useDelimiter("\\.");
        this.fullyQualifiedTypeSegments = new ArrayList<>();
        while (scanner.hasNext()) {
            fullyQualifiedTypeSegments.add(scanner.next());
        }
    }

    @Override
    public C.CompilationUnit visitCompilationUnit(C.CompilationUnit cu, P p) {
        C.CompilationUnit c = cu;
        Set<CType> types = c.getTypesInUse();
        for (CType type : types) {
            if (type instanceof CType.FullyQualified) {
                CType.FullyQualified fq = (CType.FullyQualified) type;
                if ((c = maybeMark(c, fq)) != cu) {
                    return c;
                }
            } else if (type instanceof CType.Method) {
                CType.Method method = (CType.Method) type;
                if (method.hasFlags(Flag.Static)) {
                    if ((c = maybeMark(c, method.getDeclaringType())) != cu) {
                        return c;
                    }
                }
            }
        }

        for (C.Import anImport : c.getImports()) {
            if (anImport.isStatic()) {
                if ((c = maybeMark(c, TypeUtils.asFullyQualified(anImport.getQualid().getTarget().getType()))) != cu) {
                    return c;
                }
            } else if ((c = maybeMark(c, TypeUtils.asFullyQualified(anImport.getQualid().getType()))) != cu) {
                return c;
            }
        }

        return c;
    }

    private C.CompilationUnit maybeMark(C.CompilationUnit c, @Nullable CType.FullyQualified fq) {
        if (fq == null) {
            return c;
        }

        if(fullyQualifiedType.isAssignableFrom(fq)) {
            return c.withMarkers(c.getMarkers().addIfAbsent(FOUND_TYPE));
        }

        Scanner scanner = new Scanner(fq.getFullyQualifiedName());
        scanner.useDelimiter("\\.");
        int i = 0;
        for (; scanner.hasNext() && i < fullyQualifiedTypeSegments.size(); i++) {
            String segment = fullyQualifiedTypeSegments.get(i);
            if (segment.equals("*")) {
                break;
            }
            String test = scanner.next();
            if (!segment.equals(test)) {
                return c;
            }
        }

        return c.withMarkers(c.getMarkers().addIfAbsent(FOUND_TYPE));
    }
}
