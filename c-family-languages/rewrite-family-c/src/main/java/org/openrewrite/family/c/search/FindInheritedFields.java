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
import org.openrewrite.family.c.tree.Flag;
import org.openrewrite.family.c.tree.C;
import org.openrewrite.family.c.tree.CType;
import org.openrewrite.family.c.tree.TypeUtils;

import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.emptySet;

public class FindInheritedFields {
    private FindInheritedFields() {
    }

    public static Set<CType.Variable> find(C c, String clazz) {
        Set<CType.Variable> fields = new HashSet<>();
        new FindInheritedFieldsVisitor(clazz).visit(c, fields);
        return fields;
    }

    private static class FindInheritedFieldsVisitor extends CIsoVisitor<Set<CType.Variable>> {
        private final String fullyQualifiedName;

        public FindInheritedFieldsVisitor(String fullyQualifiedName) {
            this.fullyQualifiedName = fullyQualifiedName;
        }

        private Set<CType.Variable> superFields(@Nullable CType.FullyQualified type) {

            if (type == null || type.getSupertype() == null) {
                return emptySet();
            }
            Set<CType.Variable> types = new HashSet<>();
            type.getMembers().stream()
                    .filter(m -> !m.hasFlags(Flag.Private) && TypeUtils.hasElementTypeAssignable(m.getType(), fullyQualifiedName))
                    .forEach(types::add);
            types.addAll(superFields(type.getSupertype()));
            return types;
        }

        @Override
        public C.ClassDeclaration visitClassDeclaration(C.ClassDeclaration classDecl, Set<CType.Variable> ctx) {
            ctx.addAll(superFields(classDecl.getType() == null ? null : classDecl.getType().getSupertype()));
            return super.visitClassDeclaration(classDecl, ctx);
        }
    }
}
