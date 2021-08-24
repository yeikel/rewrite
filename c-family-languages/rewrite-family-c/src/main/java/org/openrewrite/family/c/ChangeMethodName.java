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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.family.c.search.DeclaresMethod;
import org.openrewrite.family.c.search.UsesMethod;
import org.openrewrite.family.c.tree.*;

@Value
@EqualsAndHashCode(callSuper = true)
public class ChangeMethodName extends Recipe {

    @Option(displayName = "Method pattern",
            description = "A method pattern, expressed as a pointcut expression, that is used to find matching method declarations/invocations.",
            example = "org.mockito.Matchers anyVararg()")
    String methodPattern;

    @Option(displayName = "New method name",
            description = "The method name that will replace the existing name.",
            example = "any")
    String newMethodName;

    @Override
    public String getDisplayName() {
        return "Change method name";
    }

    @Override
    public String getDescription() {
        return "Rename a method.";
    }

    @Override
    protected CVisitor<ExecutionContext> getSingleSourceApplicableTest() {
        return new CIsoVisitor<ExecutionContext>() {
            @Override
            public CSourceFile visitSourceFile(CSourceFile cu, ExecutionContext executionContext) {
                doAfterVisit(new UsesMethod<>(methodPattern));
                doAfterVisit(new DeclaresMethod<>(methodPattern));
                return cu;
            }
        };
    }

    @Override
    public boolean causesAnotherCycle() {
        return true;
    }

    @Override
    public CVisitor<ExecutionContext> getVisitor() {
        return new ChangeMethodNameVisitor(new MethodMatcher(methodPattern));
    }

    private class ChangeMethodNameVisitor extends CIsoVisitor<ExecutionContext> {
        private final MethodMatcher methodMatcher;

        private ChangeMethodNameVisitor(MethodMatcher methodMatcher) {
            this.methodMatcher = methodMatcher;
        }

        @Override
        public C.MethodDeclaration visitMethodDeclaration(C.MethodDeclaration method, ExecutionContext ctx) {
            C.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
            C.ClassDeclaration classDecl = getCursor().firstEnclosingOrThrow(C.ClassDeclaration.class);
            if (methodMatcher.matches(method, classDecl)) {
                CType.Method type = m.getType();
                if(type != null) {
                    type = type.withName(newMethodName);
                }
                m = m.withName(m.getName().withName(newMethodName))
                        .withType(type);
            }
            return m;
        }

        @Override
        public C.MethodInvocation visitMethodInvocation(C.MethodInvocation method, ExecutionContext ctx) {
            C.MethodInvocation m = super.visitMethodInvocation(method, ctx);
            if (methodMatcher.matches(method) && !method.getSimpleName().equals(newMethodName)) {
                CType.Method type = m.getType();
                if(type != null) {
                    type = type.withName(newMethodName);
                }
                m = m.withName(m.getName().withName(newMethodName))
                        .withType(type);
            }
            return m;
        }

        @Override
        public C.MemberReference visitMemberReference(C.MemberReference memberRef, ExecutionContext context) {
            C.MemberReference m = super.visitMemberReference(memberRef, context);
            if (methodMatcher.matches(m.getReferenceType()) && !m.getReference().getSimpleName().equals(newMethodName)) {
                CType type = m.getReferenceType();
                if(type instanceof CType.Method) {
                    CType.Method mtype = (CType.Method) type;
                    type = mtype.withName(newMethodName);
                }
                m = m.withReference(m.getReference().withName(newMethodName))
                        .withReferenceType(type);
            }
            return m;
        }

        /**
         * The only time field access should be relevant to changing method names is static imports.
         * This exists to turn
         * import static com.abc.B.static1;
         * into
         * import static com.abc.B.static2;
         */
        @Override
        public C.FieldAccess visitFieldAccess(C.FieldAccess fieldAccess, ExecutionContext ctx) {
            C.FieldAccess f = super.visitFieldAccess(fieldAccess, ctx);
            if (methodMatcher.isFullyQualifiedClassReference(f)) {
                Expression target = f.getTarget();
                if (target instanceof C.FieldAccess) {
                    String className = target.printTrimmed(getCursor());
                    String fullyQualified = className + "." + newMethodName;
                    return TypeTree.build(fullyQualified)
                            .withPrefix(f.getPrefix());
                }
            }
            return f;
        }
    }
}
