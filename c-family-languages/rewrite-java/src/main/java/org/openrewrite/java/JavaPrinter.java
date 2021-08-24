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
package org.openrewrite.java;

import org.openrewrite.PrintOutputCapture;
import org.openrewrite.family.c.tree.*;
import org.openrewrite.family.c.tree.C.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;

import java.util.Iterator;
import java.util.List;

public class JavaPrinter<P> extends JavaVisitor<PrintOutputCapture<P>> {
    protected void visitRightPadded(List<? extends CRightPadded<? extends C>> nodes, CRightPadded.Location location, String suffixBetween, PrintOutputCapture<P> p) {
        for (int i = 0; i < nodes.size(); i++) {
            CRightPadded<? extends C> node = nodes.get(i);
            visit(node.getElement(), p);
            visitSpace(node.getAfter(), location.getAfterLocation(), p);
            if (i < nodes.size() - 1) {
                p.out.append(suffixBetween);
            }
        }
    }

    protected void visitContainer(String before, @Nullable CContainer<? extends C> container, CContainer.Location location,
                                  String suffixBetween, @Nullable String after, PrintOutputCapture<P> p) {
        if (container == null) {
            return;
        }
        visitSpace(container.getBefore(), location.getBeforeLocation(), p);
        p.out.append(before);
        visitRightPadded(container.getPadding().getElements(), location.getElementLocation(), suffixBetween, p);
        p.out.append(after == null ? "" : after);
    }

    @Override
    public Space visitSpace(Space space, Space.Location loc, PrintOutputCapture<P> p) {
        p.out.append(space.getWhitespace());

        for (Comment comment : space.getComments()) {
            visitMarkers(comment.getMarkers(), p);
            p.out.append(comment.printComment());
            p.out.append(comment.getSuffix());
        }
        return space;
    }

    protected void visitLeftPadded(@Nullable String prefix, @Nullable CLeftPadded<? extends C> leftPadded, CLeftPadded.Location location, PrintOutputCapture<P> p) {
        if (leftPadded != null) {
            visitSpace(leftPadded.getBefore(), location.getBeforeLocation(), p);
            if (prefix != null) {
                p.out.append(prefix);
            }
            visit(leftPadded.getElement(), p);
        }
    }

    protected void visitRightPadded(@Nullable CRightPadded<? extends C> rightPadded, CRightPadded.Location location, @Nullable String suffix, PrintOutputCapture<P> p) {
        if (rightPadded != null) {
            visit(rightPadded.getElement(), p);
            visitSpace(rightPadded.getAfter(), location.getAfterLocation(), p);
            if (suffix != null) {
                p.out.append(suffix);
            }
        }
    }

    protected void visitModifiers(Iterable<C.Modifier> modifiers, PrintOutputCapture<P> p) {
        for (C.Modifier mod : modifiers) {
            visit(mod.getAnnotations(), p);
            String keyword = "";
            switch (mod.getType()) {
                case Default:
                    keyword = "default";
                    break;
                case Public:
                    keyword = "public";
                    break;
                case Protected:
                    keyword = "protected";
                    break;
                case Private:
                    keyword = "private";
                    break;
                case Abstract:
                    keyword = "abstract";
                    break;
                case Static:
                    keyword = "static";
                    break;
                case Final:
                    keyword = "final";
                    break;
                case Native:
                    keyword = "native";
                    break;
                case Strictfp:
                    keyword = "strictfp";
                    break;
                case Synchronized:
                    keyword = "synchronized";
                    break;
                case Transient:
                    keyword = "transient";
                    break;
                case Volatile:
                    keyword = "volatile";
                    break;
            }
            visitSpace(mod.getPrefix(), Space.Location.MODIFIER_PREFIX, p);
            visitMarkers(mod.getMarkers(), p);

            p.out.append(keyword);
        }
    }

    @Override
    public C visitAnnotation(C.Annotation annotation, PrintOutputCapture<P> p) {
        visitSpace(annotation.getPrefix(), Space.Location.ANNOTATION_PREFIX, p);
        visitMarkers(annotation.getMarkers(), p);
        p.out.append("@");
        visit(annotation.getAnnotationType(), p);
        visitContainer("(", annotation.getPadding().getArguments(), CContainer.Location.ANNOTATION_ARGUMENTS, ",", ")", p);
        return annotation;
    }

    @Override
    public C visitAnnotatedType(AnnotatedType annotatedType, PrintOutputCapture<P> p) {
        visitSpace(annotatedType.getPrefix(), Space.Location.ANNOTATED_TYPE_PREFIX, p);
        visitMarkers(annotatedType.getMarkers(), p);
        visit(annotatedType.getAnnotations(), p);
        visit(annotatedType.getTypeExpression(), p);
        return annotatedType;
    }

    @Override
    public C visitArrayDimension(ArrayDimension arrayDimension, PrintOutputCapture<P> p) {
        visitSpace(arrayDimension.getPrefix(), Space.Location.DIMENSION_PREFIX, p);
        visitMarkers(arrayDimension.getMarkers(), p);
        p.out.append("[");
        visitRightPadded(arrayDimension.getPadding().getIndex(), CRightPadded.Location.ARRAY_INDEX, "]", p);
        return arrayDimension;
    }

    @Override
    public C visitArrayType(ArrayType arrayType, PrintOutputCapture<P> p) {
        visitSpace(arrayType.getPrefix(), Space.Location.ARRAY_TYPE_PREFIX, p);
        visitMarkers(arrayType.getMarkers(), p);
        visit(arrayType.getElementType(), p);
        for (CRightPadded<Space> d : arrayType.getDimensions()) {
            visitSpace(d.getElement(), Space.Location.DIMENSION, p);
            p.out.append('[');
            visitSpace(d.getAfter(), Space.Location.DIMENSION_SUFFIX, p);
            p.out.append(']');
        }
        return arrayType;
    }

    @Override
    public C visitAssert(Assert azzert, PrintOutputCapture<P> p) {
        visitSpace(azzert.getPrefix(), Space.Location.ASSERT_PREFIX, p);
        visitMarkers(azzert.getMarkers(), p);
        p.out.append("assert");
        visit(azzert.getCondition(), p);
        visitLeftPadded(":", azzert.getDetail(), CLeftPadded.Location.ASSERT_DETAIL, p);
        return azzert;
    }

    @Override
    public C visitAssignment(Assignment assignment, PrintOutputCapture<P> p) {
        visitSpace(assignment.getPrefix(), Space.Location.ASSIGNMENT_PREFIX, p);
        visitMarkers(assignment.getMarkers(), p);
        visit(assignment.getVariable(), p);
        visitLeftPadded("=", assignment.getPadding().getAssignment(), CLeftPadded.Location.ASSIGNMENT, p);
        return assignment;
    }

    @Override
    public C visitAssignmentOperation(AssignmentOperation assignOp, PrintOutputCapture<P> p) {
        String keyword = "";
        switch (assignOp.getOperator()) {
            case Addition:
                keyword = "+=";
                break;
            case Subtraction:
                keyword = "-=";
                break;
            case Multiplication:
                keyword = "*=";
                break;
            case Division:
                keyword = "/=";
                break;
            case Modulo:
                keyword = "%=";
                break;
            case BitAnd:
                keyword = "&=";
                break;
            case BitOr:
                keyword = "|=";
                break;
            case BitXor:
                keyword = "^=";
                break;
            case LeftShift:
                keyword = "<<=";
                break;
            case RightShift:
                keyword = ">>=";
                break;
            case UnsignedRightShift:
                keyword = ">>>=";
                break;
        }
        visitSpace(assignOp.getPrefix(), Space.Location.ASSIGNMENT_OPERATION_PREFIX, p);
        visitMarkers(assignOp.getMarkers(), p);
        visit(assignOp.getVariable(), p);
        visitSpace(assignOp.getPadding().getOperator().getBefore(), Space.Location.ASSIGNMENT_OPERATION_OPERATOR, p);
        p.out.append(keyword);
        visit(assignOp.getAssignment(), p);
        return assignOp;
    }

    @Override
    public C visitBinary(Binary binary, PrintOutputCapture<P> p) {
        String keyword = "";
        switch (binary.getOperator()) {
            case Addition:
                keyword = "+";
                break;
            case Subtraction:
                keyword = "-";
                break;
            case Multiplication:
                keyword = "*";
                break;
            case Division:
                keyword = "/";
                break;
            case Modulo:
                keyword = "%";
                break;
            case LessThan:
                keyword = "<";
                break;
            case GreaterThan:
                keyword = ">";
                break;
            case LessThanOrEqual:
                keyword = "<=";
                break;
            case GreaterThanOrEqual:
                keyword = ">=";
                break;
            case Equal:
                keyword = "==";
                break;
            case NotEqual:
                keyword = "!=";
                break;
            case BitAnd:
                keyword = "&";
                break;
            case BitOr:
                keyword = "|";
                break;
            case BitXor:
                keyword = "^";
                break;
            case LeftShift:
                keyword = "<<";
                break;
            case RightShift:
                keyword = ">>";
                break;
            case UnsignedRightShift:
                keyword = ">>>";
                break;
            case Or:
                keyword = "||";
                break;
            case And:
                keyword = "&&";
                break;
        }
        visitSpace(binary.getPrefix(), Space.Location.BINARY_PREFIX, p);
        visitMarkers(binary.getMarkers(), p);
        visit(binary.getLeft(), p);
        visitSpace(binary.getPadding().getOperator().getBefore(), Space.Location.BINARY_OPERATOR, p);
        p.out.append(keyword);
        visit(binary.getRight(), p);
        return binary;
    }

    @Override
    public C visitBlock(Block block, PrintOutputCapture<P> p) {
        visitSpace(block.getPrefix(), Space.Location.BLOCK_PREFIX, p);
        visitMarkers(block.getMarkers(), p);


        if (block.isStatic()) {
            p.out.append("static");
            visitRightPadded(block.getPadding().getStatic(), CRightPadded.Location.STATIC_INIT, p);
        }

        p.out.append('{');
        visitStatements(block.getPadding().getStatements(), CRightPadded.Location.BLOCK_STATEMENT, p);
        visitSpace(block.getEnd(), Space.Location.BLOCK_END, p);
        p.out.append('}');
        return block;
    }

    protected void visitStatements(List<CRightPadded<Statement>> statements, CRightPadded.Location location, PrintOutputCapture<P> p) {
        for (CRightPadded<Statement> paddedStat : statements) {
            visitStatement(paddedStat, location, p);
        }
    }

    protected void visitStatement(@Nullable CRightPadded<Statement> paddedStat, CRightPadded.Location location, PrintOutputCapture<P> p) {
        if (paddedStat == null) {
            return;
        }

        visit(paddedStat.getElement(), p);
        visitSpace(paddedStat.getAfter(), location.getAfterLocation(), p);

        Statement s = paddedStat.getElement();
        while (true) {
            if (s instanceof Assert ||
                    s instanceof Assignment ||
                    s instanceof AssignmentOperation ||
                    s instanceof Break ||
                    s instanceof Continue ||
                    s instanceof DoWhileLoop ||
                    s instanceof Empty ||
                    s instanceof MethodInvocation ||
                    s instanceof NewClass ||
                    s instanceof Return ||
                    s instanceof Throw ||
                    s instanceof Unary ||
                    s instanceof VariableDeclarations) {
                p.out.append(';');
                return;
            }

            if (s instanceof MethodDeclaration && ((MethodDeclaration) s).getBody() == null) {
                p.out.append(';');
                return;
            }

            if (s instanceof Label) {
                s = ((Label) s).getStatement();
                continue;
            }
            return;
        }
    }

    @Override
    public C visitBreak(Break breakStatement, PrintOutputCapture<P> p) {
        visitSpace(breakStatement.getPrefix(), Space.Location.BREAK_PREFIX, p);
        visitMarkers(breakStatement.getMarkers(), p);
        p.out.append("break");
        visit(breakStatement.getLabel(), p);
        return breakStatement;
    }

    @Override
    public C visitCase(Case caze, PrintOutputCapture<P> p) {
        visitSpace(caze.getPrefix(), Space.Location.CASE_PREFIX, p);
        visitMarkers(caze.getMarkers(), p);
        Expression elem = caze.getPattern();
        if (elem instanceof Identifier && ((Identifier) elem).getSimpleName().equals("default")) {
            p.out.append("default");
        } else {
            p.out.append("case");
            visit(elem, p);
        }
        visitSpace(caze.getPadding().getStatements().getBefore(), Space.Location.CASE, p);
        p.out.append(':');
        visitStatements(caze.getPadding().getStatements().getPadding().getElements(), CRightPadded.Location.CASE, p);
        return caze;
    }

    @Override
    public C visitCatch(Try.Catch catzh, PrintOutputCapture<P> p) {
        visitSpace(catzh.getPrefix(), Space.Location.CATCH_PREFIX, p);
        visitMarkers(catzh.getMarkers(), p);
        p.out.append("catch");
        visit(catzh.getParameter(), p);
        visit(catzh.getBody(), p);
        return catzh;
    }

    @Override
    public C visitClassDeclaration(ClassDeclaration classDecl, PrintOutputCapture<P> p) {
        String kind = "";
        switch (classDecl.getKind()) {
            case Class:
                kind = "class";
                break;
            case Enum:
                kind = "enum";
                break;
            case Interface:
                kind = "interface";
                break;
            case Annotation:
                kind = "@interface";
                break;
        }

        visitSpace(classDecl.getPrefix(), Space.Location.CLASS_DECLARATION_PREFIX, p);
        visitMarkers(classDecl.getMarkers(), p);
        visitSpace(Space.EMPTY, Space.Location.ANNOTATIONS, p);
        visit(classDecl.getLeadingAnnotations(), p);
        visitModifiers(classDecl.getModifiers(), p);
        visit(classDecl.getAnnotations().getKind().getAnnotations(), p);
        visitSpace(classDecl.getAnnotations().getKind().getPrefix(), Space.Location.CLASS_KIND, p);
        p.out.append(kind);
        visit(classDecl.getName(), p);
        visitContainer("<", classDecl.getPadding().getTypeParameters(), CContainer.Location.TYPE_PARAMETERS, ",", ">", p);
        visitLeftPadded("extends", classDecl.getPadding().getExtends(), CLeftPadded.Location.EXTENDS, p);
        visitContainer(classDecl.getKind().equals(ClassDeclaration.Kind.Type.Interface) ? "extends" : "implements",
                classDecl.getPadding().getImplements(), CContainer.Location.IMPLEMENTS, ",", null, p);
        visit(classDecl.getBody(), p);
        return classDecl;
    }

    @Override
    public J visitCompilationUnit(J.CompilationUnit cu, PrintOutputCapture<P> p) {
        visitSpace(cu.getPrefix(), Space.Location.COMPILATION_UNIT_PREFIX, p);
        visitMarkers(cu.getMarkers(), p);
        visitRightPadded(cu.getPadding().getPackageDeclaration(), CRightPadded.Location.PACKAGE, ";", p);
        visitRightPadded(cu.getPadding().getImports(), CRightPadded.Location.IMPORT, ";", p);
        if (!cu.getImports().isEmpty()) {
            p.out.append(";");
        }
        visit(cu.getClasses(), p);
        visitSpace(cu.getEof(), Space.Location.COMPILATION_UNIT_EOF, p);
        return cu;
    }

    @Override
    public C visitContinue(Continue continueStatement, PrintOutputCapture<P> p) {
        visitSpace(continueStatement.getPrefix(), Space.Location.CONTINUE_PREFIX, p);
        visitMarkers(continueStatement.getMarkers(), p);
        p.out.append("continue");
        visit(continueStatement.getLabel(), p);
        return continueStatement;
    }

    @Override
    public <T extends C> C visitControlParentheses(ControlParentheses<T> controlParens, PrintOutputCapture<P> p) {
        visitSpace(controlParens.getPrefix(), Space.Location.CONTROL_PARENTHESES_PREFIX, p);
        visitMarkers(controlParens.getMarkers(), p);
        p.out.append('(');
        visitRightPadded(controlParens.getPadding().getTree(), CRightPadded.Location.PARENTHESES, ")", p);
        return controlParens;
    }

    @Override
    public C visitDoWhileLoop(DoWhileLoop doWhileLoop, PrintOutputCapture<P> p) {
        visitSpace(doWhileLoop.getPrefix(), Space.Location.DO_WHILE_PREFIX, p);
        visitMarkers(doWhileLoop.getMarkers(), p);
        p.out.append("do");
        visitStatement(doWhileLoop.getPadding().getBody(), CRightPadded.Location.WHILE_BODY, p);
        visitLeftPadded("while", doWhileLoop.getPadding().getWhileCondition(), CLeftPadded.Location.WHILE_CONDITION, p);
        return doWhileLoop;
    }

    @Override
    public C visitElse(If.Else elze, PrintOutputCapture<P> p) {
        visitSpace(elze.getPrefix(), Space.Location.ELSE_PREFIX, p);
        visitMarkers(elze.getMarkers(), p);
        p.out.append("else");
        visitStatement(elze.getPadding().getBody(), CRightPadded.Location.IF_ELSE, p);
        return elze;
    }

    @Override
    public C visitEnumValue(EnumValue enoom, PrintOutputCapture<P> p) {
        visitSpace(enoom.getPrefix(), Space.Location.ENUM_VALUE_PREFIX, p);
        visitMarkers(enoom.getMarkers(), p);
        visit(enoom.getAnnotations(), p);
        visit(enoom.getName(), p);
        NewClass initializer = enoom.getInitializer();
        if (enoom.getInitializer() != null) {
            visitSpace(initializer.getPrefix(), Space.Location.NEW_CLASS_PREFIX, p);
            visitSpace(initializer.getNew(), Space.Location.NEW_PREFIX, p);
            visitContainer("(", initializer.getPadding().getArguments(), CContainer.Location.NEW_CLASS_ARGUMENTS, ",", ")", p);
            visit(initializer.getBody(), p);
        }
        return enoom;
    }

    @Override
    public C visitEnumValueSet(EnumValueSet enums, PrintOutputCapture<P> p) {
        visitSpace(enums.getPrefix(), Space.Location.ENUM_VALUE_SET_PREFIX, p);
        visitMarkers(enums.getMarkers(), p);
        visitRightPadded(enums.getPadding().getEnums(), CRightPadded.Location.ENUM_VALUE, ",", p);
        if (enums.isTerminatedWithSemicolon()) {
            p.out.append(';');
        }
        return enums;
    }

    @Override
    public C visitFieldAccess(FieldAccess fieldAccess, PrintOutputCapture<P> p) {
        visitSpace(fieldAccess.getPrefix(), Space.Location.FIELD_ACCESS_PREFIX, p);
        visitMarkers(fieldAccess.getMarkers(), p);
        visit(fieldAccess.getTarget(), p);
        visitLeftPadded(".", fieldAccess.getPadding().getName(), CLeftPadded.Location.FIELD_ACCESS_NAME, p);
        return fieldAccess;
    }

    @Override
    public C visitForLoop(ForLoop forLoop, PrintOutputCapture<P> p) {
        visitSpace(forLoop.getPrefix(), Space.Location.FOR_PREFIX, p);
        visitMarkers(forLoop.getMarkers(), p);
        p.out.append("for");
        ForLoop.Control ctrl = forLoop.getControl();
        visitSpace(ctrl.getPrefix(), Space.Location.FOR_CONTROL_PREFIX, p);
        p.out.append('(');
        visitRightPadded(ctrl.getPadding().getInit(), CRightPadded.Location.FOR_INIT, ",", p);
        p.out.append(';');
        visitRightPadded(ctrl.getPadding().getCondition(), CRightPadded.Location.FOR_CONDITION, ";", p);
        visitRightPadded(ctrl.getPadding().getUpdate(), CRightPadded.Location.FOR_UPDATE, ",", p);
        p.out.append(')');
        visitStatement(forLoop.getPadding().getBody(), CRightPadded.Location.FOR_BODY, p);
        return forLoop;
    }

    @Override
    public C visitForEachLoop(ForEachLoop forEachLoop, PrintOutputCapture<P> p) {
        visitSpace(forEachLoop.getPrefix(), Space.Location.FOR_EACH_LOOP_PREFIX, p);
        visitMarkers(forEachLoop.getMarkers(), p);
        p.out.append("for");
        ForEachLoop.Control ctrl = forEachLoop.getControl();
        visitSpace(ctrl.getPrefix(), Space.Location.FOR_EACH_CONTROL_PREFIX, p);
        p.out.append('(');
        visitRightPadded(ctrl.getPadding().getVariable(), CRightPadded.Location.FOREACH_VARIABLE, ":", p);
        visitRightPadded(ctrl.getPadding().getIterable(), CRightPadded.Location.FOREACH_ITERABLE, "", p);
        p.out.append(')');
        visitStatement(forEachLoop.getPadding().getBody(), CRightPadded.Location.FOR_BODY, p);
        return forEachLoop;
    }

    @Override
    public C visitIdentifier(Identifier ident, PrintOutputCapture<P> p) {
        visitSpace(ident.getPrefix(), Space.Location.IDENTIFIER_PREFIX, p);
        visitMarkers(ident.getMarkers(), p);
        p.out.append(ident.getSimpleName());
        return ident;
    }

    @Override
    public C visitIf(If iff, PrintOutputCapture<P> p) {
        visitSpace(iff.getPrefix(), Space.Location.IF_PREFIX, p);
        visitMarkers(iff.getMarkers(), p);
        p.out.append("if");
        visit(iff.getIfCondition(), p);
        visitStatement(iff.getPadding().getThenPart(), CRightPadded.Location.IF_THEN, p);
        visit(iff.getElsePart(), p);
        return iff;
    }

    @Override
    public J visitImport(J.Import impoort, PrintOutputCapture<P> p) {
        visitSpace(impoort.getPrefix(), Space.Location.IMPORT_PREFIX, p);
        visitMarkers(impoort.getMarkers(), p);
        p.out.append("import");
        if (impoort.isStatic()) {
            visitSpace(impoort.getPadding().getStatic().getBefore(), Space.Location.STATIC_IMPORT, p);
            p.out.append("static");
        }
        visit(impoort.getQualid(), p);
        return impoort;
    }

    @Override
    public C visitInstanceOf(InstanceOf instanceOf, PrintOutputCapture<P> p) {
        visitSpace(instanceOf.getPrefix(), Space.Location.INSTANCEOF_PREFIX, p);
        visitMarkers(instanceOf.getMarkers(), p);
        visitRightPadded(instanceOf.getPadding().getExpr(), CRightPadded.Location.INSTANCEOF, "instanceof", p);
        visit(instanceOf.getClazz(), p);
        return instanceOf;
    }

    @Override
    public C visitLabel(Label label, PrintOutputCapture<P> p) {
        visitSpace(label.getPrefix(), Space.Location.LABEL_PREFIX, p);
        visitMarkers(label.getMarkers(), p);
        visitRightPadded(label.getPadding().getLabel(), CRightPadded.Location.LABEL, ":", p);
        visit(label.getStatement(), p);
        return label;
    }

    @Override
    public C visitLambda(Lambda lambda, PrintOutputCapture<P> p) {
        visitSpace(lambda.getPrefix(), Space.Location.LAMBDA_PREFIX, p);
        visitMarkers(lambda.getMarkers(), p);
        visitSpace(lambda.getParameters().getPrefix(), Space.Location.LAMBDA_PARAMETERS_PREFIX, p);
        visitMarkers(lambda.getParameters().getMarkers(), p);
        if (lambda.getParameters().isParenthesized()) {
            p.out.append('(');
            visitRightPadded(lambda.getParameters().getPadding().getParams(), CRightPadded.Location.LAMBDA_PARAM, ",", p);
            p.out.append(')');
        } else {
            visitRightPadded(lambda.getParameters().getPadding().getParams(), CRightPadded.Location.LAMBDA_PARAM, ",", p);
        }
        visitSpace(lambda.getArrow(), Space.Location.LAMBDA_ARROW_PREFIX, p);
        p.out.append("->");
        visit(lambda.getBody(), p);
        return lambda;
    }

    @Override
    public C visitLiteral(Literal literal, PrintOutputCapture<P> p) {
        visitSpace(literal.getPrefix(), Space.Location.LITERAL_PREFIX, p);
        visitMarkers(literal.getMarkers(), p);
        List<Literal.UnicodeEscape> unicodeEscapes = literal.getUnicodeEscapes();
        if (unicodeEscapes == null) {
            p.out.append(literal.getValueSource());
        } else if (literal.getValueSource() != null) {
            Iterator<Literal.UnicodeEscape> surrogateIter = unicodeEscapes.iterator();
            Literal.UnicodeEscape surrogate = surrogateIter.hasNext() ?
                    surrogateIter.next() : null;
            int i = 0;
            if (surrogate != null && surrogate.getValueSourceIndex() == 0) {
                p.out.append("\\u").append(surrogate.getCodePoint());
                if (surrogateIter.hasNext()) {
                    surrogate = surrogateIter.next();
                }
            }

            char[] valueSourceArr = literal.getValueSource().toCharArray();
            for (char c : valueSourceArr) {
                p.out.append(c);
                if (surrogate != null && surrogate.getValueSourceIndex() == ++i) {
                    p.out.append("\\u").append(surrogate.getCodePoint());
                    if (surrogateIter.hasNext()) {
                        surrogate = surrogateIter.next();
                    }
                }
            }
        }
        return literal;
    }

    @Override
    public C visitMemberReference(MemberReference memberRef, PrintOutputCapture<P> p) {
        visitSpace(memberRef.getPrefix(), Space.Location.MEMBER_REFERENCE_PREFIX, p);
        visitMarkers(memberRef.getMarkers(), p);
        visitRightPadded(memberRef.getPadding().getContaining(), CRightPadded.Location.MEMBER_REFERENCE_CONTAINING, p);
        p.out.append("::");
        visitContainer("<", memberRef.getPadding().getTypeParameters(), CContainer.Location.TYPE_PARAMETERS, ",", ">", p);
        visitLeftPadded("", memberRef.getPadding().getReference(), CLeftPadded.Location.MEMBER_REFERENCE_NAME, p);
        return memberRef;
    }

    @Override
    public C visitMethodDeclaration(MethodDeclaration method, PrintOutputCapture<P> p) {
        visitSpace(method.getPrefix(), Space.Location.METHOD_DECLARATION_PREFIX, p);
        visitMarkers(method.getMarkers(), p);
        visitSpace(Space.EMPTY, Space.Location.ANNOTATIONS, p);
        visit(method.getLeadingAnnotations(), p);
        visitModifiers(method.getModifiers(), p);
        TypeParameters typeParameters = method.getAnnotations().getTypeParameters();
        if (typeParameters != null) {
            visit(typeParameters.getAnnotations(), p);
            visitSpace(typeParameters.getPrefix(), Space.Location.TYPE_PARAMETERS, p);
            visitMarkers(typeParameters.getMarkers(), p);
            p.out.append("<");
            visitRightPadded(typeParameters.getPadding().getTypeParameters(), CRightPadded.Location.TYPE_PARAMETER, ",", p);
            p.out.append(">");
        }
        visit(method.getReturnTypeExpression(), p);
        visit(method.getAnnotations().getName().getAnnotations(), p);
        visit(method.getName(), p);
        visitContainer("(", method.getPadding().getParameters(), CContainer.Location.METHOD_DECLARATION_PARAMETERS, ",", ")", p);
        visitContainer("throws", method.getPadding().getThrows(), CContainer.Location.THROWS, ",", null, p);
        visit(method.getBody(), p);
        visitLeftPadded("default", method.getPadding().getDefaultValue(), CLeftPadded.Location.METHOD_DECLARATION_DEFAULT_VALUE, p);
        return method;
    }

    @Override
    public C visitMethodInvocation(MethodInvocation method, PrintOutputCapture<P> p) {
        visitSpace(method.getPrefix(), Space.Location.METHOD_INVOCATION_PREFIX, p);
        visitMarkers(method.getMarkers(), p);
        visitRightPadded(method.getPadding().getSelect(), CRightPadded.Location.METHOD_SELECT, ".", p);
        visitContainer("<", method.getPadding().getTypeParameters(), CContainer.Location.TYPE_PARAMETERS, ",", ">", p);
        visit(method.getName(), p);
        visitContainer("(", method.getPadding().getArguments(), CContainer.Location.METHOD_INVOCATION_ARGUMENTS, ",", ")", p);
        return method;
    }

    @Override
    public C visitMultiCatch(MultiCatch multiCatch, PrintOutputCapture<P> p) {
        visitSpace(multiCatch.getPrefix(), Space.Location.MULTI_CATCH_PREFIX, p);
        visitMarkers(multiCatch.getMarkers(), p);
        visitRightPadded(multiCatch.getPadding().getAlternatives(), CRightPadded.Location.CATCH_ALTERNATIVE, "|", p);
        return multiCatch;
    }

    @Override
    public C visitVarType(VarType varType, PrintOutputCapture<P> p) {
        visitSpace(varType.getPrefix(), Space.Location.VAR_KEYWORD, p);
        visitMarkers(varType.getMarkers(), p);
        p.out.append("var");
        return varType;
    }

    @Override
    public C visitVariableDeclarations(VariableDeclarations multiVariable, PrintOutputCapture<P> p) {
        visitSpace(multiVariable.getPrefix(), Space.Location.VARIABLE_DECLARATIONS_PREFIX, p);
        visitMarkers(multiVariable.getMarkers(), p);
        visitSpace(Space.EMPTY, Space.Location.ANNOTATIONS, p);
        visit(multiVariable.getLeadingAnnotations(), p);
        visitModifiers(multiVariable.getModifiers(), p);
        visit(multiVariable.getTypeExpression(), p);
        for (CLeftPadded<Space> dim : multiVariable.getDimensionsBeforeName()) {
            visitSpace(dim.getBefore(), Space.Location.DIMENSION_PREFIX, p);
            p.out.append('[');
            visitSpace(dim.getElement(), Space.Location.DIMENSION, p);
            p.out.append(']');
        }
        if (multiVariable.getVarargs() != null) {
            visitSpace(multiVariable.getVarargs(), Space.Location.VARARGS, p);
            p.out.append("...");
        }
        visitRightPadded(multiVariable.getPadding().getVariables(), CRightPadded.Location.NAMED_VARIABLE, ",", p);
        return multiVariable;
    }

    @Override
    public C visitNewArray(NewArray newArray, PrintOutputCapture<P> p) {
        visitSpace(newArray.getPrefix(), Space.Location.NEW_ARRAY_PREFIX, p);
        visitMarkers(newArray.getMarkers(), p);
        if (newArray.getTypeExpression() != null) {
            p.out.append("new");
        }
        visit(newArray.getTypeExpression(), p);
        visit(newArray.getDimensions(), p);
        visitContainer("{", newArray.getPadding().getInitializer(), CContainer.Location.NEW_ARRAY_INITIALIZER, ",", "}", p);
        return newArray;
    }

    @Override
    public C visitNewClass(NewClass newClass, PrintOutputCapture<P> p) {
        visitSpace(newClass.getPrefix(), Space.Location.NEW_CLASS_PREFIX, p);
        visitMarkers(newClass.getMarkers(), p);
        visitRightPadded(newClass.getPadding().getEnclosing(), CRightPadded.Location.NEW_CLASS_ENCLOSING, ".", p);
        visitSpace(newClass.getNew(), Space.Location.NEW_PREFIX, p);
        p.out.append("new");
        visit(newClass.getClazz(), p);
        visitContainer("(", newClass.getPadding().getArguments(), CContainer.Location.NEW_CLASS_ARGUMENTS, ",", ")", p);
        visit(newClass.getBody(), p);
        return newClass;
    }

    @Override
    public J visitPackage(J.Package pkg, PrintOutputCapture<P> p) {
        pkg.getAnnotations().forEach(a -> visitAnnotation(a, p));
        visitSpace(pkg.getPrefix(), Space.Location.PACKAGE_PREFIX, p);
        visitMarkers(pkg.getMarkers(), p);
        p.out.append("package");
        visit(pkg.getExpression(), p);
        return pkg;
    }

    @Override
    public C visitParameterizedType(ParameterizedType type, PrintOutputCapture<P> p) {
        visitSpace(type.getPrefix(), Space.Location.PARAMETERIZED_TYPE_PREFIX, p);
        visitMarkers(type.getMarkers(), p);
        visit(type.getClazz(), p);
        visitContainer("<", type.getPadding().getTypeParameters(), CContainer.Location.TYPE_PARAMETERS, ",", ">", p);
        return type;
    }

    @Override
    public C visitPrimitive(Primitive primitive, PrintOutputCapture<P> p) {
        String keyword;
        switch (primitive.getType()) {
            case Boolean:
                keyword = "boolean";
                break;
            case Byte:
                keyword = "byte";
                break;
            case Char:
                keyword = "char";
                break;
            case Double:
                keyword = "double";
                break;
            case Float:
                keyword = "float";
                break;
            case Int:
                keyword = "int";
                break;
            case Long:
                keyword = "long";
                break;
            case Short:
                keyword = "short";
                break;
            case Void:
                keyword = "void";
                break;
            case String:
                keyword = "String";
                break;
            case Wildcard:
                keyword = "*";
                break;
            case None:
                throw new IllegalStateException("Unable to print None primitive");
            case Null:
                throw new IllegalStateException("Unable to print Null primitive");
            default:
                throw new IllegalStateException("Unable to print non-primitive type");
        }
        visitSpace(primitive.getPrefix(), Space.Location.PRIMITIVE_PREFIX, p);
        visitMarkers(primitive.getMarkers(), p);
        p.out.append(keyword);
        return primitive;
    }

    @Override
    public <T extends C> C visitParentheses(Parentheses<T> parens, PrintOutputCapture<P> p) {
        visitSpace(parens.getPrefix(), Space.Location.PARENTHESES_PREFIX, p);
        visitMarkers(parens.getMarkers(), p);
        p.out.append("(");
        visitRightPadded(parens.getPadding().getTree(), CRightPadded.Location.PARENTHESES, ")", p);
        return parens;
    }

    @Override
    public C visitReturn(Return retrn, PrintOutputCapture<P> p) {
        visitSpace(retrn.getPrefix(), Space.Location.RETURN_PREFIX, p);
        visitMarkers(retrn.getMarkers(), p);
        p.out.append("return");
        visit(retrn.getExpression(), p);
        return retrn;
    }

    @Override
    public C visitSwitch(Switch switzh, PrintOutputCapture<P> p) {
        visitSpace(switzh.getPrefix(), Space.Location.SWITCH_PREFIX, p);
        visitMarkers(switzh.getMarkers(), p);
        p.out.append("switch");
        visit(switzh.getSelector(), p);
        visit(switzh.getCases(), p);
        return switzh;
    }

    @Override
    public C visitSynchronized(J.Synchronized synch, PrintOutputCapture<P> p) {
        visitSpace(synch.getPrefix(), Space.Location.SYNCHRONIZED_PREFIX, p);
        visitMarkers(synch.getMarkers(), p);
        p.out.append("synchronized");
        visit(synch.getLock(), p);
        visit(synch.getBody(), p);
        return synch;
    }

    @Override
    public C visitTernary(Ternary ternary, PrintOutputCapture<P> p) {
        visitSpace(ternary.getPrefix(), Space.Location.TERNARY_PREFIX, p);
        visitMarkers(ternary.getMarkers(), p);
        visit(ternary.getCondition(), p);
        visitLeftPadded("?", ternary.getPadding().getTruePart(), CLeftPadded.Location.TERNARY_TRUE, p);
        visitLeftPadded(":", ternary.getPadding().getFalsePart(), CLeftPadded.Location.TERNARY_FALSE, p);
        return ternary;
    }

    @Override
    public C visitThrow(Throw thrown, PrintOutputCapture<P> p) {
        visitSpace(thrown.getPrefix(), Space.Location.THROW_PREFIX, p);
        visitMarkers(thrown.getMarkers(), p);
        p.out.append("throw");
        visit(thrown.getException(), p);
        return thrown;
    }

    @Override
    public C visitTry(Try tryable, PrintOutputCapture<P> p) {
        visitSpace(tryable.getPrefix(), Space.Location.TRY_PREFIX, p);
        visitMarkers(tryable.getMarkers(), p);
        p.out.append("try");
        if (tryable.getPadding().getResources() != null) {
            //Note: we do not call visitContainer here because the last resource may or may not be semicolon terminated.
            //      Doing this means that visitTryResource is not called, therefore this logic must visit the resources.
            visitSpace(tryable.getPadding().getResources().getBefore(), Space.Location.TRY_RESOURCES, p);
            p.out.append('(');
            List<CRightPadded<Try.Resource>> resources = tryable.getPadding().getResources().getPadding().getElements();
            for (int i = 0; i < resources.size(); i++) {
                CRightPadded<Try.Resource> resource = resources.get(i);

                visitSpace(resource.getElement().getPrefix(), Space.Location.TRY_RESOURCE, p);
                visitMarkers(resource.getElement().getMarkers(), p);
                visit(resource.getElement().getVariableDeclarations(), p);

                if (i < resources.size() - 1 || resource.getElement().isTerminatedWithSemicolon()) {
                    p.out.append(';');
                }

                visitSpace(resource.getAfter(), Space.Location.TRY_RESOURCE_SUFFIX, p);
            }
            p.out.append(')');
        }

        visit(tryable.getBody(), p);
        visit(tryable.getCatches(), p);
        visitLeftPadded("finally", tryable.getPadding().getFinally(), CLeftPadded.Location.TRY_FINALLY, p);
        return tryable;
    }

    @Override
    public C visitTypeParameter(TypeParameter typeParam, PrintOutputCapture<P> p) {
        visitSpace(typeParam.getPrefix(), Space.Location.TYPE_PARAMETERS_PREFIX, p);
        visitMarkers(typeParam.getMarkers(), p);
        visit(typeParam.getAnnotations(), p);
        visit(typeParam.getName(), p);
        visitContainer("extends", typeParam.getPadding().getBounds(), CContainer.Location.TYPE_BOUNDS, "&", "", p);
        return typeParam;
    }

    @Override
    public C visitUnary(Unary unary, PrintOutputCapture<P> p) {
        visitSpace(unary.getPrefix(), Space.Location.UNARY_PREFIX, p);
        visitMarkers(unary.getMarkers(), p);
        switch (unary.getOperator()) {
            case PreIncrement:
                p.out.append("++");
                visit(unary.getExpression(), p);
                break;
            case PreDecrement:
                p.out.append("--");
                visit(unary.getExpression(), p);
                break;
            case PostIncrement:
                visit(unary.getExpression(), p);
                visitSpace(unary.getPadding().getOperator().getBefore(), Space.Location.UNARY_OPERATOR, p);
                p.out.append("++");
                break;
            case PostDecrement:
                visit(unary.getExpression(), p);
                visitSpace(unary.getPadding().getOperator().getBefore(), Space.Location.UNARY_OPERATOR, p);
                p.out.append("--");
                break;
            case Positive:
                p.out.append("+");
                visit(unary.getExpression(), p);
                break;
            case Negative:
                p.out.append("-");
                visit(unary.getExpression(), p);
                break;
            case Complement:
                p.out.append("~");
                visit(unary.getExpression(), p);
                break;
            case Not:
            default:
                p.out.append("!");
                visit(unary.getExpression(), p);
        }
        return unary;
    }

    @Override
    public C visitVariable(VariableDeclarations.NamedVariable variable, PrintOutputCapture<P> p) {
        visitSpace(variable.getPrefix(), Space.Location.VARIABLE_PREFIX, p);
        visitMarkers(variable.getMarkers(), p);
        visit(variable.getName(), p);
        for (CLeftPadded<Space> dimension : variable.getDimensionsAfterName()) {
            visitSpace(dimension.getBefore(), Space.Location.DIMENSION_PREFIX, p);
            p.out.append('[');
            visitSpace(dimension.getElement(), Space.Location.DIMENSION, p);
            p.out.append(']');
        }
        visitLeftPadded("=", variable.getPadding().getInitializer(), CLeftPadded.Location.VARIABLE_INITIALIZER, p);
        return variable;
    }

    @Override
    public C visitWhileLoop(WhileLoop whileLoop, PrintOutputCapture<P> p) {
        visitSpace(whileLoop.getPrefix(), Space.Location.WHILE_PREFIX, p);
        visitMarkers(whileLoop.getMarkers(), p);
        p.out.append("while");
        visit(whileLoop.getCondition(), p);
        visitStatement(whileLoop.getPadding().getBody(), CRightPadded.Location.WHILE_BODY, p);
        return whileLoop;
    }

    @Override
    public C visitWildcard(Wildcard wildcard, PrintOutputCapture<P> p) {
        visitSpace(wildcard.getPrefix(), Space.Location.WILDCARD_PREFIX, p);
        visitMarkers(wildcard.getMarkers(), p);
        p.out.append('?');
        if (wildcard.getPadding().getBound() != null) {
            //noinspection ConstantConditions
            switch (wildcard.getBound()) {
                case Extends:
                    visitSpace(wildcard.getPadding().getBound().getBefore(), Space.Location.WILDCARD_BOUND, p);
                    p.out.append("extends");
                    break;
                case Super:
                    visitSpace(wildcard.getPadding().getBound().getBefore(), Space.Location.WILDCARD_BOUND, p);
                    p.out.append("super");
                    break;
            }
        }
        visit(wildcard.getBoundedType(), p);
        return wildcard;
    }
}
