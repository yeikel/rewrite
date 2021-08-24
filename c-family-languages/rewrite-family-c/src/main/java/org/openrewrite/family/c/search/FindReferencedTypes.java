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

import org.openrewrite.family.c.CIsoVisitor;
import org.openrewrite.family.c.tree.C;
import org.openrewrite.family.c.tree.CType;
import org.openrewrite.family.c.tree.NameTree;
import org.openrewrite.family.c.tree.TypeUtils;

import java.util.HashSet;
import java.util.Set;

public class FindReferencedTypes {
    private FindReferencedTypes() {
    }

    public static Set<CType.FullyQualified> find(C c) {
        Set<CType.FullyQualified> fields = new HashSet<>();
        new FindReferencedTypesVisitor().visit(c, fields);
        return fields;
    }

    private static class FindReferencedTypesVisitor extends CIsoVisitor<Set<CType.FullyQualified>> {
        @Override
        public <N extends NameTree> N visitTypeName(N name, Set<CType.FullyQualified> ctx) {
            CType.FullyQualified fullyQualified = TypeUtils.asFullyQualified(name.getType());
            if (fullyQualified != null) {
                ctx.add(fullyQualified);
            }
            return super.visitTypeName(name, ctx);
        }
    }
}
