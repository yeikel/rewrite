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
package org.openrewrite.family.c.search;

import lombok.RequiredArgsConstructor;
import org.openrewrite.Incubating;
import org.openrewrite.family.c.CIsoVisitor;
import org.openrewrite.family.c.marker.JavaSearchResult;
import org.openrewrite.family.c.tree.C;
import org.openrewrite.family.c.tree.CType;
import org.openrewrite.family.c.tree.TypeUtils;
import org.openrewrite.marker.Marker;

import java.util.Set;

import static org.openrewrite.Tree.randomId;

@RequiredArgsConstructor
@Incubating(since = "7.7.0")
public class UsesField<P> extends CIsoVisitor<P> {
    private static final Marker FOUND_TYPE = new JavaSearchResult(randomId(), null, null);

    private final String fullyQualifiedType;
    private final String field;

    @Override
    public C.CompilationUnit visitCompilationUnit(C.CompilationUnit cu, P p) {
        Set<CType> types = cu.getTypesInUse();
        for (CType type : types) {
            if (type instanceof CType.Variable) {
                CType.Variable variable = (CType.Variable) type;
                if (variable.getName().equals(field) && TypeUtils.isOfClassType(variable.getType(), fullyQualifiedType)) {
                    return cu.withMarkers(cu.getMarkers().addIfAbsent(FOUND_TYPE));
                }
            }
        }
        return cu;
    }
}
