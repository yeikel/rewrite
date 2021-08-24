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
package org.openrewrite.family.c.format;

import org.openrewrite.Tree;
import org.openrewrite.family.c.CIsoVisitor;
import org.openrewrite.family.c.tree.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.family.c.style.BlankLinesStyle;

import java.util.Iterator;
import java.util.List;

public class BlankLinesVisitor<P> extends CIsoVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    private final BlankLinesStyle style;

    public BlankLinesVisitor(BlankLinesStyle style) {
        this(style, null);
    }

    public BlankLinesVisitor(BlankLinesStyle style, @Nullable Tree stopAfter) {
        this.style = style;
        this.stopAfter = stopAfter;
    }

    @Override
    public CSourceFile visitSourceFile(CSourceFile sourceFile, P p) {
        CSourceFile c = sourceFile;
        if (c.getPackageDeclaration() != null) {
            if (!c.getPrefix().getComments().isEmpty()) {
                c = c.withComments(ListUtils.mapLast(c.getComments(), c -> {
                    String suffix = keepMaximumLines(c.getSuffix(), style.getKeepMaximum().getBetweenHeaderAndPackage());
                    suffix = minimumLines(suffix, style.getMinimum().getBeforePackage());
                    return c.withSuffix(suffix);
                }));
            } else {
                /*
                 if comments are empty and package is present, leading whitespace is on the compilation unit and
                 should be removed
                 */
                c = c.withPrefix(Space.EMPTY);
            }
        }

        if (c.getPackageDeclaration() == null) {
            if (c.getComments().isEmpty()) {
                /*
                if package decl and comments are null/empty, leading whitespace is on the
                compilation unit and should be removed
                 */
                c = c.withPrefix(Space.EMPTY);
            } else {
                c = c.withComments(ListUtils.mapLast(c.getComments(), c ->
                        c.withSuffix(minimumLines(c.getSuffix(), style.getMinimum().getBeforeImports()))));
            }
        } else {
            c = c.getPadding().withImports(ListUtils.mapFirst(c.getPadding().getImports(), i ->
                    minimumLines(i, style.getMinimum().getAfterPackage())));
        }
        return super.visitSourceFile(c, p);
    }

    @Override
    public C.ClassDeclaration visitClassDeclaration(C.ClassDeclaration classDecl, P p) {
        C.ClassDeclaration j = super.visitClassDeclaration(classDecl, p);
        if (j.getBody() != null) {
            List<CRightPadded<Statement>> statements = j.getBody().getPadding().getStatements();
            j = j.withBody(j.getBody().getPadding().withStatements(ListUtils.map(statements, (i, s) -> {
                if (i == 0) {
                    s = minimumLines(s, style.getMinimum().getAfterClassHeader());
                } else if (statements.get(i - 1).getElement() instanceof C.Block) {
                    s = minimumLines(s, style.getMinimum().getAroundInitializer());
                }

                return s;
            })));

            j = j.withBody(j.getBody().withEnd(minimumLines(j.getBody().getEnd(),
                    style.getMinimum().getBeforeClassEnd())));
        }

        J.CompilationUnit cu = getCursor().firstEnclosingOrThrow(J.CompilationUnit.class);
        boolean hasImports = !cu.getImports().isEmpty();
        boolean firstClass = j.equals(cu.getClasses().get(0));

        j = firstClass ?
                (hasImports ? minimumLines(j, style.getMinimum().getAfterImports()) : j) :
                minimumLines(j, style.getMinimum().getAroundClass());

        if (!hasImports && firstClass) {
            if (cu.getPackageDeclaration() == null) {
                if (!j.getPrefix().getWhitespace().isEmpty()) {
                    j = j.withPrefix(j.getPrefix().withWhitespace(""));
                }
            } else {
                j = minimumLines(j, style.getMinimum().getAfterPackage());
            }
        }

        return j;
    }

    @Override
    public J.Import visitImport(J.Import impoort, P p) {
        J.Import i = super.visitImport(impoort, p);
        J.CompilationUnit cu = getCursor().firstEnclosingOrThrow(J.CompilationUnit.class);
        if (i.equals(cu.getImports().get(0)) && cu.getPackageDeclaration() == null && cu.getPrefix().equals(Space.EMPTY)) {
            i = i.withPrefix(i.getPrefix().withWhitespace(""));
        }
        return i;
    }

    @Override
    public C.MethodDeclaration visitMethodDeclaration(C.MethodDeclaration method, P p) {
        C.MethodDeclaration j = super.visitMethodDeclaration(method, p);
        if (j.getBody() != null) {
            if (j.getBody().getStatements().isEmpty()) {
                Space end = minimumLines(j.getBody().getEnd(),
                        style.getMinimum().getBeforeMethodBody());
                if (end.getIndent().isEmpty() && style.getMinimum().getBeforeMethodBody() > 0) {
                    end = end.withWhitespace(end.getWhitespace() + method.getPrefix().getIndent());
                }
                j = j.withBody(j.getBody().withEnd(end));
            } else {
                j = j.withBody(j.getBody().withStatements(ListUtils.mapFirst(j.getBody().getStatements(), s ->
                        minimumLines(s, style.getMinimum().getBeforeMethodBody()))));
            }
        }

        return j;
    }

    @Override
    public C.NewClass visitNewClass(C.NewClass newClass, P p) {
        C.NewClass j = super.visitNewClass(newClass, p);
        if (j.getBody() != null) {
            j = j.withBody(j.getBody().withStatements(ListUtils.mapFirst(j.getBody().getStatements(), s ->
                    minimumLines(s, style.getMinimum().getAfterAnonymousClassHeader()))));
        }
        return j;
    }

    @Override
    public Statement visitStatement(Statement statement, P p) {
        Statement j = super.visitStatement(statement, p);
        Iterator<Object> cursorPath = getCursor().getParentOrThrow().getPath(J.class::isInstance);
        Object parentTree = cursorPath.next();
        if (cursorPath.hasNext()) {
            Object grandparentTree = cursorPath.next();
            if (grandparentTree instanceof C.ClassDeclaration && parentTree instanceof C.Block) {
                C.Block block = (C.Block) parentTree;
                C.ClassDeclaration classDecl = (C.ClassDeclaration) grandparentTree;

                int declMax = style.getKeepMaximum().getInDeclarations();

                // don't adjust the first statement in a block
                if (!block.getStatements().isEmpty() && block.getStatements().iterator().next() != j) {
                    if (j instanceof C.VariableDeclarations) {
                        if (classDecl.getKind() == J.ClassDeclaration.Kind.Type.Interface) {
                            declMax = Math.max(declMax, style.getMinimum().getAroundFieldInInterface());
                            j = minimumLines(j, style.getMinimum().getAroundFieldInInterface());
                        } else {
                            declMax = Math.max(declMax, style.getMinimum().getAroundField());
                            j = minimumLines(j, style.getMinimum().getAroundField());
                        }
                    } else if (j instanceof C.MethodDeclaration) {
                        if (classDecl.getKind() == J.ClassDeclaration.Kind.Type.Interface) {
                            declMax = Math.max(declMax, style.getMinimum().getAroundMethodInInterface());
                            j = minimumLines(j, style.getMinimum().getAroundMethodInInterface());
                        } else {
                            declMax = Math.max(declMax, style.getMinimum().getAroundMethod());
                            j = minimumLines(j, style.getMinimum().getAroundMethod());
                        }
                    } else if (j instanceof C.Block) {
                        declMax = Math.max(declMax, style.getMinimum().getAroundInitializer());
                        j = minimumLines(j, style.getMinimum().getAroundInitializer());
                    } else if (j instanceof C.ClassDeclaration) {
                        declMax = Math.max(declMax, style.getMinimum().getAroundClass());
                        j = minimumLines(j, style.getMinimum().getAroundClass());
                    }
                }

                j = keepMaximumLines(j, declMax);
            } else {
                return keepMaximumLines(j, style.getKeepMaximum().getInCode());
            }
        }
        return j;
    }

    @Override
    public C.Block visitBlock(C.Block block, P p) {
        C.Block j = super.visitBlock(block, p);
        j = j.withEnd(keepMaximumLines(j.getEnd(), style.getKeepMaximum().getBeforeEndOfBlock()));
        return j;
    }

    private <J2 extends J> J2 keepMaximumLines(J2 tree, int max) {
        return tree.withPrefix(keepMaximumLines(tree.getPrefix(), max));
    }

    private Space keepMaximumLines(Space prefix, int max) {
        return prefix.withWhitespace(keepMaximumLines(prefix.getWhitespace(), max));
    }

    private String keepMaximumLines(String whitespace, int max) {
        long blankLines = whitespace.chars().filter(c -> c == '\n').count() - 1;
        if (blankLines > max) {
            int startWhitespaceAtIndex = 0;
            for (int i = 0; i < blankLines - max + 1; i++, startWhitespaceAtIndex++) {
                startWhitespaceAtIndex = whitespace.indexOf('\n', startWhitespaceAtIndex);
            }
            startWhitespaceAtIndex--;
            return whitespace.substring(startWhitespaceAtIndex);
        }
        return whitespace;
    }

    private <C2 extends C> CRightPadded<C2> minimumLines(CRightPadded<C2> tree, int min) {
        return tree.withElement(minimumLines(tree.getElement(), min));
    }

    private <C2 extends C> C2 minimumLines(C2 tree, int min) {
        return tree.withPrefix(minimumLines(tree.getPrefix(), min));
    }

    private Space minimumLines(Space prefix, int min) {
        if (prefix.getComments().isEmpty() ||
                prefix.getWhitespace().contains("\n") ||
                prefix.getComments().get(0) instanceof Javadoc ||
                (prefix.getComments().get(0).isMultiline() && prefix.getComments().get(0).printComment().contains("\n"))) {
            return prefix.withWhitespace(minimumLines(prefix.getWhitespace(), min));
        }

        // the first comment is a trailing comment on the previous line
        return prefix.withComments(ListUtils.map(prefix.getComments(), (i, c) -> i == 0 ?
                c.withSuffix(minimumLines(c.getSuffix(), min)) : c));
    }

    private String minimumLines(String whitespace, int min) {
        if (min == 0) {
            return whitespace;
        }
        String minWhitespace = whitespace;
        for (int i = 0; i < min - whitespace.chars().filter(c -> c == '\n').count() + 1; i++) {
            //noinspection StringConcatenationInLoop
            minWhitespace = "\n" + minWhitespace;
        }
        return minWhitespace;
    }

    @Nullable
    @Override
    public C postVisit(C tree, P p) {
        if (stopAfter != null && stopAfter.isScope(tree)) {
            getCursor().putMessageOnFirstEnclosing(CSourceFile.class, "stop", true);
        }
        return super.postVisit(tree, p);
    }

    @Nullable
    @Override
    public C visit(@Nullable Tree tree, P p) {
        if (getCursor().getNearestMessage("stop") != null) {
            return (C) tree;
        }
        return super.visit(tree, p);
    }
}
