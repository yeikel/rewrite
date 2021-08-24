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
package org.openrewrite.family.c.tree;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.family.c.CVisitor;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
import org.openrewrite.template.SourceTemplate;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

@SuppressWarnings("unused")
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
public interface C extends Serializable, Tree {
    static void clearCaches() {
        Identifier.flyweights.clear();
        CType.clearCaches();
    }

    @SuppressWarnings("unchecked")
    @Override
    default <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        return (R) acceptC((CVisitor<P>) v, p);
    }

    @Override
    default <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v instanceof CVisitor;
    }

    @Nullable
    default <P> C acceptC(CVisitor<P> v, P p) {
        return v.defaultValue(this, p);
    }

    <C2 extends C> C2 withPrefix(Space space);

    Space getPrefix();

    default List<Comment> getComments() {
        return getPrefix().getComments();
    }

    default <C2 extends C> C2 withComments(List<Comment> comments) {
        return withPrefix(getPrefix().withComments(comments));
    }

    default <C2 extends C> C2 withTemplate(SourceTemplate<C, CCoordinates> template, CCoordinates coordinates, Object... parameters) {
        return template.withTemplate(this, coordinates, parameters);
    }

    <C2 extends C> C2 withMarkers(Markers markers);

    Markers getMarkers();

    @SuppressWarnings("unchecked")
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class AnnotatedType implements C, Expression, TypeTree {
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        List<Annotation> annotations;

        @With
        TypeTree typeExpression;

        @Override
        public CType getType() {
            return typeExpression.getType();
        }

        @Override
        public AnnotatedType withType(@Nullable CType type) {
            return withTypeExpression(typeExpression.withType(type));
        }

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitAnnotatedType(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class Annotation implements C, Expression {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @With
        @Getter
        NameTree annotationType;

        public String getSimpleName() {
            return annotationType instanceof Identifier ?
                    ((Identifier) annotationType).getSimpleName() :
                    ((FieldAccess) annotationType).getSimpleName();
        }

        @Nullable
        CContainer<Expression> arguments;

        @Nullable
        public List<Expression> getArguments() {
            return arguments == null ? null : arguments.getElements();
        }

        public Annotation withArguments(@Nullable List<Expression> arguments) {
            return getPadding().withArguments(CContainer.withElementsNullable(this.arguments, arguments));
        }

        @Override
        public CType getType() {
            return annotationType.getType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Annotation withType(@Nullable CType type) {
            return withAnnotationType(annotationType.withType(type));
        }

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitAnnotation(this, p);
        }

        public CoordinateBuilder.Annotation getCoordinates() {
            return new CoordinateBuilder.Annotation(this);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final Annotation t;

            @Nullable
            public CContainer<Expression> getArguments() {
                return t.arguments;
            }

            public Annotation withArguments(@Nullable CContainer<Expression> arguments) {
                return t.arguments == arguments ? t : new Annotation(t.id, t.prefix, t.markers, t.annotationType, arguments);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class ArrayAccess implements C, Expression, TypedTree {
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        Expression indexed;

        @With
        ArrayDimension dimension;

        @With
        @Nullable
        CType type;

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitArrayAccess(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class ArrayType implements C, TypeTree, Expression {
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        TypeTree elementType;

        @With
        List<CRightPadded<Space>> dimensions;

        @Override
        public CType getType() {
            return elementType.getType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public ArrayType withType(@Nullable CType type) {
            return type == getType() ? this : withElementType(elementType.withType(type));
        }

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitArrayType(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Assert implements C, Statement {
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        Expression condition;

        @Nullable
        @With
        CLeftPadded<Expression> detail;

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitAssert(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class Assignment implements C, Statement, Expression, TypedTree {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @With
        @Getter
        Expression variable;

        CLeftPadded<Expression> assignment;

        public Expression getAssignment() {
            return assignment.getElement();
        }

        public Assignment withAssignment(Expression assignment) {
            return getPadding().withAssignment(this.assignment.withElement(assignment));
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        @With
        @Nullable
        @Getter
        CType type;

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitAssignment(this, p);
        }

        @Override
        public List<C> getSideEffects() {
            return singletonList(this);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final Assignment t;

            public CLeftPadded<Expression> getAssignment() {
                return t.assignment;
            }

            public Assignment withAssignment(CLeftPadded<Expression> assignment) {
                return t.assignment == assignment ? t : new Assignment(t.id, t.prefix, t.markers, t.variable, assignment, t.type);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class AssignmentOperation implements C, Statement, Expression, TypedTree {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @With
        @Getter
        Expression variable;

        CLeftPadded<Type> operator;

        public Type getOperator() {
            return operator.getElement();
        }

        public AssignmentOperation withOperator(Type operator) {
            return getPadding().withOperator(this.operator.withElement(operator));
        }

        @With
        @Getter
        Expression assignment;

        @With
        @Nullable
        @Getter
        CType type;

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitAssignmentOperation(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        @Override
        public List<C> getSideEffects() {
            return singletonList(this);
        }

        public enum Type {
            Addition,
            Subtraction,
            Multiplication,
            Division,
            Modulo,
            BitAnd,
            BitOr,
            BitXor,
            LeftShift,
            RightShift,
            UnsignedRightShift
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final AssignmentOperation t;

            public CLeftPadded<Type> getOperator() {
                return t.operator;
            }

            public AssignmentOperation withOperator(CLeftPadded<Type> operator) {
                return t.operator == operator ? t : new AssignmentOperation(t.id, t.prefix, t.markers, t.variable, operator, t.assignment, t.type);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Data
    final class Binary implements C, Expression, TypedTree {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        Expression left;

        CLeftPadded<Type> operator;

        public Type getOperator() {
            return operator.getElement();
        }

        public Binary withOperator(Type operator) {
            return getPadding().withOperator(this.operator.withElement(operator));
        }

        @With
        Expression right;

        @With
        @Nullable
        CType type;

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitBinary(this, p);
        }

        @Override
        public List<C> getSideEffects() {
            List<C> sideEffects = new ArrayList<>(2);
            sideEffects.addAll(left.getSideEffects());
            sideEffects.addAll(right.getSideEffects());
            return sideEffects;
        }

        public enum Type {
            Addition,
            Subtraction,
            Multiplication,
            Division,
            Modulo,
            LessThan,
            GreaterThan,
            LessThanOrEqual,
            GreaterThanOrEqual,
            Equal,
            NotEqual,
            BitAnd,
            BitOr,
            BitXor,
            LeftShift,
            RightShift,
            UnsignedRightShift,
            Or,
            And
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final Binary t;

            public CLeftPadded<Type> getOperator() {
                return t.operator;
            }

            public Binary withOperator(CLeftPadded<Type> operator) {
                return t.operator == operator ? t : new Binary(t.id, t.prefix, t.markers, t.left, operator, t.right, t.type);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class Block implements C, Statement {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @Getter
        @EqualsAndHashCode.Include
        UUID id;

        @Getter
        @With
        Space prefix;

        @Getter
        @With
        Markers markers;

        CRightPadded<Boolean> statik;

        public boolean isStatic() {
            return statik.getElement();
        }

        public Block withStatic(boolean statik) {
            return getPadding().withStatic(this.statik.withElement(statik));
        }

        List<CRightPadded<Statement>> statements;

        public List<Statement> getStatements() {
            return CRightPadded.getElements(statements);
        }

        public Block withStatements(List<Statement> statements) {
            return getPadding().withStatements(CRightPadded.withElements(this.statements, statements));
        }

        @Getter
        @With
        Space end;

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitBlock(this, p);
        }

        public CoordinateBuilder.Block getCoordinates() {
            return new CoordinateBuilder.Block(this);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final Block t;

            public CRightPadded<Boolean> getStatic() {
                return t.statik;
            }

            public Block withStatic(CRightPadded<Boolean> statik) {
                return t.statik == statik ? t : new Block(t.id, t.prefix, t.markers, statik, t.statements, t.end);
            }

            public List<CRightPadded<Statement>> getStatements() {
                return t.statements;
            }

            public Block withStatements(List<CRightPadded<Statement>> statements) {
                return t.statements == statements ? t : new Block(t.id, t.prefix, t.markers, t.statik, statements, t.end);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Break implements C, Statement {
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        @Nullable
        C.Identifier label;

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitBreak(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class Case implements C, Statement {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @With
        @Getter
        Expression pattern;

        CContainer<Statement> statements;

        public List<Statement> getStatements() {
            return statements.getElements();
        }

        public Case withStatements(List<Statement> statements) {
            return getPadding().withStatements(this.statements.getPadding().withElements(CRightPadded.withElements(
                    this.statements.getPadding().getElements(), statements)));
        }

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitCase(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final Case t;

            public CContainer<Statement> getStatements() {
                return t.statements;
            }

            public Case withStatements(CContainer<Statement> statements) {
                return t.statements == statements ? t : new Case(t.id, t.prefix, t.markers, t.pattern, statements);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class ClassDeclaration implements C, Statement, TypedTree {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @Nullable
        @NonFinal
        transient WeakReference<Annotations> annotations;

        @With
        @Getter
        @EqualsAndHashCode.Include
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @With
        @Getter
        List<Annotation> leadingAnnotations;

        @With
        @Getter
        List<Modifier> modifiers;

        Kind kind;

        public Kind.Type getKind() {
            return kind.getType();
        }

        public ClassDeclaration withKind(Kind.Type type) {
            Kind k = getAnnotations().getKind();
            if (k.type == type) {
                return this;
            } else {
                return getAnnotations().withKind(k.withType(type));
            }
        }

        @With
        @Getter
        Identifier name;

        @Nullable
        CContainer<TypeParameter> typeParameters;

        @Nullable
        public List<TypeParameter> getTypeParameters() {
            return typeParameters == null ? null : typeParameters.getElements();
        }

        public ClassDeclaration withTypeParameters(@Nullable List<TypeParameter> typeParameters) {
            return getPadding().withTypeParameters(CContainer.withElementsNullable(this.typeParameters, typeParameters));
        }

        @Nullable
        CLeftPadded<TypeTree> extendings;

        @Nullable
        public TypeTree getExtends() {
            return extendings == null ? null : extendings.getElement();
        }

        public ClassDeclaration withExtends(@Nullable TypeTree extendings) {
            return getPadding().withExtends(CLeftPadded.withElement(this.extendings, extendings));
        }

        @Nullable
        CContainer<TypeTree> implementings;

        @Nullable
        public List<TypeTree> getImplements() {
            return implementings == null ? null : implementings.getElements();
        }

        public ClassDeclaration withImplements(@Nullable List<TypeTree> implementings) {
            return getPadding().withImplements(CContainer.withElementsNullable(this.implementings, implementings));
        }

        @With
        @Getter
        Block body;

        @Getter
        @Nullable
        CType.FullyQualified type;

        @SuppressWarnings("unchecked")
        @Override
        public ClassDeclaration withType(@Nullable CType type) {
            if (type == this.type) {
                return this;
            }

            if (!(type instanceof CType.FullyQualified)) {
                throw new IllegalArgumentException("A class can only be type attributed with a fully qualified type name");
            }

            return new ClassDeclaration(id, prefix, markers, leadingAnnotations, modifiers, kind, name, typeParameters, extendings, implementings, body, (CType.FullyQualified) type);
        }

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitClassDeclaration(this, p);
        }

        // gather annotations from everywhere they may occur
        public List<Annotation> getAllAnnotations() {
            List<Annotation> allAnnotations = new ArrayList<>(leadingAnnotations);
            for (Modifier modifier : modifiers) {
                allAnnotations.addAll(modifier.getAnnotations());
            }
            allAnnotations.addAll(kind.getAnnotations());
            return allAnnotations;
        }

        @Override
        public CoordinateBuilder.ClassDeclaration getCoordinates() {
            return new CoordinateBuilder.ClassDeclaration(this);
        }

        public String getSimpleName() {
            return name.getSimpleName();
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class Kind implements C {

            @With
            @Getter
            @EqualsAndHashCode.Include
            UUID id;

            @With
            @Getter
            Space prefix;

            @With
            @Getter
            Markers markers;

            @With
            @Getter
            List<Annotation> annotations;

            @With
            @Getter
            Type type;

            public enum Type {
                Class,
                Enum,
                Interface,
                Annotation
            }
        }

        public boolean hasModifier(Modifier.Type modifier) {
            return Modifier.hasModifier(getModifiers(), modifier);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final ClassDeclaration t;

            @Nullable
            public CLeftPadded<TypeTree> getExtends() {
                return t.extendings;
            }

            public ClassDeclaration withExtends(@Nullable CLeftPadded<TypeTree> extendings) {
                return t.extendings == extendings ? t : new ClassDeclaration(t.id, t.prefix, t.markers, t.leadingAnnotations, t.modifiers, t.kind, t.name, t.typeParameters, extendings, t.implementings, t.body, t.type);
            }

            @Nullable
            public CContainer<TypeTree> getImplements() {
                return t.implementings;
            }

            public ClassDeclaration withImplements(@Nullable CContainer<TypeTree> implementings) {
                return t.implementings == implementings ? t : new ClassDeclaration(t.id, t.prefix, t.markers, t.leadingAnnotations, t.modifiers, t.kind, t.name, t.typeParameters, t.extendings, implementings, t.body, t.type);
            }

            public Kind getKind() {
                return t.kind;
            }

            public ClassDeclaration withKind(Kind kind) {
                return t.kind == kind ? t : new ClassDeclaration(t.id, t.prefix, t.markers, t.leadingAnnotations, t.modifiers, kind, t.name, t.typeParameters, t.extendings, t.implementings, t.body, t.type);
            }

            @Nullable
            public CContainer<TypeParameter> getTypeParameters() {
                return t.typeParameters;
            }

            public ClassDeclaration withTypeParameters(@Nullable CContainer<TypeParameter> typeParameters) {
                return t.typeParameters == typeParameters ? t : new ClassDeclaration(t.id, t.prefix, t.markers, t.leadingAnnotations, t.modifiers, t.kind, t.name, typeParameters, t.extendings, t.implementings, t.body, t.type);
            }
        }

        public Annotations getAnnotations() {
            Annotations a;
            if (this.annotations == null) {
                a = new Annotations(this);
                this.annotations = new WeakReference<>(a);
            } else {
                a = this.annotations.get();
                if (a == null || a.t != this) {
                    a = new Annotations(this);
                    this.annotations = new WeakReference<>(a);
                }
            }
            return a;
        }

        @RequiredArgsConstructor
        public static class Annotations {
            private final ClassDeclaration t;

            public Kind getKind() {
                return t.kind;
            }

            public ClassDeclaration withKind(Kind kind) {
                return t.kind == kind ? t : new ClassDeclaration(t.id, t.prefix, t.markers, t.leadingAnnotations, t.modifiers, kind, t.name, t.typeParameters, t.extendings, t.implementings, t.body, t.type);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Continue implements C, Statement {
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        @Nullable
        C.Identifier label;

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitContinue(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class DoWhileLoop implements C, Loop {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        CRightPadded<Statement> body;

        public Statement getBody() {
            return body.getElement();
        }

        @SuppressWarnings("unchecked")
        public DoWhileLoop withBody(Statement body) {
            return getPadding().withBody(this.body.withElement(body));
        }

        CLeftPadded<ControlParentheses<Expression>> whileCondition;

        public ControlParentheses<Expression> getWhileCondition() {
            return whileCondition.getElement();
        }

        public DoWhileLoop withWhileCondition(ControlParentheses<Expression> whileCondition) {
            return getPadding().withWhileCondition(this.whileCondition.withElement(whileCondition));
        }

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitDoWhileLoop(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final DoWhileLoop t;

            public CRightPadded<Statement> getBody() {
                return t.body;
            }

            public DoWhileLoop withBody(CRightPadded<Statement> body) {
                return t.body == body ? t : new DoWhileLoop(t.id, t.prefix, t.markers, body, t.whileCondition);
            }

            public CLeftPadded<ControlParentheses<Expression>> getWhileCondition() {
                return t.whileCondition;
            }

            public DoWhileLoop withWhileCondition(CLeftPadded<ControlParentheses<Expression>> whileCondition) {
                return t.whileCondition == whileCondition ? t : new DoWhileLoop(t.id, t.prefix, t.markers, t.body, whileCondition);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Empty implements C, Statement, Expression, TypeTree {
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @Override
        public CType getType() {
            return null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Empty withType(@Nullable CType type) {
            return this;
        }

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitEmpty(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class EnumValue implements C {
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        List<Annotation> annotations;

        @With
        Identifier name;

        @With
        @Nullable
        NewClass initializer;

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitEnumValue(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class EnumValueSet implements C, Statement {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        List<CRightPadded<EnumValue>> enums;

        public List<EnumValue> getEnums() {
            return CRightPadded.getElements(enums);
        }

        public EnumValueSet withEnums(List<EnumValue> enums) {
            return getPadding().withEnums(CRightPadded.withElements(this.enums, enums));
        }

        @With
        @Getter
        boolean terminatedWithSemicolon;

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitEnumValueSet(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final EnumValueSet t;

            public List<CRightPadded<EnumValue>> getEnums() {
                return t.enums;
            }

            public EnumValueSet withEnums(List<CRightPadded<EnumValue>> enums) {
                return t.enums == enums ? t : new EnumValueSet(t.id, t.prefix, t.markers, enums, t.terminatedWithSemicolon);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class FieldAccess implements C, TypeTree, Expression {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @With
        @Getter
        Expression target;

        CLeftPadded<Identifier> name;

        public Identifier getName() {
            return name.getElement();
        }

        public FieldAccess withName(Identifier name) {
            return getPadding().withName(this.name.withElement(name));
        }

        @With
        @Getter
        @Nullable
        CType type;

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitFieldAccess(this, p);
        }

        public String getSimpleName() {
            return name.getElement().getSimpleName();
        }

        @Override
        public List<C> getSideEffects() {
            return target.getSideEffects();
        }

        /**
         * @return For expressions like {@code String.class}, this casts target expression to a {@link NameTree}.
         * If the field access is not a reference to a class type, returns null.
         */
        @Nullable
        public NameTree asClassReference() {
            if (target instanceof NameTree) {
                String fqn = null;
                if (type instanceof CType.Class) {
                    fqn = ((CType.Class) type).getFullyQualifiedName();
                } else if (type instanceof CType.ShallowClass) {
                    fqn = ((CType.ShallowClass) type).getFullyQualifiedName();
                }

                return "java.lang.Class".equals(fqn) ? (NameTree) target : null;
            }
            return null;
        }

        public boolean isFullyQualifiedClassReference(String className) {
            return isFullyQualifiedClassReference(this, className);
        }

        private boolean isFullyQualifiedClassReference(FieldAccess fieldAccess, String className) {
            if (!className.contains(".")) {
                return false;
            }
            if (!fieldAccess.getName().getSimpleName().equals(className.substring(className.lastIndexOf('.') + 1))) {
                return false;
            }
            if (fieldAccess.getTarget() instanceof FieldAccess) {
                return isFullyQualifiedClassReference((FieldAccess) fieldAccess.getTarget(), className.substring(0, className.lastIndexOf('.')));
            }
            if (fieldAccess.getTarget() instanceof Identifier) {
                return ((Identifier) fieldAccess.getTarget()).getSimpleName().equals(className.substring(0, className.lastIndexOf('.')));
            }
            return false;
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final FieldAccess t;

            public CLeftPadded<Identifier> getName() {
                return t.name;
            }

            public FieldAccess withName(CLeftPadded<Identifier> name) {
                return t.name == name ? t : new FieldAccess(t.id, t.prefix, t.markers, t.target, name, t.type);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class ForEachLoop implements C, Loop {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @With
        @Getter
        Control control;

        CRightPadded<Statement> body;

        public Statement getBody() {
            return body.getElement();
        }

        @SuppressWarnings("unchecked")
        public ForEachLoop withBody(Statement body) {
            return getPadding().withBody(this.body.withElement(body));
        }

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitForEachLoop(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        @ToString
        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @RequiredArgsConstructor
        @AllArgsConstructor(access = AccessLevel.PRIVATE)
        public static final class Control implements C {
            @Nullable
            @NonFinal
            transient WeakReference<Padding> padding;

            @With
            @EqualsAndHashCode.Include
            @Getter
            UUID id;

            @With
            @Getter
            Space prefix;

            @With
            @Getter
            Markers markers;

            CRightPadded<VariableDeclarations> variable;

            public VariableDeclarations getVariable() {
                return variable.getElement();
            }

            public Control withVariable(VariableDeclarations variable) {
                return getPadding().withVariable(this.variable.withElement(variable));
            }

            CRightPadded<Expression> iterable;

            public Expression getIterable() {
                return iterable.getElement();
            }

            public Control withIterable(Expression iterable) {
                return getPadding().withIterable(this.iterable.withElement(iterable));
            }

            @Override
            public <P> C acceptC(CVisitor<P> v, P p) {
                return v.visitForEachControl(this, p);
            }

            public Padding getPadding() {
                Padding p;
                if (this.padding == null) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                } else {
                    p = this.padding.get();
                    if (p == null || p.t != this) {
                        p = new Padding(this);
                        this.padding = new WeakReference<>(p);
                    }
                }
                return p;
            }

            @RequiredArgsConstructor
            public static class Padding {
                private final Control t;

                public CRightPadded<VariableDeclarations> getVariable() {
                    return t.variable;
                }

                public Control withVariable(CRightPadded<VariableDeclarations> variable) {
                    return t.variable == variable ? t : new Control(t.id, t.prefix, t.markers, variable, t.iterable);
                }

                public CRightPadded<Expression> getIterable() {
                    return t.iterable;
                }

                public Control withIterable(CRightPadded<Expression> iterable) {
                    return t.iterable == iterable ? t : new Control(t.id, t.prefix, t.markers, t.variable, iterable);
                }
            }
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final ForEachLoop t;

            public CRightPadded<Statement> getBody() {
                return t.body;
            }

            public ForEachLoop withBody(CRightPadded<Statement> body) {
                return t.body == body ? t : new ForEachLoop(t.id, t.prefix, t.markers, t.control, body);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class ForLoop implements C, Loop {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @With
        @Getter
        Control control;

        CRightPadded<Statement> body;

        public Statement getBody() {
            return body.getElement();
        }

        @SuppressWarnings("unchecked")
        public ForLoop withBody(Statement body) {
            return getPadding().withBody(this.body.withElement(body));
        }

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitForLoop(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        @ToString
        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @RequiredArgsConstructor
        @AllArgsConstructor(access = AccessLevel.PRIVATE)
        public static final class Control implements C {
            @Nullable
            @NonFinal
            transient WeakReference<Padding> padding;

            @With
            @EqualsAndHashCode.Include
            @Getter
            UUID id;

            @With
            @Getter
            Space prefix;

            @With
            @Getter
            Markers markers;

            List<CRightPadded<Statement>> init;

            public List<Statement> getInit() {
                return CRightPadded.getElements(init);
            }

            public Control withInit(List<Statement> init) {
                return getPadding().withInit(CRightPadded.withElements(this.init, init));
            }

            CRightPadded<Expression> condition;

            public Expression getCondition() {
                return condition.getElement();
            }

            public Control withCondition(Expression condition) {
                return getPadding().withCondition(this.condition.withElement(condition));
            }

            List<CRightPadded<Statement>> update;

            public List<Statement> getUpdate() {
                return CRightPadded.getElements(update);
            }

            public Control withUpdate(List<Statement> update) {
                return getPadding().withUpdate(CRightPadded.withElements(this.update, update));
            }

            @Override
            public <P> C acceptC(CVisitor<P> v, P p) {
                return v.visitForControl(this, p);
            }

            public Padding getPadding() {
                Padding p;
                if (this.padding == null) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                } else {
                    p = this.padding.get();
                    if (p == null || p.t != this) {
                        p = new Padding(this);
                        this.padding = new WeakReference<>(p);
                    }
                }
                return p;
            }

            @RequiredArgsConstructor
            public static class Padding {
                private final Control t;

                public List<CRightPadded<Statement>> getInit() {
                    return t.init;
                }

                public Control withInit(List<CRightPadded<Statement>> init) {
                    return t.init == init ? t : new Control(t.id, t.prefix, t.markers, init, t.condition, t.update);
                }

                public CRightPadded<Expression> getCondition() {
                    return t.condition;
                }

                public Control withCondition(CRightPadded<Expression> condition) {
                    return t.condition == condition ? t : new Control(t.id, t.prefix, t.markers, t.init, condition, t.update);
                }

                public List<CRightPadded<Statement>> getUpdate() {
                    return t.update;
                }

                public Control withUpdate(List<CRightPadded<Statement>> update) {
                    return t.update == update ? t : new Control(t.id, t.prefix, t.markers, t.init, t.condition, update);
                }
            }
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final ForLoop t;

            public CRightPadded<Statement> getBody() {
                return t.body;
            }

            public ForLoop withBody(CRightPadded<Statement> body) {
                return t.body == body ? t : new ForLoop(t.id, t.prefix, t.markers, t.control, body);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    final class Identifier implements C, TypeTree, Expression {
        private static final CType NONE = new CType() {
            @Override
            public boolean deepEquals(@Nullable CType type) {
                return type == NONE;
            }
        };

        // keyed by name, type, and then field type
        private static final Map<String, Map<CType, Map<CType, IdentifierFlyweight>>> flyweights = new WeakHashMap<>();

        @Getter
        @EqualsAndHashCode.Include
        UUID id;

        @Getter
        Space prefix;

        @Getter
        Markers markers;

        IdentifierFlyweight identifier;

        private Identifier(UUID id, IdentifierFlyweight identifier, Space prefix, Markers markers) {
            this.id = id;
            this.identifier = identifier;
            this.prefix = prefix;
            this.markers = markers;
        }

        @Override
        public CType getType() {
            return identifier.getType();
        }

        @Nullable
        public CType getFieldType() {
            return identifier.getFieldType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Identifier withId(UUID id) {
            if (id == getId()) {
                return this;
            }
            return build(id, prefix, markers, getSimpleName(), getType(), getFieldType());
        }

        @SuppressWarnings("unchecked")
        @Override
        public Identifier withType(@Nullable CType type) {
            if (type == getType()) {
                return this;
            }
            return build(id, prefix, markers, getSimpleName(), type, getFieldType());
        }

        public String getSimpleName() {
            return identifier.getSimpleName();
        }

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitIdentifier(this, p);
        }

        public Identifier withName(String name) {
            if (name.equals(identifier.getSimpleName())) {
                return this;
            }
            return build(id, prefix, markers, name, getType(), getFieldType());
        }

        @SuppressWarnings("unchecked")
        public Identifier withMarkers(Markers markers) {
            if (markers == this.markers) {
                return this;
            }
            return build(id, prefix, markers, identifier.getSimpleName(), getType(), getFieldType());
        }

        @SuppressWarnings("unchecked")
        public Identifier withPrefix(Space prefix) {
            if (prefix == this.prefix) {
                return this;
            }
            return build(id, prefix, markers, identifier.getSimpleName(), getType(), getFieldType());
        }

        public static Identifier build(UUID id,
                                       Space prefix,
                                       Markers markers,
                                       String simpleName,
                                       @Nullable CType type) {
            return build(id, prefix, markers, simpleName, type, null);
        }

        @JsonCreator
        public static Identifier build(UUID id,
                                       Space prefix,
                                       Markers markers,
                                       String simpleName,
                                       @Nullable CType type,
                                       @Nullable CType fieldType) {
            synchronized (flyweights) {
                return new Identifier(
                        id,
                        flyweights
                                .computeIfAbsent(simpleName, n -> new HashMap<>())
                                .computeIfAbsent(type, t -> new HashMap<>())
                                .computeIfAbsent(fieldType, t -> new IdentifierFlyweight(simpleName, type, fieldType)),
                        prefix,
                        markers
                );
            }
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @Data
        public static final class IdentifierFlyweight implements Serializable {
            String simpleName;

            @Nullable
            CType type;

            @Nullable
            CType fieldType;
        }

        /**
         * Making debugging a bit easier
         */
        public String toString() {
            return "Ident(" + getSimpleName() + ")";
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class If implements C, Statement {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @With
        @Getter
        ControlParentheses<Expression> ifCondition;

        CRightPadded<Statement> thenPart;

        public Statement getThenPart() {
            return thenPart.getElement();
        }

        public If withThenPart(Statement thenPart) {
            return getPadding().withThenPart(this.thenPart.withElement(thenPart));
        }

        @With
        @Nullable
        @Getter
        Else elsePart;

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitIf(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        @ToString
        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @RequiredArgsConstructor
        @AllArgsConstructor(access = AccessLevel.PRIVATE)
        public static final class Else implements C {
            @Nullable
            @NonFinal
            transient WeakReference<Padding> padding;

            @With
            @EqualsAndHashCode.Include
            @Getter
            UUID id;

            @With
            @Getter
            Space prefix;

            @With
            @Getter
            Markers markers;

            CRightPadded<Statement> body;

            public Statement getBody() {
                return body.getElement();
            }

            public Else withBody(Statement body) {
                return getPadding().withBody(this.body.withElement(body));
            }

            @Override
            public <P> C acceptC(CVisitor<P> v, P p) {
                return v.visitElse(this, p);
            }

            public Padding getPadding() {
                Padding p;
                if (this.padding == null) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                } else {
                    p = this.padding.get();
                    if (p == null || p.t != this) {
                        p = new Padding(this);
                        this.padding = new WeakReference<>(p);
                    }
                }
                return p;
            }

            @RequiredArgsConstructor
            public static class Padding {
                private final Else t;

                public CRightPadded<Statement> getBody() {
                    return t.body;
                }

                public Else withBody(CRightPadded<Statement> body) {
                    return t.body == body ? t : new Else(t.id, t.prefix, t.markers, body);
                }
            }
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final If t;

            public CRightPadded<Statement> getThenPart() {
                return t.thenPart;
            }

            public If withThenPart(CRightPadded<Statement> thenPart) {
                return t.thenPart == thenPart ? t : new If(t.id, t.prefix, t.markers, t.ifCondition, thenPart, t.elsePart);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class InstanceOf implements C, Expression, TypedTree {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        CRightPadded<Expression> expression;

        public Expression getExpression() {
            return expression.getElement();
        }

        public InstanceOf withExpression(Expression expression) {
            return getPadding().withExpr(this.expression.withElement(expression));
        }

        @With
        @Getter
        C clazz;

        @With
        @Nullable
        @Getter
        CType type;

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitInstanceOf(this, p);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final InstanceOf t;

            public CRightPadded<Expression> getExpr() {
                return t.expression;
            }

            public InstanceOf withExpr(CRightPadded<Expression> expression) {
                return t.expression == expression ? t : new InstanceOf(t.id, t.prefix, t.markers, expression, t.clazz, t.type);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class Label implements C, Statement {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        /**
         * Right padded before the ':'
         */
        CRightPadded<Identifier> label;

        public Identifier getLabel() {
            return label.getElement();
        }

        public Label withLabel(Identifier label) {
            return getPadding().withLabel(this.label.withElement(label));
        }

        @With
        @Getter
        Statement statement;

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitLabel(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final Label t;

            public CRightPadded<Identifier> getLabel() {
                return t.label;
            }

            public Label withLabel(CRightPadded<Identifier> label) {
                return t.label == label ? t : new Label(t.id, t.prefix, t.markers, label, t.statement);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Lambda implements C, Statement, Expression, TypedTree {
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        Parameters parameters;

        @With
        Space arrow;

        @With
        C body;

        @With
        @Nullable
        CType type;

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitLambda(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        @ToString
        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @RequiredArgsConstructor
        @AllArgsConstructor(access = AccessLevel.PRIVATE)
        public static final class Parameters implements C {
            @Nullable
            @NonFinal
            transient WeakReference<Padding> padding;

            @With
            @EqualsAndHashCode.Include
            @Getter
            UUID id;

            @With
            @Getter
            Space prefix;

            @With
            @Getter
            Markers markers;

            @With
            @Getter
            boolean parenthesized;

            List<CRightPadded<C>> parameters;

            public List<C> getParameters() {
                return CRightPadded.getElements(parameters);
            }

            public Parameters withParameters(List<C> parameters) {
                return getPadding().withParams(CRightPadded.withElements(this.parameters, parameters));
            }

            public CoordinateBuilder.Lambda.Parameters getCoordinates() {
                return new CoordinateBuilder.Lambda.Parameters(this);
            }

            public Padding getPadding() {
                Padding p;
                if (this.padding == null) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                } else {
                    p = this.padding.get();
                    if (p == null || p.t != this) {
                        p = new Padding(this);
                        this.padding = new WeakReference<>(p);
                    }
                }
                return p;
            }

            @RequiredArgsConstructor
            public static class Padding {
                private final Parameters t;

                public List<CRightPadded<C>> getParams() {
                    return t.parameters;
                }

                public Parameters withParams(List<CRightPadded<C>> parameters) {
                    return t.parameters == parameters ? t : new Parameters(t.id, t.prefix, t.markers, t.parenthesized, parameters);
                }
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Literal implements C, Expression, TypedTree {
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        @Nullable
        Object value;

        @With
        @Nullable
        String valueSource;

        @With
        @Nullable
        List<UnicodeEscape> unicodeEscapes;

        /**
         * Including String literals
         */
        CType.Primitive type;

        @SuppressWarnings("unchecked")
        @Override
        public Literal withType(@Nullable CType type) {
            if (type == this.type) {
                return this;
            }
            if (type instanceof CType.Primitive) {
                return new Literal(id, prefix, markers, value, valueSource, unicodeEscapes, (CType.Primitive) type);
            }
            return this;
        }

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitLiteral(this, p);
        }

        /**
         * See <a href="https://docs.oracle.com/javase/specs/jls/se7/html/jls-3.html#jls-3.3">jls-3.3</a>.
         * <p>
         * Unmatched UTF-16 surrogate pairs (composed of two escape and code point pairs) are unserializable
         * by technologies like Jackson. So we separate and store the code point off and reconstruct
         * the escape sequence when printing later.
         * <p>
         * We only escape unicode characters that are part of UTF-16 surrogate pairs. Others are generally
         * treated well by tools like Jackson.
         */
        @Value
        public static class UnicodeEscape {
            @With
            int valueSourceIndex;

            @With
            String codePoint;
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class MemberReference implements C, Expression, TypedTree {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        CRightPadded<Expression> containing;

        public Expression getContaining() {
            return containing.getElement();
        }

        public MemberReference withContaining(Expression containing) {
            //noinspection ConstantConditions
            return getPadding().withContaining(CRightPadded.withElement(this.containing, containing));
        }

        @Nullable
        CContainer<Expression> typeParameters;

        @Nullable
        public List<Expression> getTypeParameters() {
            return typeParameters == null ? null : typeParameters.getElements();
        }

        public MemberReference withTypeParameters(@Nullable List<Expression> typeParameters) {
            return getPadding().withTypeParameters(CContainer.withElementsNullable(this.typeParameters, typeParameters));
        }

        CLeftPadded<Identifier> reference;

        public Identifier getReference() {
            return reference.getElement();
        }

        public MemberReference withReference(Identifier reference) {
            return getPadding().withReference(this.reference.withElement(reference));
        }

        /**
         * In the case of a method reference, this will be the type of the functional interface that
         * this method reference is supplying.
         */
        @With
        @Nullable
        @Getter
        CType type;

        /**
         * In the case of a method reference, this is the method type pointed to by {@link #reference}.
         */
        @With
        @Nullable
        @Getter
        CType referenceType;

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitMemberReference(this, p);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final MemberReference t;

            public CRightPadded<Expression> getContaining() {
                return t.containing;
            }

            public MemberReference withContaining(CRightPadded<Expression> containing) {
                return t.containing == containing ? t : new MemberReference(t.id, t.prefix, t.markers, containing, t.typeParameters, t.reference, t.type, t.referenceType);
            }

            @Nullable
            public CContainer<Expression> getTypeParameters() {
                return t.typeParameters;
            }

            public MemberReference withTypeParameters(@Nullable CContainer<Expression> typeParameters) {
                return t.typeParameters == typeParameters ? t : new MemberReference(t.id, t.prefix, t.markers, t.containing, typeParameters, t.reference, t.type, t.referenceType);
            }

            public CLeftPadded<Identifier> getReference() {
                return t.reference;
            }

            public MemberReference withReference(CLeftPadded<Identifier> reference) {
                return t.reference == reference ? t : new MemberReference(t.id, t.prefix, t.markers, t.containing, t.typeParameters, reference, t.type, t.referenceType);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class MethodDeclaration implements C, Statement, TypedTree {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @Nullable
        @NonFinal
        transient WeakReference<Annotations> annotations;

        @With
        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @With
        @Getter
        List<Annotation> leadingAnnotations;

        @With
        @Getter
        List<Modifier> modifiers;

        @Nullable
        TypeParameters typeParameters;

        @Nullable
        public List<TypeParameter> getTypeParameters() {
            return typeParameters == null ? null : typeParameters.getTypeParameters();
        }

        public MethodDeclaration withTypeParameters(@Nullable List<TypeParameter> typeParameters) {
            if (typeParameters == null) {
                if (this.getAnnotations().getTypeParameters() == null) {
                    return this;
                } else {
                    return this.getAnnotations().withTypeParameters(null);
                }
            } else {
                TypeParameters currentTypeParameters = this.getAnnotations().getTypeParameters();
                if (currentTypeParameters == null) {
                    return getAnnotations().withTypeParameters(new TypeParameters(Tree.randomId(), Space.EMPTY, Markers.EMPTY,
                            null, typeParameters.stream().map(CRightPadded::build).collect(toList())));
                } else {
                    return getAnnotations().withTypeParameters(currentTypeParameters.withTypeParameters(typeParameters));
                }
            }
        }

        /**
         * Null for constructor declarations.
         */
        @With
        @Getter
        @Nullable
        TypeTree returnTypeExpression;

        IdentifierWithAnnotations name;

        public Identifier getName() {
            return name.getIdentifier();
        }

        public MethodDeclaration withName(Identifier name) {
            return getAnnotations().withName(this.name.withIdentifier(name));
        }

        CContainer<Statement> parameters;

        public List<Statement> getParameters() {
            return parameters.getElements();
        }

        public MethodDeclaration withParameters(List<Statement> parameters) {
            return getPadding().withParameters(CContainer.withElements(this.parameters, parameters));
        }

        @Nullable
        CContainer<NameTree> throwz;

        @Nullable
        public List<NameTree> getThrows() {
            return throwz == null ? null : throwz.getElements();
        }

        public MethodDeclaration withThrows(@Nullable List<NameTree> throwz) {
            return getPadding().withThrows(CContainer.withElementsNullable(this.throwz, throwz));
        }

        /**
         * Null for abstract method declarations and interface method declarations.
         */
        @With
        @Getter
        @Nullable
        Block body;

        /**
         * For default values on definitions of annotation parameters.
         */
        @Nullable
        CLeftPadded<Expression> defaultValue;

        @Nullable
        public Expression getDefaultValue() {
            return defaultValue == null ? null : defaultValue.getElement();
        }

        public MethodDeclaration withDefaultValue(@Nullable Expression defaultValue) {
            return getPadding().withDefaultValue(CLeftPadded.withElement(this.defaultValue, defaultValue));
        }

        @Getter
        @Nullable
        CType.Method type;

        @SuppressWarnings("unchecked")
        @Override
        public MethodDeclaration withType(@Nullable CType type) {
            if (type == this.type) {
                return this;
            }

            if (!(type instanceof CType.Method)) {
                throw new IllegalArgumentException("A method can only be type attributed with a method type");
            }

            return new MethodDeclaration(id, prefix, markers, leadingAnnotations, modifiers, typeParameters, returnTypeExpression, name, parameters, throwz, body, defaultValue, (CType.Method) type);
        }

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitMethodDeclaration(this, p);
        }

        public boolean isAbstract() {
            return body == null;
        }

        public boolean isConstructor() {
            return getReturnTypeExpression() == null;
        }

        public String getSimpleName() {
            return name.getIdentifier().getSimpleName();
        }

        public boolean hasModifier(Modifier.Type modifier) {
            return Modifier.hasModifier(getModifiers(), modifier);
        }

        @Override
        public CoordinateBuilder.MethodDeclaration getCoordinates() {
            return new CoordinateBuilder.MethodDeclaration(this);
        }

        // gather annotations from everywhere they may occur
        public List<Annotation> getAllAnnotations() {
            List<Annotation> allAnnotations = new ArrayList<>(leadingAnnotations);
            for (Modifier modifier : modifiers) {
                allAnnotations.addAll(modifier.getAnnotations());
            }
            if (typeParameters != null) {
                allAnnotations.addAll(typeParameters.getAnnotations());
            }
            if (returnTypeExpression instanceof AnnotatedType) {
                allAnnotations.addAll(((AnnotatedType) returnTypeExpression).getAnnotations());
            }
            allAnnotations.addAll(name.getAnnotations());
            return allAnnotations;
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class IdentifierWithAnnotations {
            @Getter
            @With
            Identifier identifier;

            @Getter
            @With
            List<Annotation> annotations;
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final MethodDeclaration t;

            public CContainer<Statement> getParameters() {
                return t.parameters;
            }

            public MethodDeclaration withParameters(CContainer<Statement> parameters) {
                return t.parameters == parameters ? t : new MethodDeclaration(t.id, t.prefix, t.markers, t.leadingAnnotations, t.modifiers, t.typeParameters, t.returnTypeExpression, t.name, parameters, t.throwz, t.body, t.defaultValue, t.type);
            }

            @Nullable
            public CContainer<NameTree> getThrows() {
                return t.throwz;
            }

            public MethodDeclaration withThrows(@Nullable CContainer<NameTree> throwz) {
                return t.throwz == throwz ? t : new MethodDeclaration(t.id, t.prefix, t.markers, t.leadingAnnotations, t.modifiers, t.typeParameters, t.returnTypeExpression, t.name, t.parameters, throwz, t.body, t.defaultValue, t.type);
            }

            @Nullable
            public CLeftPadded<Expression> getDefaultValue() {
                return t.defaultValue;
            }

            public MethodDeclaration withDefaultValue(@Nullable CLeftPadded<Expression> defaultValue) {
                return t.defaultValue == defaultValue ? t : new MethodDeclaration(t.id, t.prefix, t.markers, t.leadingAnnotations, t.modifiers, t.typeParameters, t.returnTypeExpression, t.name, t.parameters, t.throwz, t.body, defaultValue, t.type);
            }

            @Nullable
            public TypeParameters getTypeParameters() {
                return t.typeParameters;
            }

            public MethodDeclaration withTypeParameters(@Nullable TypeParameters typeParameters) {
                return t.typeParameters == typeParameters ? t : new MethodDeclaration(t.id, t.prefix, t.markers, t.leadingAnnotations, t.modifiers, typeParameters, t.returnTypeExpression, t.name, t.parameters, t.throwz, t.body, t.defaultValue, t.type);
            }
        }

        public Annotations getAnnotations() {
            Annotations a;
            if (this.annotations == null) {
                a = new Annotations(this);
                this.annotations = new WeakReference<>(a);
            } else {
                a = this.annotations.get();
                if (a == null || a.t != this) {
                    a = new Annotations(this);
                    this.annotations = new WeakReference<>(a);
                }
            }
            return a;
        }

        @RequiredArgsConstructor
        public static class Annotations {
            private final MethodDeclaration t;

            @Nullable
            public TypeParameters getTypeParameters() {
                return t.typeParameters;
            }

            public MethodDeclaration withTypeParameters(@Nullable TypeParameters typeParameters) {
                return t.typeParameters == typeParameters ? t : new MethodDeclaration(t.id, t.prefix, t.markers, t.leadingAnnotations, t.modifiers, typeParameters, t.returnTypeExpression, t.name, t.parameters, t.throwz, t.body, t.defaultValue, t.type);
            }

            public IdentifierWithAnnotations getName() {
                return t.name;
            }

            public MethodDeclaration withName(IdentifierWithAnnotations name) {
                return t.name == name ? t : new MethodDeclaration(t.id, t.prefix, t.markers, t.leadingAnnotations, t.modifiers, t.typeParameters, t.returnTypeExpression, name, t.parameters, t.throwz, t.body, t.defaultValue, t.type);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class MethodInvocation implements C, Statement, Expression, TypedTree {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        /**
         * Right padded before the '.'
         */
        @Nullable
        CRightPadded<Expression> select;

        @Nullable
        public Expression getSelect() {
            return select == null ? null : select.getElement();
        }

        public MethodInvocation withSelect(@Nullable Expression select) {
            return getPadding().withSelect(CRightPadded.withElement(this.select, select));
        }

        @Nullable
        @With
        CContainer<Expression> typeParameters;

        @Nullable
        public List<Expression> getTypeParameters() {
            return typeParameters == null ? null : typeParameters.getElements();
        }

        @Getter
        Identifier name;

        public MethodInvocation withName(Identifier name) {
            if (this.name == name) {
                return this;
            }
            CType.Method newType = null;
            if (this.type != null) {
                newType = this.type.withName(name.getSimpleName());
            }
            return new MethodInvocation(id, prefix, markers, select, typeParameters, name, arguments, newType);
        }

        CContainer<Expression> arguments;

        public List<Expression> getArguments() {
            return arguments.getElements();
        }

        public MethodInvocation withArguments(List<Expression> arguments) {
            if (this.arguments.getElements() == arguments) {
                return this;
            }
            return getPadding().withArguments(CContainer.withElements(this.arguments, arguments));
        }

        @Nullable
        @Getter
        CType.Method type;

        @SuppressWarnings("unchecked")
        @Override
        public MethodInvocation withType(@Nullable CType type) {
            if (type == this.type) {
                return this;
            }
            if (type instanceof CType.Method) {
                return new MethodInvocation(id, prefix, markers, select, typeParameters, name, arguments, (CType.Method) type);
            }
            return this;
        }

        public MethodInvocation withDeclaringType(CType.FullyQualified type) {
            if (this.type == null) {
                return this;
            } else {
                return withType(this.type.withDeclaringType(type));
            }
        }

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitMethodInvocation(this, p);
        }

        @Override
        public CoordinateBuilder.MethodInvocation getCoordinates() {
            return new CoordinateBuilder.MethodInvocation(this);
        }

        @Nullable
        public CType getReturnType() {
            return type == null ? null : type.getResolvedSignature() == null ? null :
                    type.getResolvedSignature().getReturnType();
        }

        public String getSimpleName() {
            return name.getSimpleName();
        }

        @Override
        public List<C> getSideEffects() {
            return singletonList(this);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final MethodInvocation t;

            @Nullable
            public CRightPadded<Expression> getSelect() {
                return t.select;
            }

            public MethodInvocation withSelect(@Nullable CRightPadded<Expression> select) {
                return t.select == select ? t : new MethodInvocation(t.id, t.prefix, t.markers, select, t.typeParameters, t.name, t.arguments, t.type);
            }

            @Nullable
            public CContainer<Expression> getTypeParameters() {
                return t.typeParameters;
            }

            public MethodInvocation withTypeParameters(@Nullable CContainer<Expression> typeParameters) {
                return t.typeParameters == typeParameters ? t : new MethodInvocation(t.id, t.prefix, t.markers, t.select, typeParameters, t.name, t.arguments, t.type);
            }

            public CContainer<Expression> getArguments() {
                return t.arguments;
            }

            public MethodInvocation withArguments(CContainer<Expression> arguments) {
                return t.arguments == arguments ? t : new MethodInvocation(t.id, t.prefix, t.markers, t.select, t.typeParameters, t.name, arguments, t.type);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Modifier implements C {
        public static boolean hasModifier(Collection<Modifier> modifiers, Type modifier) {
            return modifiers.stream().anyMatch(m -> m.getType() == modifier);
        }

        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        Type type;

        @With
        @Getter
        List<Annotation> annotations;

        /**
         * These types are sorted in order of their recommended appearance in a list of modifiers, as defined in the
         * <a href="https://rules.sonarsource.com/java/tag/convention/RSPEC-1124">JLS</a>.
         */
        public enum Type {
            Default,
            Public,
            Protected,
            Private,
            Abstract,
            Static,
            Final,
            Transient,
            Volatile,
            Synchronized,
            Native,
            Strictfp,
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class MultiCatch implements C, TypeTree {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        List<CRightPadded<NameTree>> alternatives;

        public List<NameTree> getAlternatives() {
            return CRightPadded.getElements(alternatives);
        }

        public MultiCatch withAlternatives(List<NameTree> alternatives) {
            return getPadding().withAlternatives(CRightPadded.withElements(this.alternatives, alternatives));
        }

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitMultiCatch(this, p);
        }

        @SuppressWarnings("unchecked")
        @Override
        public MultiCatch withType(@Nullable CType type) {
            // cannot overwrite type directly, perform this operation on each alternative separately
            return this;
        }

        @Override
        public CType getType() {
            return new CType.MultiCatch(alternatives.stream()
                    .filter(Objects::nonNull)
                    .map(alt -> alt.getElement().getType())
                    .collect(toList()));
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final MultiCatch t;

            public List<CRightPadded<NameTree>> getAlternatives() {
                return t.alternatives;
            }

            public MultiCatch withAlternatives(List<CRightPadded<NameTree>> alternatives) {
                return t.alternatives == alternatives ? t : new MultiCatch(t.id, t.prefix, t.markers, alternatives);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class NewArray implements C, Expression, TypedTree {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @With
        @Nullable
        @Getter
        TypeTree typeExpression;

        @With
        @Getter
        List<ArrayDimension> dimensions;

        @Nullable
        CContainer<Expression> initializer;

        @Nullable
        public List<Expression> getInitializer() {
            return initializer == null ? null : initializer.getElements();
        }

        @With
        @Nullable
        @Getter
        CType type;

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitNewArray(this, p);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final NewArray t;

            @Nullable
            public CContainer<Expression> getInitializer() {
                return t.initializer;
            }

            public NewArray withInitializer(@Nullable CContainer<Expression> initializer) {
                return t.initializer == initializer ? t : new NewArray(t.id, t.prefix, t.markers, t.typeExpression, t.dimensions, initializer, t.type);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class ArrayDimension implements C {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        CRightPadded<Expression> index;

        public Expression getIndex() {
            return index.getElement();
        }

        public ArrayDimension withIndex(Expression index) {
            return getPadding().withIndex(this.index.withElement(index));
        }

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitArrayDimension(this, p);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final ArrayDimension t;

            public CRightPadded<Expression> getIndex() {
                return t.index;
            }

            public ArrayDimension withIndex(CRightPadded<Expression> index) {
                return t.index == index ? t : new ArrayDimension(t.id, t.prefix, t.markers, index);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class NewClass implements C, Statement, Expression, TypedTree {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @Getter
        @EqualsAndHashCode.Include
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        /**
         * For situations like <code>this.new A()</code>.
         * Right padded before the '.'
         */
        @Nullable
        CRightPadded<Expression> enclosing;

        @Nullable
        public Expression getEnclosing() {
            return enclosing == null ? null : enclosing.getElement();
        }

        public NewClass withEnclosing(Expression enclosing) {
            return getPadding().withEnclosing(CRightPadded.withElement(this.enclosing, enclosing));
        }

        Space nooh;

        public Space getNew() {
            return nooh;
        }

        public NewClass withNew(Space nooh) {
            if (nooh == this.nooh) {
                return this;
            }
            return new NewClass(id, prefix, markers, enclosing, nooh, clazz, arguments, body, constructorType, type);
        }

        @Nullable
        @With
        @Getter
        TypeTree clazz;

        @Nullable
        CContainer<Expression> arguments;

        @Nullable
        public List<Expression> getArguments() {
            return arguments == null ? null : arguments.getElements();
        }

        public NewClass withArguments(@Nullable List<Expression> arguments) {
            return getPadding().withArguments(CContainer.withElementsNullable(this.arguments, arguments));
        }

        @With
        @Nullable
        @Getter
        Block body;

        @With
        @Nullable
        @Getter
        CType.Method constructorType;

        @With
        @Nullable
        @Getter
        CType type;

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitNewClass(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        @Override
        public List<C> getSideEffects() {
            return singletonList(this);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final NewClass t;

            @Nullable
            public CRightPadded<Expression> getEnclosing() {
                return t.enclosing;
            }

            public NewClass withEnclosing(@Nullable CRightPadded<Expression> enclosing) {
                return t.enclosing == enclosing ? t : new NewClass(t.id, t.prefix, t.markers, enclosing, t.nooh, t.clazz, t.arguments, t.body, t.constructorType, t.type);
            }

            @Nullable
            public CContainer<Expression> getArguments() {
                return t.arguments;
            }

            public NewClass withArguments(@Nullable CContainer<Expression> arguments) {
                return t.arguments == arguments ? t : new NewClass(t.id, t.prefix, t.markers, t.enclosing, t.nooh, t.clazz, arguments, t.body, t.constructorType, t.type);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class ParameterizedType implements C, TypeTree, Expression {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @With
        @Getter
        NameTree clazz;

        @Nullable
        CContainer<Expression> typeParameters;

        @Nullable
        public List<Expression> getTypeParameters() {
            return typeParameters == null ? null : typeParameters.getElements();
        }

        public ParameterizedType withTypeParameters(@Nullable List<Expression> typeParameters) {
            return getPadding().withTypeParameters(CContainer.withElementsNullable(this.typeParameters, typeParameters));
        }

        @Override
        public CType getType() {
            return clazz.getType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public ParameterizedType withType(@Nullable CType type) {
            if (type == clazz.getType()) {
                return this;
            }
            return withClazz(clazz.withType(type));
        }

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitParameterizedType(this, p);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final ParameterizedType t;

            @Nullable
            public CContainer<Expression> getTypeParameters() {
                return t.typeParameters;
            }

            public ParameterizedType withTypeParameters(@Nullable CContainer<Expression> typeParameters) {
                return t.typeParameters == typeParameters ? t : new ParameterizedType(t.id, t.prefix, t.markers, t.clazz, typeParameters);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Parentheses<J2 extends C> implements C, Expression {
        @Nullable
        @NonFinal
        transient WeakReference<Padding<J2>> padding;

        @With
        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        CRightPadded<J2> tree;

        public J2 getTree() {
            return tree.getElement();
        }

        public Parentheses<J2> withTree(J2 tree) {
            return getPadding().withTree(this.tree.withElement(tree));
        }

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitParentheses(this, p);
        }

        @Override
        public List<C> getSideEffects() {
            return tree.getElement() instanceof Expression ? ((Expression) tree.getElement()).getSideEffects() : emptyList();
        }

        @Override
        public CType getType() {
            return tree instanceof Expression ? ((Expression) tree).getType() :
                    tree instanceof NameTree ? ((NameTree) tree).getType() :
                            null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Parentheses<J2> withType(@Nullable CType type) {
            return tree instanceof Expression ? ((Expression) tree).withType(type) :
                    tree instanceof NameTree ? ((NameTree) tree).withType(type) :
                            this;
        }

        public Padding<J2> getPadding() {
            Padding<J2> p;
            if (this.padding == null) {
                p = new Padding<>(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding<>(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding<J2 extends C> {
            private final Parentheses<J2> t;

            public CRightPadded<J2> getTree() {
                return t.tree;
            }

            public Parentheses<J2> withTree(CRightPadded<J2> tree) {
                return t.tree == tree ? t : new Parentheses<>(t.id, t.prefix, t.markers, tree);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class ControlParentheses<J2 extends C> implements C, Expression {
        @Nullable
        @NonFinal
        transient WeakReference<Padding<J2>> padding;

        @With
        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        CRightPadded<J2> tree;

        public J2 getTree() {
            return tree.getElement();
        }

        public ControlParentheses<J2> withTree(J2 tree) {
            return getPadding().withTree(this.tree.withElement(tree));
        }

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitControlParentheses(this, p);
        }

        @Override
        public List<C> getSideEffects() {
            return tree instanceof Expression ? ((Expression) tree).getSideEffects() : emptyList();
        }

        @Override
        public CType getType() {
            J2 element = tree.getElement();
            if (element instanceof Expression) {
                return ((Expression) element).getType();
            }
            if (element instanceof NameTree) {
                return ((NameTree) element).getType();
            }
            if (element instanceof VariableDeclarations) {
                return ((VariableDeclarations) element).getType();
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public ControlParentheses<J2> withType(@Nullable CType type) {
            return tree instanceof Expression ? ((Expression) tree).withType(type) :
                    tree instanceof NameTree ? ((NameTree) tree).withType(type) :
                            this;
        }

        public Padding<J2> getPadding() {
            Padding<J2> p;
            if (this.padding == null) {
                p = new Padding<>(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding<>(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding<J2 extends C> {
            private final ControlParentheses<J2> t;

            public CRightPadded<J2> getTree() {
                return t.tree;
            }

            public ControlParentheses<J2> withTree(CRightPadded<J2> tree) {
                return t.tree == tree ? t : new ControlParentheses<>(t.id, t.prefix, t.markers, tree);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @AllArgsConstructor
    final class Primitive implements C, TypeTree, Expression {
        @With
        @Getter
        @EqualsAndHashCode.Include
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        CType.Primitive type;

        @SuppressWarnings("unchecked")
        @Override
        public Primitive withType(@Nullable CType type) {
            if (type == this.type) {
                return this;
            }
            if (!(type instanceof CType.Primitive)) {
                throw new IllegalArgumentException("Cannot apply a non-primitive type to Primitive");
            }
            return new Primitive(id, prefix, markers, (CType.Primitive) type);
        }

        @Override
        @NonNull
        public CType.Primitive getType() {
            return type;
        }

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitPrimitive(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Return implements C, Statement {
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        @Nullable
        Expression expression;

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitReturn(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Switch implements C, Statement {
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        ControlParentheses<Expression> selector;

        @With
        Block cases;

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitSwitch(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Synchronized implements C, Statement {
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        ControlParentheses<Expression> lock;

        @With
        Block body;

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitSynchronized(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class Ternary implements C, Expression, TypedTree {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @With
        @Getter
        Expression condition;

        CLeftPadded<Expression> truePart;

        public Expression getTruePart() {
            return truePart.getElement();
        }

        public Ternary withTruePart(Expression truePart) {
            return getPadding().withTruePart(this.truePart.withElement(truePart));
        }

        CLeftPadded<Expression> falsePart;

        public Expression getFalsePart() {
            return falsePart.getElement();
        }

        public Ternary withFalsePart(Expression falsePart) {
            return getPadding().withFalsePart(this.falsePart.withElement(falsePart));
        }

        @With
        @Nullable
        @Getter
        CType type;

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitTernary(this, p);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final Ternary t;

            public CLeftPadded<Expression> getTruePart() {
                return t.truePart;
            }

            public Ternary withTruePart(CLeftPadded<Expression> truePart) {
                return t.truePart == truePart ? t : new Ternary(t.id, t.prefix, t.markers, t.condition, truePart, t.falsePart, t.type);
            }

            public CLeftPadded<Expression> getFalsePart() {
                return t.falsePart;
            }

            public Ternary withFalsePart(CLeftPadded<Expression> falsePart) {
                return t.falsePart == falsePart ? t : new Ternary(t.id, t.prefix, t.markers, t.condition, t.truePart, falsePart, t.type);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Throw implements C, Statement {
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        Expression exception;

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitThrow(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class Try implements C, Statement {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @Getter
        @EqualsAndHashCode.Include
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @Nullable
        CContainer<Resource> resources;

        @Nullable
        public List<Resource> getResources() {
            return resources == null ? null : resources.getElements();
        }

        public Try withResources(@Nullable List<Resource> resources) {
            return getPadding().withResources(CContainer.withElementsNullable(this.resources, resources));
        }

        @With
        @Getter
        Block body;

        @With
        @Getter
        List<Catch> catches;

        @Nullable
        CLeftPadded<Block> finallie;

        @Nullable
        public Block getFinally() {
            return finallie == null ? null : finallie.getElement();
        }

        public Try withFinally(@Nullable Block finallie) {
            return getPadding().withFinally(CLeftPadded.withElement(this.finallie, finallie));
        }

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitTry(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class Resource implements C {
            @With
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Space prefix;

            @With
            Markers markers;

            @With
            TypedTree variableDeclarations;

            /**
             * Only honored on the last resource in a collection of resources.
             */
            @With
            boolean terminatedWithSemicolon;

            @Override
            public <P> C acceptC(CVisitor<P> v, P p) {
                return v.visitTryResource(this, p);
            }
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static final class Catch implements C {
            @With
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Space prefix;

            @With
            Markers markers;

            @With
            ControlParentheses<VariableDeclarations> parameter;

            @With
            Block body;

            @Override
            public <P> C acceptC(CVisitor<P> v, P p) {
                return v.visitCatch(this, p);
            }
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final Try t;

            @Nullable
            public CContainer<Resource> getResources() {
                return t.resources;
            }

            public Try withResources(@Nullable CContainer<Resource> resources) {
                return t.resources == resources ? t : new Try(t.id, t.prefix, t.markers, resources, t.body, t.catches, t.finallie);
            }

            @Nullable
            public CLeftPadded<Block> getFinally() {
                return t.finallie;
            }

            public Try withFinally(@Nullable CLeftPadded<Block> finallie) {
                return t.finallie == finallie ? t : new Try(t.id, t.prefix, t.markers, t.resources, t.body, t.catches, finallie);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class TypeCast implements C, Expression {
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        ControlParentheses<TypeTree> clazz;

        @With
        Expression expression;

        @Override
        public CType getType() {
            return clazz.getType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public TypeCast withType(@Nullable CType type) {
            return withClazz(clazz.withType(type));
        }

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitTypeCast(this, p);
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class TypeParameter implements C {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @With
        @Getter
        List<Annotation> annotations;

        /**
         * Will be either a {@link TypeTree} or {@link Wildcard}. Wildcards aren't possible in
         * every context where type parameters may be defined (e.g. not possible on new statements).
         */
        @With
        @Getter
        Expression name;

        @Nullable
        CContainer<TypeTree> bounds;

        @Nullable
        public List<TypeTree> getBounds() {
            return bounds == null ? null : bounds.getElements();
        }

        public TypeParameter withBounds(@Nullable List<TypeTree> bounds) {
            return getPadding().withBounds(CContainer.withElementsNullable(this.bounds, bounds));
        }

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitTypeParameter(this, p);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final TypeParameter t;

            @Nullable
            public CContainer<TypeTree> getBounds() {
                return t.bounds;
            }

            public TypeParameter withBounds(@Nullable CContainer<TypeTree> bounds) {
                return t.bounds == bounds ? t : new TypeParameter(t.id, t.prefix, t.markers, t.annotations, t.name, bounds);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class TypeParameters implements C {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @With
        @Getter
        List<Annotation> annotations;

        List<CRightPadded<TypeParameter>> typeParameters;

        public List<TypeParameter> getTypeParameters() {
            return CRightPadded.getElements(typeParameters);
        }

        public TypeParameters withTypeParameters(List<TypeParameter> typeParameters) {
            return getPadding().withTypeParameters(CRightPadded.withElements(this.typeParameters, typeParameters));
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final TypeParameters t;

            public List<CRightPadded<TypeParameter>> getTypeParameters() {
                return t.typeParameters;
            }

            public TypeParameters withTypeParameters(List<CRightPadded<TypeParameter>> typeParameters) {
                return t.typeParameters == typeParameters ? t : new TypeParameters(t.id, t.prefix, t.markers, t.annotations, typeParameters);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class Unary implements C, Statement, Expression, TypedTree {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        CLeftPadded<Type> operator;

        public Type getOperator() {
            return operator.getElement();
        }

        public Unary withOperator(Type operator) {
            return getPadding().withOperator(this.operator.withElement(operator));
        }

        @With
        @Getter
        Expression expression;

        @With
        @Nullable
        @Getter
        CType type;

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitUnary(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        @Override
        public List<C> getSideEffects() {
            return expression.getSideEffects();
        }

        public enum Type {
            PreIncrement,
            PreDecrement,
            PostIncrement,
            PostDecrement,
            Positive,
            Negative,
            Complement,
            Not
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final Unary t;

            public CLeftPadded<Type> getOperator() {
                return t.operator;
            }

            public Unary withOperator(CLeftPadded<Type> operator) {
                return t.operator == operator ? t : new Unary(t.id, t.prefix, t.markers, operator, t.expression, t.type);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class VarType implements C, Expression, TypeTree {
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        CType type;

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitVarType(this, p);
        }

    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class VariableDeclarations implements C, Statement, TypedTree {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @With
        @Getter
        List<Annotation> leadingAnnotations;

        @With
        @Getter
        List<Modifier> modifiers;

        @With
        @Nullable
        @Getter
        TypeTree typeExpression;

        @With
        @Nullable
        @Getter
        Space varargs;

        @With
        @Getter
        List<CLeftPadded<Space>> dimensionsBeforeName;

        List<CRightPadded<NamedVariable>> variables;

        public List<NamedVariable> getVariables() {
            return CRightPadded.getElements(variables);
        }

        public VariableDeclarations withVariables(List<NamedVariable> vars) {
            return getPadding().withVariables(CRightPadded.withElements(this.variables, vars));
        }

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitVariableDeclarations(this, p);
        }

        @Override
        public CoordinateBuilder.VariableDeclarations getCoordinates() {
            return new CoordinateBuilder.VariableDeclarations(this);
        }

        // gather annotations from everywhere they may occur
        public List<Annotation> getAllAnnotations() {
            List<Annotation> allAnnotations = new ArrayList<>(leadingAnnotations);
            for (Modifier modifier : modifiers) {
                allAnnotations.addAll(modifier.getAnnotations());
            }
            if (typeExpression != null && typeExpression instanceof AnnotatedType) {
                allAnnotations.addAll(((AnnotatedType) typeExpression).getAnnotations());
            }
            return allAnnotations;
        }

        @Nullable
        public CType.FullyQualified getTypeAsFullyQualified() {
            return typeExpression == null ? null : TypeUtils.asFullyQualified(typeExpression.getType());
        }

        @Nullable
        @Override
        public CType getType() {
            return typeExpression == null ? null : typeExpression.getType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public VariableDeclarations withType(@Nullable CType type) {
            return typeExpression == null ? this :
                    withTypeExpression(typeExpression.withType(type));
        }

        @ToString
        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @RequiredArgsConstructor
        @AllArgsConstructor(access = AccessLevel.PRIVATE)
        public static final class NamedVariable implements C, NameTree {
            @Nullable
            @NonFinal
            transient WeakReference<Padding> padding;

            @With
            @EqualsAndHashCode.Include
            @Getter
            UUID id;

            @With
            @Getter
            Space prefix;

            @With
            @Getter
            Markers markers;

            @With
            @Getter
            Identifier name;

            @With
            @Getter
            List<CLeftPadded<Space>> dimensionsAfterName;

            @Nullable
            CLeftPadded<Expression> initializer;

            @Nullable
            public Expression getInitializer() {
                return initializer == null ? null : initializer.getElement();
            }

            public NamedVariable withInitializer(@Nullable Expression initializer) {
                return getPadding().withInitializer(CLeftPadded.withElement(this.initializer, initializer));
            }

            @With
            @Nullable
            @Getter
            CType type;

            public String getSimpleName() {
                return name.getSimpleName();
            }

            @Override
            public <P> C acceptC(CVisitor<P> v, P p) {
                return v.visitVariable(this, p);
            }

            public boolean isField(Cursor cursor) {
                return cursor
                        .getParentOrThrow() // CRightPadded
                        .getParentOrThrow() // J.VariableDeclarations
                        .getParentOrThrow() // CRightPadded
                        .getParentOrThrow() // J.Block
                        .getParentOrThrow() // maybe J.ClassDeclaration
                        .getValue() instanceof ClassDeclaration;
            }

            public Padding getPadding() {
                Padding p;
                if (this.padding == null) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                } else {
                    p = this.padding.get();
                    if (p == null || p.t != this) {
                        p = new Padding(this);
                        this.padding = new WeakReference<>(p);
                    }
                }
                return p;
            }

            @RequiredArgsConstructor
            public static class Padding {
                private final NamedVariable t;

                @Nullable
                public CLeftPadded<Expression> getInitializer() {
                    return t.initializer;
                }

                public NamedVariable withInitializer(@Nullable CLeftPadded<Expression> initializer) {
                    return t.initializer == initializer ? t : new NamedVariable(t.id, t.prefix, t.markers, t.name, t.dimensionsAfterName, initializer, t.type);
                }
            }
        }

        public boolean hasModifier(Modifier.Type modifier) {
            return Modifier.hasModifier(getModifiers(), modifier);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final VariableDeclarations t;

            public List<CRightPadded<NamedVariable>> getVariables() {
                return t.variables;
            }

            public VariableDeclarations withVariables(List<CRightPadded<NamedVariable>> variables) {
                return t.variables == variables ? t : new VariableDeclarations(t.id, t.prefix, t.markers, t.leadingAnnotations, t.modifiers, t.typeExpression, t.varargs, t.dimensionsBeforeName, variables);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class WhileLoop implements C, Loop {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @With
        @Getter
        ControlParentheses<Expression> condition;

        CRightPadded<Statement> body;

        public Statement getBody() {
            return body.getElement();
        }

        @SuppressWarnings("unchecked")
        public WhileLoop withBody(Statement body) {
            return getPadding().withBody(this.body.withElement(body));
        }

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitWhileLoop(this, p);
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final WhileLoop t;

            public CRightPadded<Statement> getBody() {
                return t.body;
            }

            public WhileLoop withBody(CRightPadded<Statement> body) {
                return t.body == body ? t : new WhileLoop(t.id, t.prefix, t.markers, t.condition, body);
            }
        }
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class Wildcard implements C, Expression, TypeTree {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @Nullable
        CLeftPadded<Bound> bound;

        @Nullable
        public Bound getBound() {
            return bound == null ? null : bound.getElement();
        }

        public Wildcard withBound(@Nullable Bound bound) {
            return getPadding().withBound(CLeftPadded.withElement(this.bound, bound));
        }

        @With
        @Nullable
        @Getter
        NameTree boundedType;

        @Override
        public CType getType() {
            return null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Wildcard withType(@Nullable CType type) {
            return this;
        }

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visitWildcard(this, p);
        }

        public enum Bound {
            Extends,
            Super
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final Wildcard t;

            @Nullable
            public CLeftPadded<Bound> getBound() {
                return t.bound;
            }

            public Wildcard withBound(@Nullable CLeftPadded<Bound> bound) {
                return t.bound == bound ? t : new Wildcard(t.id, t.prefix, t.markers, bound, t.boundedType);
            }
        }
    }
}
