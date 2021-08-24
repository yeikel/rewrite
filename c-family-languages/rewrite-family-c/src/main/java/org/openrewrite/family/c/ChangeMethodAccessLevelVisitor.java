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
package org.openrewrite.family.c;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.family.c.tree.Comment;
import org.openrewrite.family.c.tree.C;
import org.openrewrite.family.c.tree.Space;
import org.openrewrite.family.c.tree.TypeTree;
import org.openrewrite.marker.Markers;

import java.util.*;

import static java.util.Collections.emptyList;

@Value
@EqualsAndHashCode(callSuper = true)
public class ChangeMethodAccessLevelVisitor<P> extends CIsoVisitor<P> {
    private static final Collection<C.Modifier.Type> EXPLICIT_ACCESS_LEVELS = Arrays.asList(C.Modifier.Type.Public,
            C.Modifier.Type.Private, C.Modifier.Type.Protected);

    MethodMatcher methodMatcher;

    @Nullable
    C.Modifier.Type newAccessLevel;

    @Override
    public C.MethodDeclaration visitMethodDeclaration(C.MethodDeclaration method, P p) {
        C.MethodDeclaration m = super.visitMethodDeclaration(method, p);
        C.ClassDeclaration classDecl = getCursor().firstEnclosingOrThrow(C.ClassDeclaration.class);
        if (methodMatcher.matches(method, classDecl)) {
            C.Modifier.Type currentMethodAccessLevel = m.getModifiers().stream()
                    .map(C.Modifier::getType)
                    .filter(EXPLICIT_ACCESS_LEVELS::contains)
                    .findAny()
                    .orElse(null);

            if (currentMethodAccessLevel == newAccessLevel) {
                // No changes required
                return m;
            }

            // Replace former modifier by new modifier if package-private is not involved
            if (EXPLICIT_ACCESS_LEVELS.contains(currentMethodAccessLevel) && EXPLICIT_ACCESS_LEVELS.contains(newAccessLevel)) {
                m = m.withModifiers(
                        ListUtils.map(m.getModifiers(), mod -> mod.getType() == currentMethodAccessLevel ?
                                mod.withType(newAccessLevel) : mod)
                );
            }

            // If current access level is package-private (no modifier), add the new modifier
            else if (currentMethodAccessLevel == null) {
                C.Modifier mod = new C.Modifier(Tree.randomId(), Space.build(" ", emptyList()), Markers.EMPTY, newAccessLevel, Collections.emptyList());
                m = m.withModifiers(ListUtils.concat(mod, m.getModifiers()));

                if(method.getModifiers().isEmpty()) {
                    C.TypeParameters typeParams = m.getPadding().getTypeParameters();
                    if(typeParams == null) {
                        TypeTree returnExpr = m.getReturnTypeExpression();
                        if(returnExpr == null) {
                            m = m.withModifiers(Space.formatFirstPrefix(m.getModifiers(), m.getName().getPrefix()));
                            m = m.withName(m.getName().withPrefix(Space.format(" ")));
                        } else {
                            m = m.withModifiers(Space.formatFirstPrefix(m.getModifiers(), returnExpr.getPrefix()));
                            m = m.withReturnTypeExpression(returnExpr.withPrefix(Space.format(" ")));
                        }
                    } else {
                        m = m.withModifiers(Space.formatFirstPrefix(m.getModifiers(), typeParams.getPrefix()));
                        m = m.getPadding().withTypeParameters(typeParams.withPrefix(Space.format(" ")));
                    }
                }
                else {
                    m = m.withModifiers(ListUtils.map(m.getModifiers(), (i, mod2) -> {
                        if (i == 0) {
                            return mod2.withPrefix(method.getModifiers().get(0).getPrefix());
                        } else if (i == 1) {
                            return mod2.withPrefix(Space.format(" "));
                        }
                        return mod2;
                    }));
                }
            }

            // If target access level is package-private (no modifier), remove the current access level modifier
            // and copy any associated comments
            else if (newAccessLevel == null) {
                final List<Comment> modifierComments = new ArrayList<>();
                List<C.Modifier> modifiers = ListUtils.map(m.getModifiers(), mod -> {
                    if (mod.getType() == currentMethodAccessLevel) {
                        modifierComments.addAll(mod.getComments());
                        return null;
                    }

                    // copy access level modifier comment to next modifier if it exists
                    if (!modifierComments.isEmpty()) {
                        C.Modifier nextModifier = mod.withComments(ListUtils.concatAll(new ArrayList<>(modifierComments), mod.getComments()));
                        modifierComments.clear();
                        return nextModifier;
                    }
                    return mod;
                });

                // if no following modifier exists, add comments to method itself
                if (!modifierComments.isEmpty()) {
                    m = m.withComments(ListUtils.concatAll(m.getComments(), modifierComments));
                }
                m = maybeAutoFormat(m, m.withModifiers(modifiers), p, getCursor().dropParentUntil(C.class::isInstance));
            }
        }
        return m;
    }
}
