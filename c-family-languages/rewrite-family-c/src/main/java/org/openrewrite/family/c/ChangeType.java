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
import org.openrewrite.family.c.marker.JavaSearchResult;
import org.openrewrite.family.c.search.UsesType;
import org.openrewrite.family.c.tree.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;

import java.util.Stack;

import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = true)
public class ChangeType extends Recipe {
    private static final Marker FOUND_TYPE = new JavaSearchResult(randomId(), null, null);

    @Option(displayName = "Old fully-qualified type name",
            description = "Fully-qualified class name of the original type.",
            example = "org.junit.Assume")
    String oldFullyQualifiedTypeName;

    @Option(displayName = "New fully-qualified type name",
            description = "Fully-qualified class name of the replacement type, or the name of a primitive such as \"int\".",
            example = "org.junit.jupiter.api.Assumptions")
    String newFullyQualifiedTypeName;

    @Override
    public String getDisplayName() {
        return "Change type";
    }

    @Override
    public String getDescription() {
        return "Change a given type to another.";
    }

    @Override
    public boolean causesAnotherCycle() {
        return true;
    }

    @Override
    public CVisitor<ExecutionContext> getVisitor() {
        return new ChangeTypeVisitor(newFullyQualifiedTypeName);
    }

    @Override
    protected CVisitor<ExecutionContext> getSingleSourceApplicableTest() {
        return new CIsoVisitor<ExecutionContext>() {
            @Override
            public CSourceFile visitSourceFile(CSourceFile sourceFile, ExecutionContext executionContext) {
                doAfterVisit(new UsesType<>(oldFullyQualifiedTypeName));
                return sourceFile;
            }

            @Override
            public C.ClassDeclaration visitClassDeclaration(C.ClassDeclaration classDecl, ExecutionContext executionContext) {
                if (TypeUtils.isOfClassType(classDecl.getType(), oldFullyQualifiedTypeName)) {
                    return classDecl.withMarkers(classDecl.getMarkers().addIfAbsent(FOUND_TYPE));
                }
                return classDecl;
            }
        };
    }

    private class ChangeTypeVisitor extends CVisitor<ExecutionContext> {
        private final CType targetType;
        private final CType.Class originalType = CType.Class.build(oldFullyQualifiedTypeName);

        private ChangeTypeVisitor(String targetType) {
            this.targetType = CType.buildType(targetType);
        }

        @Override
        public C visitSourceFile(CSourceFile sourceFile, ExecutionContext ctx) {
            maybeRemoveImport(oldFullyQualifiedTypeName);
            if (targetType instanceof CType.FullyQualified) {
                if (((CType.FullyQualified) targetType).getOwningClass() != null) {
                    maybeAddImport(((CType.FullyQualified) targetType).getOwningClass());
                } else {
                    maybeAddImport((CType.FullyQualified) targetType);
                }
            }
            return super.visitSourceFile(sourceFile, ctx);
        }

        @Override
        public <N extends NameTree> N visitTypeName(N name, ExecutionContext ctx) {
            N n = visitAndCast(name, ctx, super::visitTypeName);
            return n.withType(updateType(n.getType()));
        }

        @Override
        public C visitAnnotation(C.Annotation annotation, ExecutionContext ctx) {
            C.Annotation a = visitAndCast(annotation, ctx, super::visitAnnotation);
            return a.withAnnotationType(transformName(a.getAnnotationType()));
        }

        @Override
        public C visitArrayType(C.ArrayType arrayType, ExecutionContext ctx) {
            C.ArrayType a = visitAndCast(arrayType, ctx, super::visitArrayType);
            return a.withElementType(transformName(a.getElementType()));
        }

        @Override
        public C visitClassDeclaration(C.ClassDeclaration classDecl, ExecutionContext ctx) {
            C.ClassDeclaration c = visitAndCast(classDecl, ctx, super::visitClassDeclaration);

            if (c.getExtends() != null) {
                c = c.withExtends(transformName(c.getExtends()));
            }

            if (c.getImplements() != null) {
                c = c.withImplements(ListUtils.map(c.getImplements(), this::transformName));
            }

            return c;
        }

        @Override
        public C visitFieldAccess(C.FieldAccess fieldAccess, ExecutionContext ctx) {
            if (fieldAccess.isFullyQualifiedClassReference(oldFullyQualifiedTypeName)) {
                if (targetType instanceof CType.FullyQualified) {
                    return updateOuterClassTypes(TypeTree.build(((CType.FullyQualified) targetType).getFullyQualifiedName())
                            .withPrefix(fieldAccess.getPrefix()));
                } else if (targetType instanceof CType.Primitive) {
                    return new C.Primitive(
                            fieldAccess.getId(),
                            fieldAccess.getPrefix(),
                            Markers.EMPTY,
                            (CType.Primitive) targetType
                    );
                }
            } else {
                StringBuilder maybeClass = new StringBuilder();
                for (Expression target = fieldAccess; target != null; ) {
                    if (target instanceof C.FieldAccess) {
                        C.FieldAccess fa = (C.FieldAccess) target;
                        maybeClass.insert(0, fa.getSimpleName()).insert(0, '.');
                        target = fa.getTarget();
                    } else if (target instanceof C.Identifier) {
                        maybeClass.insert(0, ((C.Identifier) target).getSimpleName());
                        target = null;
                    } else {
                        maybeClass = new StringBuilder("__NOT_IT__");
                        break;
                    }
                }
                CType.Class oldType = CType.Class.build(oldFullyQualifiedTypeName);
                if (maybeClass.toString().equals(oldType.getClassName())) {
                    maybeRemoveImport(oldType.getOwningClass());
                    Expression e = updateOuterClassTypes(TypeTree.build(((CType.FullyQualified) targetType).getClassName())
                            .withPrefix(fieldAccess.getPrefix()));
                    // If a FieldAccess like Map.Entry has been replaced with an Identifier, ensure that identifier has the correct type
                    if(e instanceof C.Identifier && e.getType() == null) {
                        C.Identifier i = (C.Identifier) e;
                        e = i.withType(targetType);
                    }
                    return e;
                }
            }
            return super.visitFieldAccess(fieldAccess, ctx);
        }

        @Override
        public C visitIdentifier(C.Identifier ident, ExecutionContext ctx) {
            // if the ident's type is equal to the type we're looking for, and the classname of the type we're looking for is equal to the ident's string representation
            // Then transform it, otherwise leave it alone
            C.Identifier i = visitAndCast(ident, ctx, super::visitIdentifier);

            if (TypeUtils.isOfClassType(i.getType(), oldFullyQualifiedTypeName)) {
                String className = originalType.getClassName();
                CType.FullyQualified iType = TypeUtils.asFullyQualified(i.getType());
                if (iType != null && iType.getOwningClass() != null) {
                    className = originalType.getFullyQualifiedName().substring(iType.getOwningClass().getFullyQualifiedName().length() + 1);
                }

                if (i.getSimpleName().equals(className)) {
                    if (targetType instanceof CType.FullyQualified) {
                        if (((CType.FullyQualified) targetType).getOwningClass() != null) {
                            return updateOuterClassTypes(TypeTree.build(((CType.FullyQualified) targetType).getClassName())
                                    .withType(null)
                                    .withPrefix(i.getPrefix()));
                        } else {
                            i = i.withName(((CType.FullyQualified) targetType).getClassName());
                        }
                    } else if (targetType instanceof CType.Primitive) {
                        i = i.withName(((CType.Primitive) targetType).getKeyword());
                    }
                }
            }
            return i.withType(updateType(i.getType()));
        }

        @Override
        public C visitMethodDeclaration(C.MethodDeclaration method, ExecutionContext ctx) {
            C.MethodDeclaration m = visitAndCast(method, ctx, super::visitMethodDeclaration);
            m = m.withReturnTypeExpression(transformName(m.getReturnTypeExpression()));
            return m.withThrows(m.getThrows() == null ? null : ListUtils.map(m.getThrows(), this::transformName));
        }

        @Override
        public C visitMethodInvocation(C.MethodInvocation method, ExecutionContext ctx) {
            C.MethodInvocation m = method;
            boolean isStatic = m.getType() != null && m.getType().hasFlags(Flag.Static);
            if (m.getSelect() instanceof NameTree && isStatic) {
                m = m.withSelect(transformName(m.getSelect()));
            }

            Expression select = updateType(m.getSelect());
            m = m.withSelect(select).withType(updateType(method.getType()));

            if (m != method && isStatic && targetType instanceof CType.FullyQualified) {
                maybeAddImport(((CType.FullyQualified) targetType).getFullyQualifiedName(), m.getName().getSimpleName());
            }
            return super.visitMethodInvocation(m, ctx);
        }

        @Override
        public C visitMultiCatch(C.MultiCatch multiCatch, ExecutionContext ctx) {
            C.MultiCatch m = visitAndCast(multiCatch, ctx, super::visitMultiCatch);
            return m.withAlternatives(ListUtils.map(m.getAlternatives(), this::transformName));
        }

        @Override
        public C visitVariableDeclarations(C.VariableDeclarations multiVariable, ExecutionContext ctx) {
            C.VariableDeclarations m = visitAndCast(multiVariable, ctx, super::visitVariableDeclarations);
            if (!(m.getTypeExpression() instanceof C.MultiCatch)) {
                m = m.withTypeExpression(transformName(m.getTypeExpression()));
            }
            return m;
        }

        @Override
        public C.VariableDeclarations.NamedVariable visitVariable(C.VariableDeclarations.NamedVariable variable, ExecutionContext ctx) {
            C.VariableDeclarations.NamedVariable v = visitAndCast(variable, ctx, super::visitVariable);
            return v.withType(updateType(v.getType()))
                    .withName(v.getName().withType(updateType(v.getName().getType())));
        }

        @Override
        public C visitNewArray(C.NewArray newArray, ExecutionContext ctx) {
            C.NewArray n = visitAndCast(newArray, ctx, super::visitNewArray);
            return n.withTypeExpression(transformName(n.getTypeExpression()));
        }

        @Override
        public C visitNewClass(C.NewClass newClass, ExecutionContext ctx) {
            C.NewClass n = visitAndCast(newClass, ctx, super::visitNewClass);
            return n.withClazz(transformName(n.getClazz()))
                    .withType(updateType(n.getType()));
        }

        @Override
        public C visitTypeCast(C.TypeCast typeCast, ExecutionContext ctx) {
            C.TypeCast t = visitAndCast(typeCast, ctx, super::visitTypeCast);
            return t.withClazz(t.getClazz().withTree(transformName(t.getClazz().getTree())));
        }

        @Override
        public C visitTypeParameter(C.TypeParameter typeParam, ExecutionContext ctx) {
            C.TypeParameter t = visitAndCast(typeParam, ctx, super::visitTypeParameter);
            t = t.withBounds(t.getBounds() == null ? null : ListUtils.map(t.getBounds(), this::transformName));
            return t.withName(transformName(t.getName()));
        }

        @Override
        public C visitWildcard(C.Wildcard wildcard, ExecutionContext ctx) {
            C.Wildcard w = visitAndCast(wildcard, ctx, super::visitWildcard);
            return w.withBoundedType(transformName(w.getBoundedType()));
        }

        @SuppressWarnings({"unchecked", "ConstantConditions"})
        private <T extends C> T transformName(@Nullable T nameField) {
            if (nameField instanceof NameTree) {
                CType.FullyQualified nameTreeClass = TypeUtils.asFullyQualified(((NameTree) nameField).getType());
                String name;
                if (targetType instanceof CType.FullyQualified) {
                    name = ((CType.FullyQualified) targetType).getClassName();
                } else {
                    name = ((CType.Primitive) targetType).getKeyword();
                }
                if (nameTreeClass != null && nameTreeClass.getFullyQualifiedName().equals(oldFullyQualifiedTypeName)) {
                    return (T) C.Identifier.build(randomId(),
                            nameField.getPrefix(),
                            nameField.getMarkers(),
                            name,
                            targetType
                    );
                }
            }
            return nameField;
        }

        private Expression updateOuterClassTypes(Expression typeTree) {
            if (typeTree instanceof C.FieldAccess) {
                CType.FullyQualified type = (CType.FullyQualified) targetType;

                if(type.getOwningClass() == null) {
                    // just a performance shortcut when this isn't an inner class
                    typeTree.withType(updateType(targetType));
                }

                Stack<Expression> typeStack = new Stack<>();
                typeStack.push(typeTree);

                Stack<CType.FullyQualified> attrStack = new Stack<>();
                attrStack.push(type);

                for (Expression t = ((C.FieldAccess) typeTree).getTarget(); ; ) {
                    typeStack.push(t);
                    if (t instanceof C.FieldAccess) {
                        if (Character.isUpperCase(((C.FieldAccess) t).getSimpleName().charAt(0))) {
                            if(attrStack.peek().getOwningClass() != null) {
                                attrStack.push(attrStack.peek().getOwningClass());
                            }
                        }
                        t = ((C.FieldAccess) t).getTarget();
                    } else if (t instanceof C.Identifier) {
                        if (Character.isUpperCase(((C.Identifier) t).getSimpleName().charAt(0))) {
                            if(attrStack.peek().getOwningClass() != null) {
                                attrStack.push(attrStack.peek().getOwningClass());
                            }
                        }
                        break;
                    }
                }

                Expression attributed = null;
                for (Expression e = typeStack.pop(); ; e = typeStack.pop()) {
                    if (e instanceof C.Identifier) {
                        if (attrStack.size() == typeStack.size() + 1) {
                            attributed = ((C.Identifier) e).withType(attrStack.pop());
                        } else {
                            attributed = e;
                        }
                    } else if (e instanceof C.FieldAccess) {
                        if (attrStack.size() == typeStack.size() + 1) {
                            attributed = ((C.FieldAccess) e).withTarget(attributed)
                                    .withType(attrStack.pop());
                        } else {
                            attributed = ((C.FieldAccess) e).withTarget(attributed);
                        }
                    }
                    if (typeStack.isEmpty()) {
                        break;
                    }
                }

                assert attributed != null;
                return attributed;
            }
            return typeTree;
        }

        private Expression updateType(@Nullable Expression typeTree) {
            if (typeTree == null) {
                // updateType/updateSignature are always used to swap things in-place
                // The true nullability is that the return has the same nullability as the input
                // Because it's always an in-place operation it isn't problematic to tell a white lie about the nullability of the return value

                //noinspection ConstantConditions
                return null;
            }

            return typeTree.withType(updateType(typeTree.getType()));
        }

        private CType updateType(@Nullable CType type) {
            CType.FullyQualified fqt = TypeUtils.asFullyQualified(type);
            if (fqt != null && fqt.getFullyQualifiedName().equals(oldFullyQualifiedTypeName)) {
                return targetType;
            }
            CType.GenericTypeVariable gtv = TypeUtils.asGeneric(type);
            if (gtv != null && gtv.getBound() != null
                    && gtv.getBound().getFullyQualifiedName().equals(oldFullyQualifiedTypeName)
                    && targetType instanceof CType.FullyQualified) {
                return gtv.withBound((CType.FullyQualified) targetType);
            }
            CType.Method mt = TypeUtils.asMethod(type);
            if (mt != null) {
                return mt.withDeclaringType((CType.FullyQualified) updateType(mt.getDeclaringType()))
                        .withResolvedSignature(updateSignature(mt.getResolvedSignature()))
                        .withGenericSignature(updateSignature(mt.getGenericSignature()));
            }

            //noinspection ConstantConditions
            return type;
        }

        private CType.Method.Signature updateSignature(@Nullable CType.Method.Signature signature) {
            if (signature == null) {
                //noinspection ConstantConditions
                return signature;
            }

            return signature.withReturnType(updateType(signature.getReturnType()))
                    .withParamTypes(ListUtils.map(signature.getParamTypes(), this::updateType));
        }
    }
}
