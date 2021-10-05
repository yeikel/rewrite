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
package org.openrewrite.groovy;

import org.openrewrite.PrintOutputCapture;
import org.openrewrite.Tree;
import org.openrewrite.groovy.marker.*;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.groovy.tree.GContainer;
import org.openrewrite.groovy.tree.GRightPadded;
import org.openrewrite.groovy.tree.GSpace;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaPrinter;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;

import java.util.List;

public class GroovyPrinter<P> extends GroovyVisitor<PrintOutputCapture<P>> {
    private final GroovyJavaPrinter delegate = new GroovyJavaPrinter();

    @Override
    public J visit(@Nullable Tree tree, PrintOutputCapture<P> p) {
        if (!(tree instanceof G)) {
            // re-route printing to the java printer
            return delegate.visit(tree, p);
        } else {
            return super.visit(tree, p);
        }
    }

    @Override
    public J visitJavaSourceFile(JavaSourceFile sourceFile, PrintOutputCapture<P> p) {
        G.CompilationUnit cu = (G.CompilationUnit) sourceFile;
        visitSpace(cu.getPrefix(), Space.Location.COMPILATION_UNIT_PREFIX, p);
        visitMarkers(cu.getMarkers(), p);

        JRightPadded<J.Package> pkg = cu.getPadding().getPackageDeclaration();
        if (pkg != null) {
            visit(pkg.getElement(), p);
            visitSpace(pkg.getAfter(), Space.Location.PACKAGE_SUFFIX, p);
        }

        for (JRightPadded<Statement> statement : cu.getPadding().getStatements()) {
            visitRightPadded(statement, GRightPadded.Location.TOP_LEVEL_STATEMENT_SUFFIX, p);
        }

        visitSpace(cu.getEof(), Space.Location.COMPILATION_UNIT_EOF, p);
        return cu;
    }

    @Override
    public J visitGString(G.GString gString, PrintOutputCapture<P> p) {
        visitSpace(gString.getPrefix(), GSpace.Location.GSTRING, p);
        visitMarkers(gString.getMarkers(), p);
        p.out.append('"');
        visit(gString.getStrings(), p);
        p.out.append('"');
        return gString;
    }

    @Override
    public J visitGStringValue(G.GString.Value value, PrintOutputCapture<P> p) {
        visitMarkers(value.getMarkers(), p);
        p.out.append("${");
        visit(value.getTree(), p);
        p.out.append('}');
        return value;
    }

    @Override
    public J visitListLiteral(G.ListLiteral listLiteral, PrintOutputCapture<P> p) {
        visitSpace(listLiteral.getPrefix(), GSpace.Location.MAP_ENTRY, p);
        visitMarkers(listLiteral.getMarkers(), p);
        visitContainer("[", listLiteral.getPadding().getElements(), GContainer.Location.LIST_LITERAL_ELEMENTS,
                ",", "]", p);
        return listLiteral;
    }

    @Override
    public J visitMapEntry(G.MapEntry mapEntry, PrintOutputCapture<P> p) {
        visitSpace(mapEntry.getPrefix(), GSpace.Location.MAP_ENTRY, p);
        visitMarkers(mapEntry.getMarkers(), p);
        visitRightPadded(mapEntry.getPadding().getKey(), GRightPadded.Location.MAP_ENTRY_KEY, p);
        p.out.append(':');
        visit(mapEntry.getValue(), p);
        return mapEntry;
    }

    @Override
    public Space visitSpace(Space space, GSpace.Location loc, PrintOutputCapture<P> p) {
        return delegate.visitSpace(space, Space.Location.LANGUAGE_EXTENSION, p);
    }

    protected void visitContainer(String before, @Nullable JContainer<? extends J> container, GContainer.Location location,
                                  String suffixBetween, @Nullable String after, PrintOutputCapture<P> p) {
        if (container == null) {
            return;
        }
        visitSpace(container.getBefore(), location.getBeforeLocation(), p);
        p.out.append(before);
        visitRightPadded(container.getPadding().getElements(), location.getElementLocation(), suffixBetween, p);
        p.out.append(after == null ? "" : after);
    }

    protected void visitRightPadded(List<? extends JRightPadded<? extends J>> nodes, GRightPadded.Location location, String suffixBetween, PrintOutputCapture<P> p) {
        for (int i = 0; i < nodes.size(); i++) {
            JRightPadded<? extends J> node = nodes.get(i);
            visit(node.getElement(), p);
            visitSpace(node.getAfter(), location.getAfterLocation(), p);
            if (i < nodes.size() - 1) {
                p.out.append(suffixBetween);
            }
        }
    }

    private class GroovyJavaPrinter extends JavaPrinter<P> {
        @Override
        public J visit(@Nullable Tree tree, PrintOutputCapture<P> p) {
            if (tree instanceof G) {
                // re-route printing back up to groovy
                return GroovyPrinter.this.visit(tree, p);
            } else {
                return super.visit(tree, p);
            }
        }

        @Override
        public J visitLambda(J.Lambda lambda, PrintOutputCapture<P> p) {
            visitSpace(lambda.getPrefix(), Space.Location.LAMBDA_PREFIX, p);
            visitMarkers(lambda.getMarkers(), p);
            p.out.append('{');
            visit(lambda.getParameters(), p);
            if (!lambda.getParameters().getParameters().isEmpty()) {
                visitSpace(lambda.getArrow(), Space.Location.LAMBDA_ARROW_PREFIX, p);
                p.out.append("->");
            }
            if (lambda.getBody() instanceof J.Block) {
                J.Block block = (J.Block) lambda.getBody();
                visitStatements(block.getPadding().getStatements(), JRightPadded.Location.BLOCK_STATEMENT, p);
                visitSpace(block.getEnd(), Space.Location.BLOCK_END, p);
            } else {
                visit(lambda.getBody(), p);
            }
            p.out.append('}');
            return lambda;
        }

        @Override
        public J visitFieldAccess(J.FieldAccess fieldAccess, PrintOutputCapture<P> p) {
            visitSpace(fieldAccess.getPrefix(), Space.Location.FIELD_ACCESS_PREFIX, p);
            visitMarkers(fieldAccess.getMarkers(), p);
            visit(fieldAccess.getTarget(), p);
            visitLeftPadded(fieldAccess.getName().getMarkers().findFirst(NullSafe.class).isPresent() ? "?." : ".",
                    fieldAccess.getPadding().getName(), JLeftPadded.Location.FIELD_ACCESS_NAME, p);
            return fieldAccess;
        }

        @Override
        public J visitForEachLoop(J.ForEachLoop forEachLoop, PrintOutputCapture<P> p) {
            visitSpace(forEachLoop.getPrefix(), Space.Location.FOR_EACH_LOOP_PREFIX, p);
            visitMarkers(forEachLoop.getMarkers(), p);
            p.out.append("for");
            J.ForEachLoop.Control ctrl = forEachLoop.getControl();
            visitSpace(ctrl.getPrefix(), Space.Location.FOR_EACH_CONTROL_PREFIX, p);
            p.out.append('(');
            String suffix = forEachLoop.getMarkers().findFirst(InStyleForEachLoop.class).isPresent() ? "in" : ":";
            visitRightPadded(ctrl.getPadding().getVariable(), JRightPadded.Location.FOREACH_VARIABLE, suffix, p);
            visitRightPadded(ctrl.getPadding().getIterable(), JRightPadded.Location.FOREACH_ITERABLE, "", p);
            p.out.append(')');
            visitStatement(forEachLoop.getPadding().getBody(), JRightPadded.Location.FOR_BODY, p);
            return forEachLoop;
        }

        @Override
        public J visitMethodInvocation(J.MethodInvocation method, PrintOutputCapture<P> p) {
            visitSpace(method.getPrefix(), Space.Location.METHOD_INVOCATION_PREFIX, p);
            visitMarkers(method.getMarkers(), p);

            Markers nameMarkers = method.getName().getMarkers();
            visitRightPadded(method.getPadding().getSelect(), JRightPadded.Location.METHOD_SELECT,
                    nameMarkers.findFirst(ImplicitDot.class).isPresent() ? "" :
                            nameMarkers.findFirst(NullSafe.class).isPresent() ? "?." : ".", p);

            visitContainer("<", method.getPadding().getTypeParameters(), JContainer.Location.TYPE_PARAMETERS, ",", ">", p);
            visit(method.getName(), p);

            JContainer<Expression> argContainer = method.getPadding().getArguments();

            visitSpace(argContainer.getBefore(), Space.Location.METHOD_INVOCATION_ARGUMENTS, p);
            List<JRightPadded<Expression>> args = argContainer.getPadding().getElements();
            for (int i = 0; i < args.size(); i++) {
                JRightPadded<Expression> arg = args.get(i);
                boolean omitParens = arg.getElement().getMarkers()
                        .findFirst(OmitParentheses.class).isPresent();

                if (i == 0 && !omitParens) {
                    p.out.append('(');
                } else if(i > 0 && omitParens) {
                    p.out.append(')');
                } else if(i > 0) {
                    p.out.append(',');
                }

                visitRightPadded(arg, JRightPadded.Location.METHOD_INVOCATION_ARGUMENT, p);

                if (i == args.size() - 1 && !omitParens) {
                    p.out.append(')');
                }
            }

            return method;
        }

        @Override
        public J visitReturn(J.Return retrn, PrintOutputCapture<P> p) {
            if (retrn.getMarkers().findFirst(ImplicitReturn.class).isPresent()) {
                visitSpace(retrn.getPrefix(), Space.Location.RETURN_PREFIX, p);
                visitMarkers(retrn.getMarkers(), p);
                visit(retrn.getExpression(), p);
                return retrn;
            }
            return super.visitReturn(retrn, p);
        }

        protected void visitStatement(@Nullable JRightPadded<Statement> paddedStat, JRightPadded.Location location, PrintOutputCapture<P> p) {
            if (paddedStat != null) {
                visit(paddedStat.getElement(), p);
                visitSpace(paddedStat.getAfter(), location.getAfterLocation(), p);
                visitMarkers(paddedStat.getMarkers(), p);
            }
        }

        @Override
        public <M extends Marker> M visitMarker(Marker marker, PrintOutputCapture<P> p) {
            if (marker instanceof Semicolon) {
                p.out.append(';');
            }
            return super.visitMarker(marker, p);
        }
    }

    @Override
    public <M extends Marker> M visitMarker(Marker marker, PrintOutputCapture<P> p) {
        if (marker instanceof Semicolon) {
            p.out.append(';');
        }
        return super.visitMarker(marker, p);
    }
}