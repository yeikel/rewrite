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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.family.c.CIsoVisitor;
import org.openrewrite.family.c.marker.JavaSearchResult;
import org.openrewrite.family.c.tree.C;
import org.openrewrite.family.c.tree.TypeUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Finds fields that have a matching type.
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class FindFields extends Recipe {

    @Option(displayName = "Fully-qualified type name",
            description = "A fully-qualified Java type name, that is used to find matching fields.",
            example = "org.slf4j.api.Logger")
    String fullyQualifiedTypeName;

    @Override
    public String getDisplayName() {
        return "Find fields";
    }

    @Override
    public String getDescription() {
        return "Finds declared fields matching a particular class name.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new CIsoVisitor<ExecutionContext>() {
            @Override
            public C.VariableDeclarations visitVariableDeclarations(C.VariableDeclarations multiVariable, ExecutionContext ctx) {
                if (multiVariable.getTypeExpression() instanceof C.MultiCatch) {
                    return multiVariable;
                }
                if (multiVariable.getTypeExpression() != null && TypeUtils.hasElementType(multiVariable.getTypeExpression()
                        .getType(), fullyQualifiedTypeName)) {
                    return multiVariable.withMarkers(multiVariable.getMarkers().addIfAbsent(new JavaSearchResult(FindFields.this)));
                }
                return multiVariable;
            }
        };
    }

    public static Set<C.VariableDeclarations> find(C c, String fullyQualifiedTypeName) {
        CIsoVisitor<Set<C.VariableDeclarations>> findVisitor = new CIsoVisitor<Set<C.VariableDeclarations>>() {
            @Override
            public C.VariableDeclarations visitVariableDeclarations(C.VariableDeclarations multiVariable, Set<C.VariableDeclarations> vs) {
                if (multiVariable.getTypeExpression() instanceof C.MultiCatch) {
                    return multiVariable;
                }
                if (multiVariable.getTypeExpression() != null && TypeUtils.hasElementType(multiVariable.getTypeExpression()
                        .getType(), fullyQualifiedTypeName)) {
                    vs.add(multiVariable);
                }
                return multiVariable;
            }
        };

        Set<C.VariableDeclarations> vs = new HashSet<>();
        findVisitor.visit(c, vs);
        return vs;
    }
}
