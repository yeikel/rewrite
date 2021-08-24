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

import org.openrewrite.family.c.tree.Expression;
import org.openrewrite.family.c.tree.C;
import org.openrewrite.family.c.tree.CSourceFile;
import org.openrewrite.family.c.tree.Statement;

/**
 * This iso(morphic) refactoring visitor is the appropriate base class for most Java refactoring visitors.
 * It comes with an additional constraint compared to the non-isomorphic JavaRefactorVisitor:
 * Each visit method must return an AST element of the same type as the one being visited.
 *
 * For visitors that do not need the extra flexibility of JavaRefactorVisitor, this constraint
 * makes for a more pleasant visitor authoring experience as less casting will be required.
 */
public class CIsoVisitor<P> extends CVisitor<P> {
    @Override
    public Expression visitExpression(Expression expression, P p) {
        return (Expression) super.visitExpression(expression, p);
    }

    @Override
    public Statement visitStatement(Statement statement, P p) {
        return (Statement) super.visitStatement(statement, p);
    }

    @Override
    public C.AnnotatedType visitAnnotatedType(C.AnnotatedType annotatedType, P p) {
        return (C.AnnotatedType) super.visitAnnotatedType(annotatedType, p);
    }

    @Override
    public C.Annotation visitAnnotation(C.Annotation annotation, P p) {
        return (C.Annotation) super.visitAnnotation(annotation, p);
    }

    @Override
    public C.ArrayAccess visitArrayAccess(C.ArrayAccess arrayAccess, P p) {
        return (C.ArrayAccess) super.visitArrayAccess(arrayAccess, p);
    }

    @Override
    public C.ArrayDimension visitArrayDimension(C.ArrayDimension arrayDimension, P p) {
        return (C.ArrayDimension) super.visitArrayDimension(arrayDimension, p);
    }

    @Override
    public C.ArrayType visitArrayType(C.ArrayType arrayType, P p) {
        return (C.ArrayType) super.visitArrayType(arrayType, p);
    }

    @Override
    public C.Assert visitAssert(C.Assert _assert, P p) {
        return (C.Assert) super.visitAssert(_assert, p);
    }

    @Override
    public C.Assignment visitAssignment(C.Assignment assignment, P p) {
        return (C.Assignment) super.visitAssignment(assignment, p);
    }

    @Override
    public C.AssignmentOperation visitAssignmentOperation(C.AssignmentOperation assignOp, P p) {
        return (C.AssignmentOperation) super.visitAssignmentOperation(assignOp, p);
    }

    @Override
    public C.Binary visitBinary(C.Binary binary, P p) {
        return (C.Binary) super.visitBinary(binary, p);
    }

    @Override
    public C.Block visitBlock(C.Block block, P p) {
        return (C.Block) super.visitBlock(block, p);
    }

    @Override
    public C.Break visitBreak(C.Break breakStatement, P p) {
        return (C.Break) super.visitBreak(breakStatement, p);
    }

    @Override
    public C.Case visitCase(C.Case _case, P p) {
        return (C.Case) super.visitCase(_case, p);
    }

    @Override
    public C.Try.Catch visitCatch(C.Try.Catch _catch, P p) {
        return (C.Try.Catch) super.visitCatch(_catch, p);
    }

    @Override
    public C.ClassDeclaration visitClassDeclaration(C.ClassDeclaration classDecl, P p) {
        return (C.ClassDeclaration) super.visitClassDeclaration(classDecl, p);
    }

    @Override
    public CSourceFile visitSourceFile(CSourceFile cu, P p) {
        return (CSourceFile) super.visitSourceFile(cu, p);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends C> C.ControlParentheses<T> visitControlParentheses(C.ControlParentheses<T> controlParens, P p) {
        return (C.ControlParentheses<T>) super.visitControlParentheses(controlParens, p);
    }

    @Override
    public C.Continue visitContinue(C.Continue continueStatement, P p) {
        return (C.Continue) super.visitContinue(continueStatement, p);
    }

    @Override
    public C.DoWhileLoop visitDoWhileLoop(C.DoWhileLoop doWhileLoop, P p) {
        return (C.DoWhileLoop) super.visitDoWhileLoop(doWhileLoop, p);
    }

    @Override
    public C.If.Else visitElse(C.If.Else elze, P p) {
        return (C.If.Else) super.visitElse(elze, p);
    }

    @Override
    public C.Empty visitEmpty(C.Empty empty, P p) {
        return (C.Empty) super.visitEmpty(empty, p);
    }

    @Override
    public C.EnumValue visitEnumValue(C.EnumValue _enum, P p) {
        return (C.EnumValue) super.visitEnumValue(_enum, p);
    }

    @Override
    public C.EnumValueSet visitEnumValueSet(C.EnumValueSet enums, P p) {
        return (C.EnumValueSet) super.visitEnumValueSet(enums, p);
    }

    @Override
    public C.FieldAccess visitFieldAccess(C.FieldAccess fieldAccess, P p) {
        return (C.FieldAccess) super.visitFieldAccess(fieldAccess, p);
    }

    @Override
    public C.ForEachLoop visitForEachLoop(C.ForEachLoop forLoop, P p) {
        return (C.ForEachLoop) super.visitForEachLoop(forLoop, p);
    }

    @Override
    public C.ForLoop visitForLoop(C.ForLoop forLoop, P p) {
        return (C.ForLoop) super.visitForLoop(forLoop, p);
    }

    @Override
    public C.Identifier visitIdentifier(C.Identifier identifier, P p) {
        return (C.Identifier) super.visitIdentifier(identifier, p);
    }

    @Override
    public C.If visitIf(C.If iff, P p) {
        return (C.If) super.visitIf(iff, p);
    }

    @Override
    public C.InstanceOf visitInstanceOf(C.InstanceOf instanceOf, P p) {
        return (C.InstanceOf) super.visitInstanceOf(instanceOf, p);
    }

    @Override
    public C.Label visitLabel(C.Label label, P p) {
        return (C.Label) super.visitLabel(label, p);
    }

    @Override
    public C.Lambda visitLambda(C.Lambda lambda, P p) {
        return (C.Lambda) super.visitLambda(lambda, p);
    }

    @Override
    public C.Literal visitLiteral(C.Literal literal, P p) {
        return (C.Literal) super.visitLiteral(literal, p);
    }

    @Override
    public C.MemberReference visitMemberReference(C.MemberReference memberRef, P p) {
        return (C.MemberReference) super.visitMemberReference(memberRef, p);
    }

    @Override
    public C.MethodDeclaration visitMethodDeclaration(C.MethodDeclaration method, P p) {
        return (C.MethodDeclaration) super.visitMethodDeclaration(method, p);
    }

    @Override
    public C.MethodInvocation visitMethodInvocation(C.MethodInvocation method, P p) {
        return (C.MethodInvocation) super.visitMethodInvocation(method, p);
    }

    @Override
    public C.MultiCatch visitMultiCatch(C.MultiCatch multiCatch, P p) {
        return (C.MultiCatch) super.visitMultiCatch(multiCatch, p);
    }

    @Override
    public C.VariableDeclarations visitVariableDeclarations(C.VariableDeclarations multiVariable, P p) {
        return (C.VariableDeclarations) super.visitVariableDeclarations(multiVariable, p);
    }

    @Override
    public C.NewArray visitNewArray(C.NewArray newArray, P p) {
        return (C.NewArray) super.visitNewArray(newArray, p);
    }

    @Override
    public C.NewClass visitNewClass(C.NewClass newClass, P p) {
        return (C.NewClass) super.visitNewClass(newClass, p);
    }

    @Override
    public C.ParameterizedType visitParameterizedType(C.ParameterizedType type, P p) {
        return (C.ParameterizedType) super.visitParameterizedType(type, p);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends C> C.Parentheses<T> visitParentheses(C.Parentheses<T> parens, P p) {
        return (C.Parentheses<T>) super.visitParentheses(parens, p);
    }

    @Override
    public C.Primitive visitPrimitive(C.Primitive primitive, P p) {
        return (C.Primitive) super.visitPrimitive(primitive, p);
    }

    @Override
    public C.Return visitReturn(C.Return _return, P p) {
        return (C.Return) super.visitReturn(_return, p);
    }

    @Override
    public C.Switch visitSwitch(C.Switch _switch, P p) {
        return (C.Switch) super.visitSwitch(_switch, p);
    }

    @Override
    public C.Synchronized visitSynchronized(C.Synchronized _sync, P p) {
        return (C.Synchronized) super.visitSynchronized(_sync, p);
    }

    @Override
    public C.Ternary visitTernary(C.Ternary ternary, P p) {
        return (C.Ternary) super.visitTernary(ternary, p);
    }

    @Override
    public C.Throw visitThrow(C.Throw thrown, P p) {
        return (C.Throw) super.visitThrow(thrown, p);
    }

    @Override
    public C.Try visitTry(C.Try _try, P p) {
        return (C.Try) super.visitTry(_try, p);
    }

    @Override
    public C.Try.Resource visitTryResource(C.Try.Resource tryResource, P p) {
        return (C.Try.Resource) super.visitTryResource(tryResource, p);
    }

    @Override
    public C.TypeCast visitTypeCast(C.TypeCast typeCast, P p) {
        return (C.TypeCast) super.visitTypeCast(typeCast, p);
    }

    @Override
    public C.TypeParameter visitTypeParameter(C.TypeParameter typeParam, P p) {
        return (C.TypeParameter) super.visitTypeParameter(typeParam, p);
    }

    @Override
    public C.Unary visitUnary(C.Unary unary, P p) {
        return (C.Unary) super.visitUnary(unary, p);
    }

    @Override
    public C.VariableDeclarations.NamedVariable visitVariable(C.VariableDeclarations.NamedVariable variable, P p) {
        return (C.VariableDeclarations.NamedVariable) super.visitVariable(variable, p);
    }

    @Override
    public C.WhileLoop visitWhileLoop(C.WhileLoop whileLoop, P p) {
        return (C.WhileLoop) super.visitWhileLoop(whileLoop, p);
    }

    @Override
    public C.Wildcard visitWildcard(C.Wildcard wildcard, P p) {
        return (C.Wildcard) super.visitWildcard(wildcard, p);
    }
}
