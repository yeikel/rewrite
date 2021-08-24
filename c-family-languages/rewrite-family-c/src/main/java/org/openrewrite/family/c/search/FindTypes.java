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
import org.openrewrite.family.c.tree.C;
import org.openrewrite.family.c.tree.CType;
import org.openrewrite.family.c.tree.NameTree;
import org.openrewrite.family.c.tree.TypeUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.family.c.CIsoVisitor;
import org.openrewrite.family.c.CVisitor;
import org.openrewrite.family.c.marker.JavaSearchResult;

import java.util.HashSet;
import java.util.Set;

/**
 * This recipe finds all explicit references to a type.
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class FindTypes extends Recipe {

    @Option(displayName = "Fully-qualified type name",
            description = "A fully-qualified type name, that is used to find matching type references.",
            example = "java.util.List")
    String fullyQualifiedTypeName;

    @Option(displayName = "Check for assignability",
            description = "When enabled, find type references that are assignable to the provided type.",
            required = false)
    @Nullable
    Boolean checkAssignability;

    @Override
    public String getDisplayName() {
        return "Find types";
    }

    @Override
    public String getDescription() {
        return "Find type references by name.";
    }

    @Override
    protected CVisitor<ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesType<>(fullyQualifiedTypeName);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        CType.FullyQualified fullyQualifiedType = CType.Class.build(fullyQualifiedTypeName);

        return new CVisitor<ExecutionContext>() {
            @Override
            public C visitIdentifier(C.Identifier ident, ExecutionContext executionContext) {
                if (ident.getType() != null) {
                    CType.FullyQualified type = TypeUtils.asFullyQualified(ident.getType());
                    if (typeMatches(Boolean.TRUE.equals(checkAssignability), fullyQualifiedType, type) &&
                            ident.getSimpleName().equals(type.getClassName())) {
                        return ident.withMarkers(ident.getMarkers().addIfAbsent(new JavaSearchResult(FindTypes.this)));
                    }
                }
                return super.visitIdentifier(ident, executionContext);
            }

            @Override
            public <N extends NameTree> N visitTypeName(N name, ExecutionContext ctx) {
                N n = super.visitTypeName(name, ctx);
                CType.FullyQualified type = TypeUtils.asFullyQualified(n.getType());
                if (typeMatches(Boolean.TRUE.equals(checkAssignability), fullyQualifiedType, type) &&
                        getCursor().firstEnclosing(C.Import.class) == null) {
                    return n.withMarkers(n.getMarkers().addIfAbsent(new JavaSearchResult(FindTypes.this)));
                }
                return n;
            }

            @Override
            public C visitFieldAccess(C.FieldAccess fieldAccess, ExecutionContext ctx) {
                C.FieldAccess fa = (C.FieldAccess) super.visitFieldAccess(fieldAccess, ctx);
                CType.FullyQualified type = TypeUtils.asFullyQualified(fa.getTarget().getType());
                if (typeMatches(Boolean.TRUE.equals(checkAssignability), fullyQualifiedType, type) &&
                        fa.getName().getSimpleName().equals("class")) {
                    return fa.withMarkers(fa.getMarkers().addIfAbsent(new JavaSearchResult(FindTypes.this)));
                }
                return fa;
            }
        };
    }

    public static Set<NameTree> findAssignable(C j, String fullyQualifiedClassName) {
        return find(true, j, fullyQualifiedClassName);
    }

    public static Set<NameTree> find(C j, String fullyQualifiedClassName) {
        return find(false, j, fullyQualifiedClassName);
    }

    private static Set<NameTree> find(boolean checkAssignability, C j, String fullyQualifiedClassName) {
        CType.FullyQualified fullyQualifiedType = CType.Class.build(fullyQualifiedClassName);

        CIsoVisitor<Set<NameTree>> findVisitor = new CIsoVisitor<Set<NameTree>>() {
            @Override
            public C.Identifier visitIdentifier(C.Identifier ident, Set<NameTree> ns) {
                if (ident.getType() != null) {
                    CType.FullyQualified type = TypeUtils.asFullyQualified(ident.getType());
                    if (typeMatches(checkAssignability, fullyQualifiedType, type) && ident.getSimpleName().equals(type.getClassName())) {
                        ns.add(ident);
                    }
                }
                return super.visitIdentifier(ident, ns);
            }

            @Override
            public <N extends NameTree> N visitTypeName(N name, Set<NameTree> ns) {
                N n = super.visitTypeName(name, ns);
                CType.FullyQualified type = TypeUtils.asFullyQualified(n.getType());
                if (typeMatches(checkAssignability, fullyQualifiedType, type) &&
                        getCursor().firstEnclosing(C.Import.class) == null) {
                    ns.add(name);
                }
                return n;
            }

            @Override
            public C.FieldAccess visitFieldAccess(C.FieldAccess fieldAccess, Set<NameTree> ns) {
                C.FieldAccess fa = super.visitFieldAccess(fieldAccess, ns);
                CType.FullyQualified type = TypeUtils.asFullyQualified(fa.getTarget().getType());
                if (typeMatches(checkAssignability, fullyQualifiedType, type) &&
                        fa.getName().getSimpleName().equals("class")) {
                    ns.add(fieldAccess);
                }
                return fa;
            }
        };

        Set<NameTree> ts = new HashSet<>();
        findVisitor.visit(j, ts);
        return ts;
    }

    private static boolean typeMatches(boolean checkAssignability, CType.FullyQualified match,
                                       @Nullable CType.FullyQualified test) {
        return test != null && (checkAssignability ?
                match.isAssignableFrom(test) :
                match.getFullyQualifiedName().equals(test.getFullyQualifiedName())
        );
    }
}
