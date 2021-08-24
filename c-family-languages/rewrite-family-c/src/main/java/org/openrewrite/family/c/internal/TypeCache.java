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
package org.openrewrite.family.c.internal;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.openrewrite.family.c.tree.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.family.c.CIsoVisitor;

import java.util.HashSet;
import java.util.Set;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class TypeCache {
    private final CSourceFile cu;
    private final Set<CType> typesInUse;
    private final Set<CType.Method> declaredMethods;

    public static TypeCache build(CSourceFile cu) {
        Set<CType> types = new HashSet<CType>() {
            @Override
            public boolean add(@Nullable CType CType) {
                if (CType != null) {
                    return super.add(CType);
                }
                return false;
            }
        };

        Set<CType.Method> declaredMethods = new HashSet<CType.Method>() {
            @Override
            public boolean add(@Nullable CType.Method CType) {
                if (CType != null) {
                    return super.add(CType);
                }
                return false;
            }
        };

        new CIsoVisitor<Integer>() {

            @Override
            public <N extends NameTree> N visitTypeName(N nameTree, Integer p) {
                visitSpace(nameTree.getPrefix(), Space.Location.ANY, p);
                types.add(nameTree.getType());
                return super.visitTypeName(nameTree, p);
            }

            @Override
            public C.ArrayAccess visitArrayAccess(C.ArrayAccess arrayAccess, Integer p) {
                visitSpace(arrayAccess.getPrefix(), Space.Location.ANY, p);
                types.add(arrayAccess.getType());
                return super.visitArrayAccess(arrayAccess, p);
            }

            @Override
            public C.Assignment visitAssignment(C.Assignment assignment, Integer p) {
                visitSpace(assignment.getPrefix(), Space.Location.ANY, p);
                types.add(assignment.getType());
                return super.visitAssignment(assignment, p);
            }

            @Override
            public C.AssignmentOperation visitAssignmentOperation(C.AssignmentOperation assignOp, Integer p) {
                visitSpace(assignOp.getPrefix(), Space.Location.ANY, p);
                types.add(assignOp.getType());
                return super.visitAssignmentOperation(assignOp, p);
            }

            @Override
            public C.Binary visitBinary(C.Binary binary, Integer p) {
                visitSpace(binary.getPrefix(), Space.Location.ANY, p);
                types.add(binary.getType());
                return super.visitBinary(binary, p);
            }

            @Override
            public C.ClassDeclaration visitClassDeclaration(C.ClassDeclaration c, Integer p) {
                visitSpace(c.getPrefix(), Space.Location.ANY, p);
                for (C.Annotation annotation : c.getAllAnnotations()) {
                    visit(annotation, p);
                }
                if (c.getPadding().getTypeParameters() != null) {
                    visitContainer(c.getPadding().getTypeParameters(), CContainer.Location.TYPE_PARAMETERS, p);
                }
                if (c.getPadding().getExtends() != null) {
                    visitLeftPadded(c.getPadding().getExtends(), CLeftPadded.Location.EXTENDS, p);
                }
                if (c.getPadding().getImplements() != null) {
                    visitContainer(c.getPadding().getImplements(), CContainer.Location.IMPLEMENTS, p);
                }
                visit(c.getBody(), p);
                return c;
            }

            @Override
            public C.Identifier visitIdentifier(C.Identifier identifier, Integer p) {
                visitSpace(identifier.getPrefix(), Space.Location.ANY, p);
                types.add(identifier.getType());
                types.add(identifier.getFieldType());
                return super.visitIdentifier(identifier, p);
            }

            @Override
            public C.Import visitImport(C.Import impoort, Integer p) {
                visitSpace(impoort.getPrefix(), Space.Location.ANY, p);
                return impoort;
            }

            @Override
            public C.Package visitPackage(C.Package pkg, Integer p) {
                visitSpace(pkg.getPrefix(), Space.Location.ANY, p);
                for (C.Annotation annotation : pkg.getAnnotations()) {
                    visit(annotation, p);
                }
                return pkg;
            }

            @Override
            public C.InstanceOf visitInstanceOf(C.InstanceOf instanceOf, Integer p) {
                visitSpace(instanceOf.getPrefix(), Space.Location.ANY, p);
                types.add(instanceOf.getType());
                return super.visitInstanceOf(instanceOf, p);
            }

            @Override
            public C.Lambda visitLambda(C.Lambda lambda, Integer p) {
                visitSpace(lambda.getPrefix(), Space.Location.ANY, p);
                types.add(lambda.getType());
                return super.visitLambda(lambda, p);
            }

            @Override
            public C.Literal visitLiteral(C.Literal literal, Integer p) {
                visitSpace(literal.getPrefix(), Space.Location.ANY, p);
                types.add(literal.getType());
                return super.visitLiteral(literal, p);
            }

            @Override
            public C.MemberReference visitMemberReference(C.MemberReference memberRef, Integer p) {
                visitSpace(memberRef.getPrefix(), Space.Location.ANY, p);
                types.add(memberRef.getType());
                types.add(memberRef.getReferenceType());
                return super.visitMemberReference(memberRef, p);
            }

            @Override
            public C.MethodDeclaration visitMethodDeclaration(C.MethodDeclaration method, Integer p) {
                visitSpace(method.getPrefix(), Space.Location.ANY, p);
                declaredMethods.add(method.getType());
                return super.visitMethodDeclaration(method, p);
            }

            @Override
            public C.MethodInvocation visitMethodInvocation(C.MethodInvocation method, Integer p) {
                visitSpace(method.getPrefix(), Space.Location.ANY, p);
                types.add(method.getType());
                types.add(method.getReturnType());
                return super.visitMethodInvocation(method, p);
            }

            @Override
            public C.MultiCatch visitMultiCatch(C.MultiCatch multiCatch, Integer p) {
                visitSpace(multiCatch.getPrefix(), Space.Location.ANY, p);
                types.add(multiCatch.getType());
                return super.visitMultiCatch(multiCatch, p);
            }

            @Override
            public C.NewArray visitNewArray(C.NewArray newArray, Integer p) {
                visitSpace(newArray.getPrefix(), Space.Location.ANY, p);
                types.add(newArray.getType());
                return super.visitNewArray(newArray, p);
            }

            @Override
            public C.NewClass visitNewClass(C.NewClass newClass, Integer p) {
                visitSpace(newClass.getPrefix(), Space.Location.ANY, p);
                types.add(newClass.getType());
                return super.visitNewClass(newClass, p);
            }

            @Override
            public C.ParameterizedType visitParameterizedType(C.ParameterizedType type, Integer p) {
                visitSpace(type.getPrefix(), Space.Location.ANY, p);
                types.add(type.getType());
                return super.visitParameterizedType(type, p);
            }

            @Override
            public C.Primitive visitPrimitive(C.Primitive primitive, Integer p) {
                visitSpace(primitive.getPrefix(), Space.Location.ANY, p);
                types.add(primitive.getType());
                return super.visitPrimitive(primitive, p);
            }

            @Override
            public C.Ternary visitTernary(C.Ternary ternary, Integer p) {
                visitSpace(ternary.getPrefix(), Space.Location.ANY, p);
                types.add(ternary.getType());
                return super.visitTernary(ternary, p);
            }

            @Override
            public C.TypeCast visitTypeCast(C.TypeCast typeCast, Integer p) {
                visitSpace(typeCast.getPrefix(), Space.Location.ANY, p);
                types.add(typeCast.getType());
                return super.visitTypeCast(typeCast, p);
            }

            @Override
            public C.Unary visitUnary(C.Unary unary, Integer p) {
                visitSpace(unary.getPrefix(), Space.Location.ANY, p);
                types.add(unary.getType());
                return super.visitUnary(unary, p);
            }

            @Override
            public C.VariableDeclarations.NamedVariable visitVariable(C.VariableDeclarations.NamedVariable variable, Integer p) {
                visitSpace(variable.getPrefix(), Space.Location.ANY, p);
                types.add(variable.getType());
                return super.visitVariable(variable, p);
            }
        }.visit(cu, 0);

        return new TypeCache(cu, types, declaredMethods);
    }
}
