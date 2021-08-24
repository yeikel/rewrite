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

import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.family.c.tree.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class CVisitor<P> extends TreeVisitor<C, P> {
    /**
     * This method will add an import to the compilation unit if there is a reference to the type. It adds an additional
     * visitor which means the "add import" is deferred and does not complete immediately. This operation is idempotent
     * and calling this method multiple times with the same arguments will only add an import once.
     * and calling this method multiple times with the same arguments will only add an import once.
     *
     * @param clazz The class that will be imported into the compilation unit.
     */
    public final void maybeAddImport(@Nullable CType.FullyQualified clazz) {
        if (clazz != null) {
            maybeAddImport(clazz.getFullyQualifiedName());
        }
    }

    public final <J2 extends C> J2 maybeAutoFormat(J2 before, J2 after, P p) {
        return maybeAutoFormat(before, after, p, getCursor());
    }

    public final <J2 extends C> J2 maybeAutoFormat(J2 before, J2 after, P p, Cursor cursor) {
        return maybeAutoFormat(before, after, null, p, cursor);
    }

    public final <J2 extends C> J2 maybeAutoFormat(J2 before, J2 after, @Nullable C stopAfter, P p, Cursor cursor) {
        if (before != after) {
            return autoFormat(after, stopAfter, p, cursor);
        }
        return after;
    }

    public final <J2 extends C> J2 autoFormat(J2 j, P p) {
        return autoFormat(j, p, getCursor());
    }

    public final <J2 extends C> J2 autoFormat(J2 j, P p, Cursor cursor) {
        return autoFormat(j, null, p, cursor);
    }

    @SuppressWarnings("unchecked")
    public final <C2 extends C> C2 autoFormat(C2 j, @Nullable C stopAfter, P p, Cursor cursor) {
        return (C2) j.formatter(stopAfter, cursor).visitNonNull(j, p, cursor);
    }

    /**
     * This method will add an import to the compilation unit if there is a reference to the type. It adds an additional
     * visitor which means the "add import" is deferred and does not complete immediately. This operation is idempotent
     * and calling this method multiple times with the same arguments will only add an import once.
     *
     * @param fullyQualifiedName Fully-qualified name of the class.
     */
    public final void maybeAddImport(String fullyQualifiedName) {
        TreeVisitor<C, P> op = getCursor().firstEnclosingOrThrow(CSourceFile.class)
                .maybeAddImport(fullyQualifiedName);
        if (!getAfterVisit().contains(op)) {
            doAfterVisit(op);
        }
    }

    public final void maybeRemoveImport(@Nullable CType.FullyQualified clazz) {
        if (clazz != null) {
            maybeRemoveImport(clazz.getFullyQualifiedName());
        }
    }

    public final void maybeRemoveImport(String fullyQualifiedName) {
        TreeVisitor<C, P> op = getCursor().firstEnclosingOrThrow(CSourceFile.class)
                .maybeRemoveImport(fullyQualifiedName);
        if (!getAfterVisit().contains(op)) {
            doAfterVisit(op);
        }
    }

    public C visitExpression(Expression expression, P p) {
        return expression;
    }

    public C visitStatement(Statement statement, P p) {
        return statement;
    }

    @SuppressWarnings("unused")
    public Space visitSpace(Space space, Space.Location loc, P p) {
        return space;
    }

    public <N extends NameTree> N visitTypeName(N nameTree, P p) {
        return nameTree;
    }

    @Nullable
    private <N extends NameTree> CLeftPadded<N> visitTypeName(@Nullable CLeftPadded<N> nameTree, P p) {
        return nameTree == null ? null : nameTree.withElement(visitTypeName(nameTree.getElement(), p));
    }

    @Nullable
    private <N extends NameTree> CRightPadded<N> visitTypeName(@Nullable CRightPadded<N> nameTree, P p) {
        return nameTree == null ? null : nameTree.withElement(visitTypeName(nameTree.getElement(), p));
    }

    @Nullable
    private <J2 extends C> CContainer<J2> visitTypeNames(@Nullable CContainer<J2> nameTrees, P p) {
        if (nameTrees == null) {
            return null;
        }
        @SuppressWarnings("unchecked") List<CRightPadded<J2>> js = ListUtils.map(nameTrees.getPadding().getElements(),
                t -> t.getElement() instanceof NameTree ? (CRightPadded<J2>) visitTypeName((CRightPadded<NameTree>) t, p) : t);
        return js == nameTrees.getPadding().getElements() ? nameTrees : CContainer.build(nameTrees.getBefore(), js, Markers.EMPTY);
    }

    public C visitAnnotatedType(C.AnnotatedType annotatedType, P p) {
        C.AnnotatedType a = annotatedType;
        a = a.withPrefix(visitSpace(a.getPrefix(), Space.Location.ANNOTATED_TYPE_PREFIX, p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        Expression temp = (Expression) visitExpression(a, p);
        if (!(temp instanceof C.AnnotatedType)) {
            return temp;
        } else {
            a = (C.AnnotatedType) temp;
        }
        a = a.withAnnotations(ListUtils.map(a.getAnnotations(), e -> visitAndCast(e, p)));
        a = a.withTypeExpression(visitAndCast(a.getTypeExpression(), p));
        a = a.withTypeExpression(visitTypeName(a.getTypeExpression(), p));
        return a;
    }

    public C visitAnnotation(C.Annotation annotation, P p) {
        C.Annotation a = annotation;
        a = a.withPrefix(visitSpace(a.getPrefix(), Space.Location.ANNOTATION_PREFIX, p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        Expression temp = (Expression) visitExpression(a, p);
        if (!(temp instanceof C.Annotation)) {
            return temp;
        } else {
            a = (C.Annotation) temp;
        }
        if (a.getPadding().getArguments() != null) {
            a = a.getPadding().withArguments(visitContainer(a.getPadding().getArguments(), CContainer.Location.ANNOTATION_ARGUMENTS, p));
        }
        a = a.withAnnotationType(visitAndCast(a.getAnnotationType(), p));
        a = a.withAnnotationType(visitTypeName(a.getAnnotationType(), p));
        return a;
    }

    public C visitArrayAccess(C.ArrayAccess arrayAccess, P p) {
        C.ArrayAccess a = arrayAccess;
        a = a.withPrefix(visitSpace(a.getPrefix(), Space.Location.ARRAY_ACCESS_PREFIX, p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        Expression temp = (Expression) visitExpression(a, p);
        if (!(temp instanceof C.ArrayAccess)) {
            return temp;
        } else {
            a = (C.ArrayAccess) temp;
        }
        a = a.withIndexed(visitAndCast(a.getIndexed(), p));
        a = a.withDimension(visitAndCast(a.getDimension(), p));
        return a;
    }

    public C visitArrayDimension(C.ArrayDimension arrayDimension, P p) {
        C.ArrayDimension a = arrayDimension;
        a = a.withPrefix(visitSpace(a.getPrefix(), Space.Location.DIMENSION_PREFIX, p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        a = a.getPadding().withIndex(visitRightPadded(a.getPadding().getIndex(), CRightPadded.Location.ARRAY_INDEX, p));
        return a;
    }

    public C visitArrayType(C.ArrayType arrayType, P p) {
        C.ArrayType a = arrayType;
        a = a.withPrefix(visitSpace(a.getPrefix(), Space.Location.ARRAY_TYPE_PREFIX, p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        Expression temp = (Expression) visitExpression(a, p);
        if (!(temp instanceof C.ArrayType)) {
            return temp;
        } else {
            a = (C.ArrayType) temp;
        }
        a = a.withElementType(visitAndCast(a.getElementType(), p));
        a = a.withElementType(visitTypeName(a.getElementType(), p));
        a = a.withDimensions(
                ListUtils.map(a.getDimensions(), dim ->
                        visitRightPadded(dim.withElement(
                                visitSpace(dim.getElement(), Space.Location.DIMENSION, p)
                        ), CRightPadded.Location.DIMENSION, p)
                )
        );
        return a;
    }

    public C visitAssert(C.Assert azzert, P p) {
        C.Assert a = azzert;
        a = a.withPrefix(visitSpace(a.getPrefix(), Space.Location.ASSERT_PREFIX, p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        Statement temp = (Statement) visitStatement(a, p);
        if (!(temp instanceof C.Assert)) {
            return temp;
        } else {
            a = (C.Assert) temp;
        }
        a = a.withCondition(visitAndCast(a.getCondition(), p));
        if(a.getDetail() != null) {
            a = a.withDetail(visitLeftPadded(a.getDetail(), CLeftPadded.Location.ASSERT_DETAIL, p));
        }
        return a;
    }

    public C visitAssignment(C.Assignment assignment, P p) {
        C.Assignment a = assignment;
        a = a.withPrefix(visitSpace(a.getPrefix(), Space.Location.ASSIGNMENT_PREFIX, p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        Statement temp = (Statement) visitStatement(a, p);
        if (!(temp instanceof C.Assignment)) {
            return temp;
        } else {
            a = (C.Assignment) temp;
        }
        Expression temp2 = (Expression) visitExpression(a, p);
        if (!(temp2 instanceof C.Assignment)) {
            return temp2;
        } else {
            a = (C.Assignment) temp2;
        }
        a = a.withVariable(visitAndCast(a.getVariable(), p));
        a = a.getPadding().withAssignment(visitLeftPadded(a.getPadding().getAssignment(), CLeftPadded.Location.ASSIGNMENT, p));
        return a;
    }

    public C visitAssignmentOperation(C.AssignmentOperation assignOp, P p) {
        C.AssignmentOperation a = assignOp;
        a = a.withPrefix(visitSpace(a.getPrefix(), Space.Location.ASSIGNMENT_OPERATION_PREFIX, p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        Statement temp = (Statement) visitStatement(a, p);
        if (!(temp instanceof C.AssignmentOperation)) {
            return temp;
        } else {
            a = (C.AssignmentOperation) temp;
        }
        Expression temp2 = (Expression) visitExpression(a, p);
        if (!(temp2 instanceof C.AssignmentOperation)) {
            return temp2;
        } else {
            a = (C.AssignmentOperation) temp2;
        }
        a = a.withVariable(visitAndCast(a.getVariable(), p));
        a = a.getPadding().withOperator(visitLeftPadded(a.getPadding().getOperator(), CLeftPadded.Location.ASSIGNMENT_OPERATION_OPERATOR, p));
        a = a.withAssignment(visitAndCast(a.getAssignment(), p));
        return a;
    }

    public C visitBinary(C.Binary binary, P p) {
        C.Binary b = binary;
        b = b.withPrefix(visitSpace(b.getPrefix(), Space.Location.BINARY_PREFIX, p));
        b = b.withMarkers(visitMarkers(b.getMarkers(), p));
        Expression temp = (Expression) visitExpression(b, p);
        if (!(temp instanceof C.Binary)) {
            return temp;
        } else {
            b = (C.Binary) temp;
        }
        b = b.withLeft(visitAndCast(b.getLeft(), p));
        b = b.getPadding().withOperator(visitLeftPadded(b.getPadding().getOperator(), CLeftPadded.Location.BINARY_OPERATOR, p));
        b = b.withRight(visitAndCast(b.getRight(), p));
        return b;
    }

    public C visitBlock(C.Block block, P p) {
        C.Block b = block;
        b = b.withPrefix(visitSpace(b.getPrefix(), Space.Location.BLOCK_PREFIX, p));
        b = b.withMarkers(visitMarkers(b.getMarkers(), p));
        b = b.getPadding().withStatic(visitRightPadded(b.getPadding().getStatic(), CRightPadded.Location.STATIC_INIT, p));
        Statement temp = (Statement) visitStatement(b, p);
        if (!(temp instanceof C.Block)) {
            return temp;
        } else {
            b = (C.Block) temp;
        }
        b = b.getPadding().withStatements(ListUtils.map(b.getPadding().getStatements(), t ->
                visitRightPadded(t, CRightPadded.Location.BLOCK_STATEMENT, p)));
        b = b.withEnd(visitSpace(b.getEnd(), Space.Location.BLOCK_END, p));
        return b;
    }

    public C visitBreak(C.Break breakStatement, P p) {
        C.Break b = breakStatement;
        b = b.withPrefix(visitSpace(b.getPrefix(), Space.Location.BREAK_PREFIX, p));
        b = b.withMarkers(visitMarkers(b.getMarkers(), p));
        Statement temp = (Statement) visitStatement(b, p);
        if (!(temp instanceof C.Break)) {
            return temp;
        } else {
            b = (C.Break) temp;
        }
        b = b.withLabel(visitAndCast(b.getLabel(), p));
        return b;
    }

    public C visitCase(C.Case caze, P p) {
        C.Case c = caze;
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.CASE_PREFIX, p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        Statement temp = (Statement) visitStatement(c, p);
        if (!(temp instanceof C.Case)) {
            return temp;
        } else {
            c = (C.Case) temp;
        }
        c = c.withPattern(visitAndCast(c.getPattern(), p));
        c = c.getPadding().withStatements(visitContainer(c.getPadding().getStatements(), CContainer.Location.CASE, p));
        return c;
    }

    public C visitCatch(C.Try.Catch catzh, P p) {
        C.Try.Catch c = catzh;
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.CATCH_PREFIX, p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        c = c.withParameter(visitAndCast(c.getParameter(), p));
        c = c.withBody(visitAndCast(c.getBody(), p));
        return c;
    }

    public C visitClassDeclaration(C.ClassDeclaration classDecl, P p) {
        C.ClassDeclaration c = classDecl;
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.CLASS_DECLARATION_PREFIX, p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        Statement temp = (Statement) visitStatement(c, p);
        if (!(temp instanceof C.ClassDeclaration)) {
            return temp;
        } else {
            c = (C.ClassDeclaration) temp;
        }
        c = c.withLeadingAnnotations(ListUtils.map(c.getLeadingAnnotations(), a -> visitAndCast(a, p)));
        c = c.withModifiers(ListUtils.map(c.getModifiers(),
                mod -> mod.withPrefix(visitSpace(mod.getPrefix(), Space.Location.MODIFIER_PREFIX, p))));
        c = c.withModifiers(ListUtils.map(c.getModifiers(), m -> visitAndCast(m, p)));
        //Kind can have annotations associated with it, need to visit those.
        c = c.getAnnotations().withKind(
                classDecl.getAnnotations().getKind().withAnnotations(
                        ListUtils.map(classDecl.getAnnotations().getKind().getAnnotations(), a -> visitAndCast(a, p))
                )
        );
        c = c.getAnnotations().withKind(
                c.getAnnotations().getKind().withPrefix(
                        visitSpace(c.getAnnotations().getKind().getPrefix(), Space.Location.CLASS_KIND, p)
                )
        );
        c = c.withName(visitAndCast(c.getName(), p));
        if (c.getPadding().getTypeParameters() != null) {
            c = c.getPadding().withTypeParameters(visitContainer(c.getPadding().getTypeParameters(), CContainer.Location.TYPE_PARAMETERS, p));
        }
        if (c.getPadding().getExtends() != null) {
            c = c.getPadding().withExtends(visitLeftPadded(c.getPadding().getExtends(), CLeftPadded.Location.EXTENDS, p));
        }
        c = c.getPadding().withExtends(visitTypeName(c.getPadding().getExtends(), p));
        if (c.getPadding().getImplements() != null) {
            c = c.getPadding().withImplements(visitContainer(c.getPadding().getImplements(), CContainer.Location.IMPLEMENTS, p));
        }
        c = c.getPadding().withImplements(visitTypeNames(c.getPadding().getImplements(), p));
        c = c.withBody(visitAndCast(c.getBody(), p));
        return c;
    }

    public C visitSourceFile(CSourceFile sourceFile, P p) {
        return sourceFile;
    }

    public C visitContinue(C.Continue continueStatement, P p) {
        C.Continue c = continueStatement;
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.CONTINUE_PREFIX, p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        Statement temp = (Statement) visitStatement(c, p);
        if (!(temp instanceof C.Continue)) {
            return temp;
        } else {
            c = (C.Continue) temp;
        }
        c = c.withLabel(visitAndCast(c.getLabel(), p));
        return c;
    }

    public <T extends C> C visitControlParentheses(C.ControlParentheses<T> controlParens, P p) {
        C.ControlParentheses<T> cp = controlParens;
        cp = cp.withPrefix(visitSpace(cp.getPrefix(), Space.Location.CONTROL_PARENTHESES_PREFIX, p));
        Expression temp = (Expression) visitExpression(cp, p);
        if (!(temp instanceof C.ControlParentheses)) {
            return temp;
        } else {
            //noinspection unchecked
            cp = (C.ControlParentheses<T>) temp;
        }
        cp = cp.getPadding().withTree(visitRightPadded(cp.getPadding().getTree(), CRightPadded.Location.PARENTHESES, p));
        cp = cp.withMarkers(visitMarkers(cp.getMarkers(), p));
        return cp;
    }

    public C visitDoWhileLoop(C.DoWhileLoop doWhileLoop, P p) {
        C.DoWhileLoop d = doWhileLoop;
        d = d.withPrefix(visitSpace(d.getPrefix(), Space.Location.DO_WHILE_PREFIX, p));
        d = d.withMarkers(visitMarkers(d.getMarkers(), p));
        Statement temp = (Statement) visitStatement(d, p);
        if (!(temp instanceof C.DoWhileLoop)) {
            return temp;
        } else {
            d = (C.DoWhileLoop) temp;
        }
        d = d.getPadding().withWhileCondition(visitLeftPadded(d.getPadding().getWhileCondition(), CLeftPadded.Location.WHILE_CONDITION, p));
        d = d.getPadding().withBody(visitRightPadded(d.getPadding().getBody(), CRightPadded.Location.WHILE_BODY, p));
        return d;
    }

    public C visitEmpty(C.Empty empty, P p) {
        C.Empty e = empty;
        e = e.withPrefix(visitSpace(e.getPrefix(), Space.Location.EMPTY_PREFIX, p));
        e = e.withMarkers(visitMarkers(e.getMarkers(), p));
        Statement temp = (Statement) visitStatement(e, p);
        if (!(temp instanceof C.Empty)) {
            return temp;
        } else {
            e = (C.Empty) temp;
        }
        Expression temp2 = (Expression) visitExpression(e, p);
        if (!(temp instanceof C.Empty)) {
            return temp2;
        } else {
            e = (C.Empty) temp2;
        }
        return e;
    }

    public C visitEnumValue(C.EnumValue enoom, P p) {
        C.EnumValue e = enoom;
        e = e.withPrefix(visitSpace(e.getPrefix(), Space.Location.ENUM_VALUE_PREFIX, p));
        e = e.withMarkers(visitMarkers(e.getMarkers(), p));
        e = e.withName(visitAndCast(e.getName(), p));
        e = e.withInitializer(visitAndCast(e.getInitializer(), p));
        return e;
    }

    public C visitEnumValueSet(C.EnumValueSet enums, P p) {
        C.EnumValueSet e = enums;
        e = e.withPrefix(visitSpace(e.getPrefix(), Space.Location.ENUM_VALUE_SET_PREFIX, p));
        e = e.withMarkers(visitMarkers(e.getMarkers(), p));
        Statement temp = (Statement) visitStatement(e, p);
        if (!(temp instanceof C.EnumValueSet)) {
            return temp;
        } else {
            e = (C.EnumValueSet) temp;
        }
        e = e.getPadding().withEnums(ListUtils.map(e.getPadding().getEnums(), t -> visitRightPadded(t, CRightPadded.Location.ENUM_VALUE, p)));
        return e;
    }

    public C visitFieldAccess(C.FieldAccess fieldAccess, P p) {
        C.FieldAccess f = fieldAccess;
        f = f.withPrefix(visitSpace(f.getPrefix(), Space.Location.FIELD_ACCESS_PREFIX, p));
        f = f.withMarkers(visitMarkers(f.getMarkers(), p));
        f = visitTypeName(f, p);
        Expression temp = (Expression) visitExpression(f, p);
        if (!(temp instanceof C.FieldAccess)) {
            return temp;
        } else {
            f = (C.FieldAccess) temp;
        }
        f = f.withTarget(visitAndCast(f.getTarget(), p));
        f = f.getPadding().withName(visitLeftPadded(f.getPadding().getName(), CLeftPadded.Location.FIELD_ACCESS_NAME, p));
        return f;
    }

    public C visitForEachLoop(C.ForEachLoop forLoop, P p) {
        C.ForEachLoop f = forLoop;
        f = f.withPrefix(visitSpace(f.getPrefix(), Space.Location.FOR_EACH_LOOP_PREFIX, p));
        f = f.withMarkers(visitMarkers(f.getMarkers(), p));
        Statement temp = (Statement) visitStatement(f, p);
        if (!(temp instanceof C.ForEachLoop)) {
            return temp;
        } else {
            f = (C.ForEachLoop) temp;
        }
        f = f.withControl(visitAndCast(f.getControl(), p));
        f = f.getPadding().withBody(visitRightPadded(f.getPadding().getBody(), CRightPadded.Location.FOR_BODY, p));
        return f;
    }

    public C visitForEachControl(C.ForEachLoop.Control control, P p) {
        C.ForEachLoop.Control c = control;
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.FOR_EACH_CONTROL_PREFIX, p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        c = c.getPadding().withVariable(visitRightPadded(c.getPadding().getVariable(), CRightPadded.Location.FOREACH_VARIABLE, p));
        c = c.getPadding().withIterable(visitRightPadded(c.getPadding().getIterable(), CRightPadded.Location.FOREACH_ITERABLE, p));
        return c;
    }

    public C visitForLoop(C.ForLoop forLoop, P p) {
        C.ForLoop f = forLoop;
        f = f.withPrefix(visitSpace(f.getPrefix(), Space.Location.FOR_PREFIX, p));
        f = f.withMarkers(visitMarkers(f.getMarkers(), p));
        Statement temp = (Statement) visitStatement(f, p);
        if (!(temp instanceof C.ForLoop)) {
            return temp;
        } else {
            f = (C.ForLoop) temp;
        }
        f = f.withControl(visitAndCast(f.getControl(), p));
        f = f.getPadding().withBody(visitRightPadded(f.getPadding().getBody(), CRightPadded.Location.FOR_BODY, p));
        return f;
    }

    public C visitForControl(C.ForLoop.Control control, P p) {
        C.ForLoop.Control c = control;
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.FOR_CONTROL_PREFIX, p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        c = c.getPadding().withInit(ListUtils.map(c.getPadding().getInit(), t -> visitRightPadded(t, CRightPadded.Location.FOR_INIT, p)));
        c = c.getPadding().withCondition(visitRightPadded(c.getPadding().getCondition(), CRightPadded.Location.FOR_CONDITION, p));
        c = c.getPadding().withUpdate(ListUtils.map(c.getPadding().getUpdate(), t -> visitRightPadded(t, CRightPadded.Location.FOR_UPDATE, p)));
        return c;
    }

    public C visitIdentifier(C.Identifier ident, P p) {
        C.Identifier i = ident;
        i = i.withPrefix(visitSpace(i.getPrefix(), Space.Location.IDENTIFIER_PREFIX, p));
        i = i.withMarkers(visitMarkers(i.getMarkers(), p));
        Expression temp = (Expression) visitExpression(i, p);
        if (!(temp instanceof C.Identifier)) {
            return temp;
        } else {
            i = (C.Identifier) temp;
        }
        return i;
    }

    public C visitElse(C.If.Else elze, P p) {
        C.If.Else e = elze;
        e = e.withPrefix(visitSpace(e.getPrefix(), Space.Location.ELSE_PREFIX, p));
        e = e.withMarkers(visitMarkers(e.getMarkers(), p));
        e = e.getPadding().withBody(visitRightPadded(e.getPadding().getBody(), CRightPadded.Location.IF_ELSE, p));
        return e;
    }

    public C visitIf(C.If iff, P p) {
        C.If i = iff;
        i = i.withPrefix(visitSpace(i.getPrefix(), Space.Location.IF_PREFIX, p));
        i = i.withMarkers(visitMarkers(i.getMarkers(), p));
        Statement temp = (Statement) visitStatement(i, p);
        if (!(temp instanceof C.If)) {
            return temp;
        } else {
            i = (C.If) temp;
        }
        i = i.withIfCondition(visitAndCast(i.getIfCondition(), p));
        i = i.getPadding().withThenPart(visitRightPadded(i.getPadding().getThenPart(), CRightPadded.Location.IF_THEN, p));
        i = i.withElsePart(visitAndCast(i.getElsePart(), p));
        return i;
    }

    public C visitInstanceOf(C.InstanceOf instanceOf, P p) {
        C.InstanceOf i = instanceOf;
        i = i.withPrefix(visitSpace(i.getPrefix(), Space.Location.INSTANCEOF_PREFIX, p));
        i = i.withMarkers(visitMarkers(i.getMarkers(), p));
        Expression temp = (Expression) visitExpression(i, p);
        if (!(temp instanceof C.InstanceOf)) {
            return temp;
        } else {
            i = (C.InstanceOf) temp;
        }
        i = i.getPadding().withExpr(visitRightPadded(i.getPadding().getExpr(), CRightPadded.Location.INSTANCEOF, p));
        i = i.withClazz(visitAndCast(i.getClazz(), p));
        return i;
    }

    public C visitLabel(C.Label label, P p) {
        C.Label l = label;
        l = l.withPrefix(visitSpace(l.getPrefix(), Space.Location.LABEL_PREFIX, p));
        l = l.withMarkers(visitMarkers(l.getMarkers(), p));
        Statement temp = (Statement) visitStatement(l, p);
        if (!(temp instanceof C.Label)) {
            return temp;
        } else {
            l = (C.Label) temp;
        }
        l = l.getPadding().withLabel(visitRightPadded(l.getPadding().getLabel(), CRightPadded.Location.LABEL, p));
        l = l.withStatement(visitAndCast(l.getStatement(), p));
        return l;
    }

    public C visitLambda(C.Lambda lambda, P p) {
        C.Lambda l = lambda;
        l = l.withPrefix(visitSpace(l.getPrefix(), Space.Location.LAMBDA_PREFIX, p));
        l = l.withMarkers(visitMarkers(l.getMarkers(), p));
        Expression temp = (Expression) visitExpression(l, p);
        if (!(temp instanceof C.Lambda)) {
            return temp;
        } else {
            l = (C.Lambda) temp;
        }
        l = l.withParameters(
                l.getParameters().withPrefix(
                        visitSpace(l.getParameters().getPrefix(), Space.Location.LAMBDA_PARAMETERS_PREFIX, p)
                )
        );
        l = l.withParameters(
                l.getParameters().getPadding().withParams(
                        ListUtils.map(l.getParameters().getPadding().getParams(),
                                param -> visitRightPadded(param, CRightPadded.Location.LAMBDA_PARAM, p)
                        )
                )
        );
        l = l.withParameters(visitAndCast(l.getParameters(), p));
        l = l.withArrow(visitSpace(l.getArrow(), Space.Location.LAMBDA_ARROW_PREFIX, p));
        l = l.withBody(visitAndCast(l.getBody(), p));
        return l;
    }

    public C visitLiteral(C.Literal literal, P p) {
        C.Literal l = literal;
        l = l.withPrefix(visitSpace(l.getPrefix(), Space.Location.LITERAL_PREFIX, p));
        l = l.withMarkers(visitMarkers(l.getMarkers(), p));
        Expression temp = (Expression) visitExpression(l, p);
        if (!(temp instanceof C.Literal)) {
            return temp;
        } else {
            l = (C.Literal) temp;
        }
        return l;
    }

    public C visitMemberReference(C.MemberReference memberRef, P p) {
        C.MemberReference m = memberRef;
        m = m.withPrefix(visitSpace(m.getPrefix(), Space.Location.MEMBER_REFERENCE_PREFIX, p));
        m = m.withMarkers(visitMarkers(m.getMarkers(), p));
        m = m.getPadding().withContaining(visitRightPadded(m.getPadding().getContaining(), CRightPadded.Location.MEMBER_REFERENCE_CONTAINING, p));
        if (m.getPadding().getTypeParameters() != null) {
            m = m.getPadding().withTypeParameters(visitContainer(m.getPadding().getTypeParameters(), CContainer.Location.TYPE_PARAMETERS, p));
        }
        m = m.getPadding().withReference(visitLeftPadded(m.getPadding().getReference(), CLeftPadded.Location.MEMBER_REFERENCE_NAME, p));
        return m;
    }

    public C visitMethodDeclaration(C.MethodDeclaration method, P p) {
        C.MethodDeclaration m = method;
        m = m.withPrefix(visitSpace(m.getPrefix(), Space.Location.METHOD_DECLARATION_PREFIX, p));
        m = m.withMarkers(visitMarkers(m.getMarkers(), p));
        Statement temp = (Statement) visitStatement(m, p);
        if (!(temp instanceof C.MethodDeclaration)) {
            return temp;
        } else {
            m = (C.MethodDeclaration) temp;
        }
        m = m.withLeadingAnnotations(ListUtils.map(m.getLeadingAnnotations(), a -> visitAndCast(a, p)));
        m = m.withModifiers(ListUtils.map(m.getModifiers(), e -> visitAndCast(e, p)));
        m = m.withModifiers(ListUtils.map(m.getModifiers(),
                mod -> mod.withPrefix(visitSpace(mod.getPrefix(), Space.Location.MODIFIER_PREFIX, p))));
        C.TypeParameters typeParameters = m.getAnnotations().getTypeParameters();
        if (typeParameters != null) {
            m = m.getAnnotations().withTypeParameters(typeParameters.withAnnotations(
                    ListUtils.map(typeParameters.getAnnotations(), a -> visitAndCast(a, p))
            ));
        }
        typeParameters = m.getAnnotations().getTypeParameters();
        if (typeParameters != null) {
            m = m.getAnnotations().withTypeParameters(
                    typeParameters.getPadding().withTypeParameters(
                            ListUtils.map(typeParameters.getPadding().getTypeParameters(),
                                    tp -> visitRightPadded(tp, CRightPadded.Location.TYPE_PARAMETER, p)
                            )
                    )
            );
        }
        m = m.withReturnTypeExpression(visitAndCast(m.getReturnTypeExpression(), p));
        m = m.withReturnTypeExpression(
                m.getReturnTypeExpression() == null ?
                        null :
                        visitTypeName(m.getReturnTypeExpression(), p));
        m = m.getAnnotations().withName(m.getAnnotations().getName().withAnnotations(ListUtils.map(m.getAnnotations().getName().getAnnotations(), a -> visitAndCast(a, p))));
        m = m.withName(visitAndCast(m.getName(), p));
        m = m.getPadding().withParameters(visitContainer(m.getPadding().getParameters(), CContainer.Location.METHOD_DECLARATION_PARAMETERS, p));
        if (m.getPadding().getThrows() != null) {
            m = m.getPadding().withThrows(visitContainer(m.getPadding().getThrows(), CContainer.Location.THROWS, p));
        }
        m = m.getPadding().withThrows(visitTypeNames(m.getPadding().getThrows(), p));
        m = m.withBody(visitAndCast(m.getBody(), p));
        if (m.getPadding().getDefaultValue() != null) {
            m = m.getPadding().withDefaultValue(visitLeftPadded(m.getPadding().getDefaultValue(), CLeftPadded.Location.METHOD_DECLARATION_DEFAULT_VALUE, p));
        }
        return m;
    }

    public C visitMethodInvocation(C.MethodInvocation method, P p) {
        C.MethodInvocation m = method;
        m = m.withPrefix(visitSpace(m.getPrefix(), Space.Location.METHOD_INVOCATION_PREFIX, p));
        m = m.withMarkers(visitMarkers(m.getMarkers(), p));
        Statement temp = (Statement) visitStatement(m, p);
        if (!(temp instanceof C.MethodInvocation)) {
            return temp;
        } else {
            m = (C.MethodInvocation) temp;
        }
        Expression temp2 = (Expression) visitExpression(m, p);
        if (!(temp2 instanceof C.MethodInvocation)) {
            return temp2;
        } else {
            m = (C.MethodInvocation) temp2;
        }
        if (m.getPadding().getSelect() != null && m.getPadding().getSelect().getElement() instanceof NameTree &&
                method.getType() != null && method.getType().hasFlags(Flag.Static)) {
            //noinspection unchecked
            m = m.getPadding().withSelect(
                    (CRightPadded<Expression>) (CRightPadded<?>)
                            visitTypeName((CRightPadded<NameTree>) (CRightPadded<?>) m.getPadding().getSelect(), p));
        }
        if (m.getPadding().getSelect() != null) {
            m = m.getPadding().withSelect(visitRightPadded(m.getPadding().getSelect(), CRightPadded.Location.METHOD_SELECT, p));
        }
        if (m.getPadding().getTypeParameters() != null) {
            m = m.getPadding().withTypeParameters(visitContainer(m.getPadding().getTypeParameters(), CContainer.Location.TYPE_PARAMETERS, p));
        }
        m = m.getPadding().withTypeParameters(visitTypeNames(m.getPadding().getTypeParameters(), p));
        m = m.withName(visitAndCast(m.getName(), p));
        m = m.getPadding().withArguments(visitContainer(m.getPadding().getArguments(), CContainer.Location.METHOD_INVOCATION_ARGUMENTS, p));
        return m;
    }

    public C visitMultiCatch(C.MultiCatch multiCatch, P p) {
        C.MultiCatch m = multiCatch;
        m = m.withPrefix(visitSpace(m.getPrefix(), Space.Location.MULTI_CATCH_PREFIX, p));
        m = m.withMarkers(visitMarkers(m.getMarkers(), p));
        m = m.getPadding().withAlternatives(ListUtils.map(m.getPadding().getAlternatives(), t ->
                visitTypeName(visitRightPadded(t, CRightPadded.Location.CATCH_ALTERNATIVE, p), p)));
        return m;
    }

    public C visitVariableDeclarations(C.VariableDeclarations multiVariable, P p) {
        C.VariableDeclarations m = multiVariable;
        m = m.withPrefix(visitSpace(m.getPrefix(), Space.Location.VARIABLE_DECLARATIONS_PREFIX, p));
        m = m.withMarkers(visitMarkers(m.getMarkers(), p));
        Statement temp = (Statement) visitStatement(m, p);
        if (!(temp instanceof C.VariableDeclarations)) {
            return temp;
        } else {
            m = (C.VariableDeclarations) temp;
        }
        m = m.withLeadingAnnotations(ListUtils.map(m.getLeadingAnnotations(), a -> visitAndCast(a, p)));
        m = m.withModifiers(Objects.requireNonNull(ListUtils.map(m.getModifiers(), e -> visitAndCast(e, p))));
        m = m.withModifiers(ListUtils.map(m.getModifiers(),
                mod -> mod.withPrefix(visitSpace(mod.getPrefix(), Space.Location.MODIFIER_PREFIX, p))));
        m = m.withTypeExpression(visitAndCast(m.getTypeExpression(), p));
        m = m.withDimensionsBeforeName(ListUtils.map(m.getDimensionsBeforeName(), dim ->
                dim.withBefore(visitSpace(dim.getBefore(), Space.Location.DIMENSION_PREFIX, p))
                        .withElement(visitSpace(dim.getElement(), Space.Location.DIMENSION, p))
        ));
        m = m.withTypeExpression(m.getTypeExpression() == null ?
                null :
                visitTypeName(m.getTypeExpression(), p));
        m = m.withVarargs(m.getVarargs() == null ?
                null :
                visitSpace(m.getVarargs(), Space.Location.VARARGS, p));
        m = m.getPadding().withVariables(ListUtils.map(m.getPadding().getVariables(), t -> visitRightPadded(t, CRightPadded.Location.NAMED_VARIABLE, p)));
        return m;
    }

    public C visitNewArray(C.NewArray newArray, P p) {
        C.NewArray n = newArray;
        n = n.withPrefix(visitSpace(n.getPrefix(), Space.Location.NEW_ARRAY_PREFIX, p));
        n = n.withMarkers(visitMarkers(n.getMarkers(), p));
        Expression temp = (Expression) visitExpression(n, p);
        if (!(temp instanceof C.NewArray)) {
            return temp;
        } else {
            n = (C.NewArray) temp;
        }
        n = n.withTypeExpression(visitAndCast(n.getTypeExpression(), p));
        n = n.withTypeExpression(n.getTypeExpression() == null ?
                null :
                visitTypeName(n.getTypeExpression(), p));
        n = n.withDimensions(ListUtils.map(n.getDimensions(), d -> visitAndCast(d, p)));
        if (n.getPadding().getInitializer() != null) {
            n = n.getPadding().withInitializer(visitContainer(n.getPadding().getInitializer(), CContainer.Location.NEW_ARRAY_INITIALIZER, p));
        }
        return n;
    }

    public C visitNewClass(C.NewClass newClass, P p) {
        C.NewClass n = newClass;
        n = n.withPrefix(visitSpace(n.getPrefix(), Space.Location.NEW_CLASS_PREFIX, p));
        n = n.withMarkers(visitMarkers(n.getMarkers(), p));
        if (n.getPadding().getEnclosing() != null) {
            n = n.getPadding().withEnclosing(visitRightPadded(n.getPadding().getEnclosing(), CRightPadded.Location.NEW_CLASS_ENCLOSING, p));
        }
        Statement temp = (Statement) visitStatement(n, p);
        if (!(temp instanceof C.NewClass)) {
            return temp;
        } else {
            n = (C.NewClass) temp;
        }
        Expression temp2 = (Expression) visitExpression(n, p);
        if (!(temp2 instanceof C.NewClass)) {
            return temp2;
        } else {
            n = (C.NewClass) temp2;
        }
        n = n.withNew(visitSpace(n.getNew(), Space.Location.NEW_PREFIX, p));
        n = n.withClazz(visitAndCast(n.getClazz(), p));
        n = n.withClazz(n.getClazz() == null ?
                null :
                visitTypeName(n.getClazz(), p));
        if (n.getPadding().getArguments() != null) {
            n = n.getPadding().withArguments(visitContainer(n.getPadding().getArguments(), CContainer.Location.NEW_CLASS_ARGUMENTS, p));
        }
        n = n.withBody(visitAndCast(n.getBody(), p));
        return n;
    }

    public C visitParameterizedType(C.ParameterizedType type, P p) {
        C.ParameterizedType pt = type;
        pt = pt.withPrefix(visitSpace(pt.getPrefix(), Space.Location.PARAMETERIZED_TYPE_PREFIX, p));
        pt = pt.withMarkers(visitMarkers(pt.getMarkers(), p));
        Expression temp = (Expression) visitExpression(pt, p);
        if (!(temp instanceof C.ParameterizedType)) {
            return temp;
        } else {
            pt = (C.ParameterizedType) temp;
        }
        pt = pt.withClazz(visitAndCast(pt.getClazz(), p));
        if (pt.getPadding().getTypeParameters() != null) {
            pt = pt.getPadding().withTypeParameters(visitContainer(pt.getPadding().getTypeParameters(), CContainer.Location.TYPE_PARAMETERS, p));
        }
        pt = pt.getPadding().withTypeParameters(visitTypeNames(pt.getPadding().getTypeParameters(), p));
        return pt;
    }

    public <T extends C> C visitParentheses(C.Parentheses<T> parens, P p) {
        C.Parentheses<T> pa = parens;
        pa = pa.withPrefix(visitSpace(pa.getPrefix(), Space.Location.PARENTHESES_PREFIX, p));
        pa = pa.withMarkers(visitMarkers(pa.getMarkers(), p));
        Expression temp = (Expression) visitExpression(pa, p);
        if (!(temp instanceof C.Parentheses)) {
            return temp;
        } else {
            //noinspection unchecked
            pa = (C.Parentheses<T>) temp;
        }
        pa = pa.getPadding().withTree(visitRightPadded(pa.getPadding().getTree(), CRightPadded.Location.PARENTHESES, p));
        return pa;
    }

    public C visitPrimitive(C.Primitive primitive, P p) {
        C.Primitive pr = primitive;
        pr = pr.withPrefix(visitSpace(pr.getPrefix(), Space.Location.PRIMITIVE_PREFIX, p));
        pr = pr.withMarkers(visitMarkers(pr.getMarkers(), p));
        Expression temp = (Expression) visitExpression(pr, p);
        if (!(temp instanceof C.Primitive)) {
            return temp;
        } else {
            pr = (C.Primitive) temp;
        }
        return pr;
    }

    public C visitReturn(C.Return retrn, P p) {
        C.Return r = retrn;
        r = r.withPrefix(visitSpace(r.getPrefix(), Space.Location.RETURN_PREFIX, p));
        r = r.withMarkers(visitMarkers(r.getMarkers(), p));
        Statement temp = (Statement) visitStatement(r, p);
        if (!(temp instanceof C.Return)) {
            return temp;
        } else {
            r = (C.Return) temp;
        }
        r = r.withExpression(visitAndCast(r.getExpression(), p));
        return r;
    }

    public C visitSwitch(C.Switch switzh, P p) {
        C.Switch s = switzh;
        s = s.withPrefix(visitSpace(s.getPrefix(), Space.Location.SWITCH_PREFIX, p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        Statement temp = (Statement) visitStatement(s, p);
        if (!(temp instanceof C.Switch)) {
            return temp;
        } else {
            s = (C.Switch) temp;
        }
        s = s.withSelector(visitAndCast(s.getSelector(), p));
        s = s.withCases(visitAndCast(s.getCases(), p));
        return s;
    }

    public C visitSynchronized(C.Synchronized synch, P p) {
        C.Synchronized s = synch;
        s = s.withPrefix(visitSpace(s.getPrefix(), Space.Location.SYNCHRONIZED_PREFIX, p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        Statement temp = (Statement) visitStatement(s, p);
        if (!(temp instanceof C.Synchronized)) {
            return temp;
        } else {
            s = (C.Synchronized) temp;
        }
        s = s.withLock(visitAndCast(s.getLock(), p));
        s = s.withBody(visitAndCast(s.getBody(), p));
        return s;
    }

    public C visitTernary(C.Ternary ternary, P p) {
        C.Ternary t = ternary;
        t = t.withPrefix(visitSpace(t.getPrefix(), Space.Location.TERNARY_PREFIX, p));
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));
        Expression temp = (Expression) visitExpression(t, p);
        if (!(temp instanceof C.Ternary)) {
            return temp;
        } else {
            t = (C.Ternary) temp;
        }
        t = t.withCondition(visitAndCast(t.getCondition(), p));
        t = t.getPadding().withTruePart(visitLeftPadded(t.getPadding().getTruePart(), CLeftPadded.Location.TERNARY_TRUE, p));
        t = t.getPadding().withFalsePart(visitLeftPadded(t.getPadding().getFalsePart(), CLeftPadded.Location.TERNARY_FALSE, p));
        return t;
    }

    public C visitThrow(C.Throw thrown, P p) {
        C.Throw t = thrown;
        t = t.withPrefix(visitSpace(t.getPrefix(), Space.Location.THROW_PREFIX, p));
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));
        Statement temp = (Statement) visitStatement(t, p);
        if (!(temp instanceof C.Throw)) {
            return temp;
        } else {
            t = (C.Throw) temp;
        }
        t = t.withException(visitAndCast(t.getException(), p));
        return t;
    }

    public C visitTry(C.Try tryable, P p) {
        C.Try t = tryable;
        t = t.withPrefix(visitSpace(t.getPrefix(), Space.Location.TRY_PREFIX, p));
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));
        Statement temp = (Statement) visitStatement(t, p);
        if (!(temp instanceof C.Try)) {
            return temp;
        } else {
            t = (C.Try) temp;
        }
        if (t.getPadding().getResources() != null) {
            t = t.getPadding().withResources(visitContainer(t.getPadding().getResources(), CContainer.Location.TRY_RESOURCES, p));
        }
        t = t.withBody(visitAndCast(t.getBody(), p));
        t = t.withCatches(ListUtils.map(t.getCatches(), c -> visitAndCast(c, p)));
        if (t.getPadding().getFinally() != null) {
            t = t.getPadding().withFinally(visitLeftPadded(t.getPadding().getFinally(), CLeftPadded.Location.TRY_FINALLY, p));
        }
        return t;
    }

    public C visitTryResource(C.Try.Resource tryResource, P p) {
        C.Try.Resource r = tryResource;
        r = tryResource.withPrefix(visitSpace(r.getPrefix(), Space.Location.TRY_RESOURCE, p));
        r = r.withMarkers(visitMarkers(r.getMarkers(), p));
        r = tryResource.withVariableDeclarations(visitAndCast(r.getVariableDeclarations(), p));
        return r;
    }

    public C visitTypeCast(C.TypeCast typeCast, P p) {
        C.TypeCast t = typeCast;
        t = t.withPrefix(visitSpace(t.getPrefix(), Space.Location.TYPE_CAST_PREFIX, p));
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));
        Expression temp = (Expression) visitExpression(t, p);
        if (!(temp instanceof C.TypeCast)) {
            return temp;
        } else {
            t = (C.TypeCast) temp;
        }
        t = t.withClazz(visitAndCast(t.getClazz(), p));
        t = t.withClazz(t.getClazz().withTree(visitTypeName(t.getClazz().getTree(), p)));
        t = t.withExpression(visitAndCast(t.getExpression(), p));
        return t;
    }

    public C visitTypeParameter(C.TypeParameter typeParam, P p) {
        C.TypeParameter t = typeParam;
        t = t.withPrefix(visitSpace(t.getPrefix(), Space.Location.TYPE_PARAMETERS_PREFIX, p));
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));
        t = t.withAnnotations(ListUtils.map(t.getAnnotations(), a -> visitAndCast(a, p)));
        t = t.withName(visitAndCast(t.getName(), p));
        if (t.getName() instanceof NameTree) {
            t = t.withName((Expression) visitTypeName((NameTree) t.getName(), p));
        }
        if (t.getPadding().getBounds() != null) {
            t = t.getPadding().withBounds(visitContainer(t.getPadding().getBounds(), CContainer.Location.TYPE_BOUNDS, p));
        }
        t = t.getPadding().withBounds(visitTypeNames(t.getPadding().getBounds(), p));
        return t;
    }

    public C visitUnary(C.Unary unary, P p) {
        C.Unary u = unary;
        u = u.withPrefix(visitSpace(u.getPrefix(), Space.Location.UNARY_PREFIX, p));
        u = u.withMarkers(visitMarkers(u.getMarkers(), p));
        Statement temp = (Statement) visitStatement(u, p);
        if (!(temp instanceof C.Unary)) {
            return temp;
        } else {
            u = (C.Unary) temp;
        }
        Expression temp2 = (Expression) visitExpression(u, p);
        if (!(temp2 instanceof C.Unary)) {
            return temp2;
        } else {
            u = (C.Unary) temp2;
        }
        u = u.getPadding().withOperator(visitLeftPadded(u.getPadding().getOperator(), CLeftPadded.Location.UNARY_OPERATOR, p));
        u = u.withExpression(visitAndCast(u.getExpression(), p));
        return u;
    }

    //VarType represents the "var" expression in local variable type inference.
    public C visitVarType(C.VarType varType, P p) {
        C.VarType v = varType;
        v = v.withMarkers(visitMarkers(v.getMarkers(), p));
        return visitExpression(v, p);
    }

    public C visitVariable(C.VariableDeclarations.NamedVariable variable, P p) {
        C.VariableDeclarations.NamedVariable v = variable;
        v = v.withPrefix(visitSpace(v.getPrefix(), Space.Location.VARIABLE_PREFIX, p));
        v = v.withMarkers(visitMarkers(v.getMarkers(), p));
        v = v.withName(visitAndCast(v.getName(), p));
        v = v.withDimensionsAfterName(
                ListUtils.map(v.getDimensionsAfterName(),
                        dim -> dim.withBefore(visitSpace(dim.getBefore(), Space.Location.DIMENSION_PREFIX, p))
                                .withElement(visitSpace(dim.getElement(), Space.Location.DIMENSION, p))
                )
        );
        if (v.getPadding().getInitializer() != null) {
            v = v.getPadding().withInitializer(visitLeftPadded(v.getPadding().getInitializer(),
                    CLeftPadded.Location.VARIABLE_INITIALIZER, p));
        }
        return v;
    }

    public C visitWhileLoop(C.WhileLoop whileLoop, P p) {
        C.WhileLoop w = whileLoop;
        w = w.withPrefix(visitSpace(w.getPrefix(), Space.Location.WHILE_PREFIX, p));
        w = w.withMarkers(visitMarkers(w.getMarkers(), p));
        Statement temp = (Statement) visitStatement(w, p);
        if (!(temp instanceof C.WhileLoop)) {
            return temp;
        } else {
            w = (C.WhileLoop) temp;
        }
        w = w.withCondition(visitAndCast(w.getCondition(), p));
        w = w.getPadding().withBody(visitRightPadded(w.getPadding().getBody(), CRightPadded.Location.WHILE_BODY, p));
        return w;
    }

    public C visitWildcard(C.Wildcard wildcard, P p) {
        C.Wildcard w = wildcard;
        w = w.withPrefix(visitSpace(w.getPrefix(), Space.Location.WILDCARD_PREFIX, p));
        w = w.withMarkers(visitMarkers(w.getMarkers(), p));
        Expression temp = (Expression) visitExpression(w, p);
        if (!(temp instanceof C.Wildcard)) {
            return temp;
        } else {
            w = (C.Wildcard) temp;
        }
        if (w.getPadding().getBound() != null) {
            w = w.getPadding().withBound(
                    w.getPadding().getBound().withBefore(
                            visitSpace(w.getPadding().getBound().getBefore(), Space.Location.WILDCARD_BOUND, p)
                    )
            );
        }
        w = w.withBoundedType(visitAndCast(w.getBoundedType(), p));
        if (w.getBoundedType() != null) {
            // i.e. not a "wildcard" type
            w = w.withBoundedType(visitTypeName(w.getBoundedType(), p));
        }
        return w;
    }

    public <T> CRightPadded<T> visitRightPadded(@Nullable CRightPadded<T> right, CRightPadded.Location loc, P p) {
        if (right == null) {
            //noinspection ConstantConditions
            return null;
        }

        setCursor(new Cursor(getCursor(), right));

        T t = right.getElement();
        if (t instanceof C) {
            //noinspection unchecked
            t = visitAndCast((C) right.getElement(), p);
        }

        setCursor(getCursor().getParent());
        if (t == null) {
            return null;
        }
        Space after = visitSpace(right.getAfter(), loc.getAfterLocation(), p);
        return (after == right.getAfter() && t == right.getElement()) ? right : new CRightPadded<>(t, after, right.getMarkers());
    }

    public <T> CLeftPadded<T> visitLeftPadded(CLeftPadded<T> left, CLeftPadded.Location loc, P p) {
        setCursor(new Cursor(getCursor(), left));

        Space before = visitSpace(left.getBefore(), loc.getBeforeLocation(), p);
        T t = left.getElement();

        if (t instanceof C) {
            //noinspection unchecked
            t = visitAndCast((C) left.getElement(), p);
        }

        setCursor(getCursor().getParent());
        if (t == null) {
            return null;
        }

        return (before == left.getBefore() && t == left.getElement()) ? left : new CLeftPadded<>(before, t, left.getMarkers());
    }

    public <J2 extends C> CContainer<J2> visitContainer(CContainer<J2> container,
                                                        CContainer.Location loc, P p) {
        setCursor(new Cursor(getCursor(), container));

        Space before = visitSpace(container.getBefore(), loc.getBeforeLocation(), p);
        List<CRightPadded<J2>> js = ListUtils.map(container.getPadding().getElements(), t -> visitRightPadded(t, loc.getElementLocation(), p));

        setCursor(getCursor().getParent());

        return js == container.getPadding().getElements() && before == container.getBefore() ?
                container :
                CContainer.build(before, js, container.getMarkers());
    }

    /**
     * Check if a child AST element is in the same lexical scope as that of the AST element associated with the base
     * cursor. (i.e.: Are the variables and declarations visible in the base scope also visible to the child AST
     * element?)
     * <p>
     * The base lexical scope is first established by walking up the path of the base cursor to find its first enclosing
     * element. The child path is traversed by walking up the child path elements until either the base scope has
     * been found, a "terminating" element is encountered, or there are no more elements in the path.
     * <P><P>
     * A terminating element is one of the following:
     * <P><P>
     * <li>A static class declaration</li>
     * <li>An enumeration declaration</li>
     * <li>An interface declaration</li>
     * <li>An annotation declaration</li>
     *
     * @param base  A pointer within the AST that is used to establish the "base lexical scope".
     * @param child A pointer within the AST that will be traversed (up the tree) looking for an intersection with the base lexical scope.
     * @return true if the child is in within the lexical scope of the base
     */
    protected boolean isInSameNameScope(Cursor base, Cursor child) {
        //First establish the base scope by finding the first enclosing element.
        Tree baseScope = base.dropParentUntil(t -> t instanceof C.Block ||
                t instanceof C.MethodDeclaration ||
                t instanceof C.Try ||
                t instanceof C.ForLoop ||
                t instanceof C.ForEachLoop).getValue();

        //Now walk up the child path looking for the base scope.
        for (Iterator<Object> it = child.getPath(); it.hasNext(); ) {
            Object childScope = it.next();
            if (childScope instanceof C.ClassDeclaration) {
                C.ClassDeclaration childClass = (C.ClassDeclaration) childScope;
                if (!(childClass.getKind().equals(C.ClassDeclaration.Kind.Type.Class)) ||
                        childClass.hasModifier(C.Modifier.Type.Static)) {
                    //Short circuit the search if a terminating element is encountered.
                    return false;
                }
            }
            if (childScope instanceof Tree && baseScope.isScope((Tree) childScope)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if a child AST element is in the same lexical scope as that of the AST element associated with the current
     * cursor.
     * <p>
     * See {@link CVisitor#isInSameNameScope}
     *
     * @param child A pointer to an element within the abstract syntax tree
     * @return true if the child is in within the lexical scope of the current cursor
     */
    protected boolean isInSameNameScope(Cursor child) {
        return isInSameNameScope(getCursor(), child);
    }
}
