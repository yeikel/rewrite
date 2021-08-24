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
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.family.c.style.SpacesStyle;

public class SpacesVisitor<P> extends CIsoVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    /*
    TODO Finish support for SpacesStyle properties, from SpacesStyle.Within.groupingParentheses down
     */

    private final SpacesStyle style;

    @Nullable
    private final EmptyForInitializerPadStyle emptyForInitializerPadStyle;

    @Nullable
    private final EmptyForIteratorPadStyle emptyForIteratorPadStyle;

    public SpacesVisitor(SpacesStyle style, @Nullable EmptyForInitializerPadStyle emptyForInitializerPadStyle, @Nullable EmptyForIteratorPadStyle emptyForIteratorPadStyle) {
        this(style, emptyForInitializerPadStyle, emptyForIteratorPadStyle, null);
    }

    public SpacesVisitor(SpacesStyle style, @Nullable EmptyForInitializerPadStyle emptyForInitializerPadStyle, @Nullable EmptyForIteratorPadStyle emptyForIteratorPadStyle, @Nullable Tree stopAfter) {
        this.style = style;
        this.stopAfter = stopAfter;
        this.emptyForInitializerPadStyle = emptyForInitializerPadStyle;
        this.emptyForIteratorPadStyle = emptyForIteratorPadStyle;
    }

    <T extends C> T spaceBefore(T j, boolean spaceBefore) {
        if (spaceBefore && StringUtils.isNullOrEmpty(j.getPrefix().getWhitespace())) {
            return j.withPrefix(j.getPrefix().withWhitespace(" "));
        } else if (!spaceBefore && j.getPrefix().getWhitespace().equals(" ")) {
            return j.withPrefix(j.getPrefix().withWhitespace(""));
        } else {
            return j;
        }
    }

    <T> CContainer<T> spaceBefore(CContainer<T> container, boolean spaceBefore) {
        if (spaceBefore && StringUtils.isNullOrEmpty(container.getBefore().getWhitespace())) {
            return container.withBefore(container.getBefore().withWhitespace(" "));
        } else if (!spaceBefore && container.getBefore().getWhitespace().equals(" ")) {
            return container.withBefore(container.getBefore().withWhitespace(""));
        } else {
            return container;
        }
    }

    <T extends C> CLeftPadded<T> spaceBefore(CLeftPadded<T> container, boolean spaceBefore) {
        if (spaceBefore && StringUtils.isNullOrEmpty(container.getBefore().getWhitespace())) {
            return container.withBefore(container.getBefore().withWhitespace(" "));
        } else if (!spaceBefore && container.getBefore().getWhitespace().equals(" ")) {
            return container.withBefore(container.getBefore().withWhitespace(""));
        } else {
            return container;
        }
    }

    <T extends C> CLeftPadded<T> spaceBeforeLeftPaddedElement(CLeftPadded<T> container, boolean spaceBefore) {
        return container.withElement(spaceBefore(container.getElement(), spaceBefore));
    }

    <T extends C> CRightPadded<T> spaceBeforeRightPaddedElement(CRightPadded<T> container, boolean spaceBefore) {
        return container.withElement(spaceBefore(container.getElement(), spaceBefore));
    }

    <T extends C> CRightPadded<T> spaceAfter(CRightPadded<T> container, boolean spaceAfter) {
        if (spaceAfter && StringUtils.isNullOrEmpty(container.getAfter().getWhitespace())) {
            return container.withAfter(container.getAfter().withWhitespace(" "));
        } else if (!spaceAfter && container.getAfter().getWhitespace().equals(" ")) {
            return container.withAfter(container.getAfter().withWhitespace(""));
        } else {
            return container;
        }
    }

    @Override
    public C.ClassDeclaration visitClassDeclaration(C.ClassDeclaration classDecl, P p) {
        C.ClassDeclaration c = super.visitClassDeclaration(classDecl, p);
        if (c.getBody() != null) {
            c = c.withBody(spaceBefore(c.getBody(), style.getBeforeLeftBrace().getClassLeftBrace()));
            if (c.getBody().getStatements().isEmpty()) {
                if (c.getKind() != C.ClassDeclaration.Kind.Type.Enum) {
                    boolean withinCodeBraces = style.getWithin().getCodeBraces();
                    if (withinCodeBraces && StringUtils.isNullOrEmpty(c.getBody().getEnd().getWhitespace())) {
                        c = c.withBody(
                                c.getBody().withEnd(
                                        c.getBody().getEnd().withWhitespace(" ")
                                )
                        );
                    } else if (!withinCodeBraces && c.getBody().getEnd().getWhitespace().equals(" ")) {
                        c = c.withBody(
                                c.getBody().withEnd(
                                        c.getBody().getEnd().withWhitespace("")
                                )
                        );
                    }
                } else {
                    boolean spaceInsideOneLineEnumBraces = style.getOther().getInsideOneLineEnumBraces();
                    if (spaceInsideOneLineEnumBraces && StringUtils.isNullOrEmpty(c.getBody().getEnd().getWhitespace())) {
                        c = c.withBody(c.getBody().withEnd(c.getBody().getEnd().withWhitespace(" ")));
                    } else if (!spaceInsideOneLineEnumBraces && c.getBody().getEnd().getWhitespace().equals(" ")) {
                        c = c.withBody(c.getBody().withEnd(c.getBody().getEnd().withWhitespace("")));
                    }
                }
            }
        }
        if (c.getPadding().getTypeParameters() != null) {
            c = c.getPadding().withTypeParameters(
                    spaceBefore(c.getPadding().getTypeParameters(),
                            style.getTypeParameters().getBeforeOpeningAngleBracket())
            );
        }
        if (c.getPadding().getTypeParameters() != null) {
            boolean spaceWithinAngleBrackets = style.getWithin().getAngleBrackets();
            int typeParametersSize = c.getPadding().getTypeParameters().getElements().size();
            c = c.getPadding().withTypeParameters(
                    c.getPadding().getTypeParameters().getPadding().withElements(
                            ListUtils.map(c.getPadding().getTypeParameters().getPadding().getElements(),
                                    (index, elemContainer) -> {
                                        if (index == 0) {
                                            elemContainer = elemContainer.withElement(
                                                    spaceBefore(elemContainer.getElement(), spaceWithinAngleBrackets)
                                            );
                                        } else {
                                            elemContainer = elemContainer.withElement(
                                                    spaceBefore(elemContainer.getElement(),
                                                            style.getOther().getAfterComma())
                                            );
                                        }
                                        if (index == typeParametersSize - 1) {
                                            elemContainer = spaceAfter(elemContainer, spaceWithinAngleBrackets);
                                        }
                                        return elemContainer;
                                    }
                            )
                    )
            );
        }
        return c;
    }

    @Override
    public C.MethodDeclaration visitMethodDeclaration(C.MethodDeclaration method, P p) {
        C.MethodDeclaration m = super.visitMethodDeclaration(method, p);
        m = m.getPadding().withParameters(
                spaceBefore(m.getPadding().getParameters(), style.getBeforeParentheses().getMethodDeclaration()));
        if (m.getBody() != null) {
            m = m.withBody(spaceBefore(m.getBody(), style.getBeforeLeftBrace().getMethodLeftBrace()));
        }
        Statement firstParam = m.getParameters().iterator().next();
        if (firstParam instanceof C.Empty) {
            boolean useSpace = style.getWithin().getEmptyMethodDeclarationParentheses();
            m = m.getPadding().withParameters(
                    m.getPadding().getParameters().getPadding().withElements(
                            ListUtils.map(m.getPadding().getParameters().getPadding().getElements(),
                                    param -> param.withElement(spaceBefore(param.getElement(), useSpace))
                            )
                    )
            );
        } else {
            final int paramsSize = m.getParameters().size();
            boolean useSpace = style.getWithin().getMethodDeclarationParentheses();
            m = m.getPadding().withParameters(
                    m.getPadding().getParameters().getPadding().withElements(
                            ListUtils.map(m.getPadding().getParameters().getPadding().getElements(),
                                    (index, param) -> {
                                        if (index == 0) {
                                            param = param.withElement(spaceBefore(param.getElement(), useSpace));
                                        } else {
                                            param = param.withElement(
                                                    spaceBefore(param.getElement(), style.getOther().getAfterComma())
                                            );
                                        }
                                        if (index == paramsSize - 1) {
                                            param = spaceAfter(param, useSpace);
                                        } else {
                                            param = spaceAfter(param, style.getOther().getBeforeComma());
                                        }
                                        return param;
                                    }
                            )
                    )
            );
        }
        if (m.getAnnotations().getTypeParameters() != null) {
            boolean spaceWithinAngleBrackets = style.getWithin().getAngleBrackets();
            int typeParametersSize = m.getAnnotations().getTypeParameters().getTypeParameters().size();
            m = m.getAnnotations().withTypeParameters(
                    m.getAnnotations().getTypeParameters().getPadding().withTypeParameters(
                            ListUtils.map(m.getAnnotations().getTypeParameters().getPadding().getTypeParameters(),
                                    (index, elemContainer) -> {
                                        if (index == 0) {
                                            elemContainer = elemContainer.withElement(
                                                    spaceBefore(elemContainer.getElement(), spaceWithinAngleBrackets)
                                            );
                                        } else {
                                            elemContainer = elemContainer.withElement(
                                                    spaceBefore(elemContainer.getElement(),
                                                            style.getOther().getAfterComma())
                                            );
                                        }
                                        if (index == typeParametersSize - 1) {
                                            elemContainer = spaceAfter(elemContainer, spaceWithinAngleBrackets);
                                        }
                                        return elemContainer;
                                    })
                    )
            );
        }
        return m;
    }

    @Override
    public C.MethodInvocation visitMethodInvocation(C.MethodInvocation method, P p) {
        C.MethodInvocation m = super.visitMethodInvocation(method, p);
        m = m.getPadding().withArguments(spaceBefore(m.getPadding().getArguments(), style.getBeforeParentheses().getMethodCall()));
        Expression firstArg = m.getArguments().iterator().next();
        if (firstArg instanceof C.Empty) {
            boolean useSpace = style.getWithin().getEmptyMethodCallParentheses();
            m = m.getPadding().withArguments(
                    m.getPadding().getArguments().getPadding().withElements(
                            ListUtils.map(m.getPadding().getArguments().getPadding().getElements(),
                                    arg -> arg.withElement(spaceBefore(arg.getElement(), useSpace))
                            )
                    )
            );
        } else {
            final int argsSize = m.getArguments().size();
            boolean useSpace = style.getWithin().getMethodCallParentheses();
            m = m.getPadding().withArguments(
                    m.getPadding().getArguments().getPadding().withElements(
                            ListUtils.map(m.getPadding().getArguments().getPadding().getElements(),
                                    (index, arg) -> {
                                        if (index == 0) {
                                            arg = arg.withElement(spaceBefore(arg.getElement(), useSpace));
                                        } else {
                                            arg = arg.withElement(
                                                    spaceBefore(arg.getElement(), style.getOther().getAfterComma())
                                            );
                                        }
                                        if (index == argsSize - 1) {
                                            arg = spaceAfter(arg, useSpace);
                                        } else {
                                            arg = spaceAfter(arg, style.getOther().getBeforeComma());
                                        }
                                        return arg;
                                    }
                            )
                    )
            );
        }
        if (m.getPadding().getTypeParameters() != null) {
            m = m.getPadding().withTypeParameters(
                    spaceBefore(m.getPadding().getTypeParameters(),
                            style.getTypeArguments().getBeforeOpeningAngleBracket())
            );
            m = m.withName(spaceBefore(m.getName(), style.getTypeArguments().getAfterClosingAngleBracket()));
        }
        if (m.getPadding().getTypeParameters() != null) {
            m = m.getPadding().withTypeParameters(
                    m.getPadding().getTypeParameters().getPadding().withElements(
                            ListUtils.map(m.getPadding().getTypeParameters().getPadding().getElements(),
                                    (index, elemContainer) -> {
                                        if (index != 0) {
                                            elemContainer = elemContainer.withElement(
                                                    spaceBefore(elemContainer.getElement(),
                                                            style.getTypeArguments().getAfterComma())
                                            );
                                        }
                                        return elemContainer;
                                    }
                            )
                    )
            );
        }
        return m;
    }

    @Override
    public C.If visitIf(C.If iff, P p) {
        C.If i = super.visitIf(iff, p);
        i = i.withIfCondition(spaceBefore(i.getIfCondition(), style.getBeforeParentheses().getIfParentheses()));
        i = i.getPadding().withThenPart(spaceBeforeRightPaddedElement(i.getPadding().getThenPart(), style.getBeforeLeftBrace().getIfLeftBrace()));
        boolean useSpaceWithinIfParentheses = style.getWithin().getIfParentheses();
        i = i.withIfCondition(
                i.getIfCondition().getPadding().withTree(
                        spaceAfter(
                                i.getIfCondition().getPadding().getTree().withElement(
                                        spaceBefore(i.getIfCondition().getPadding().getTree().getElement(), useSpaceWithinIfParentheses
                                        )
                                ),
                                useSpaceWithinIfParentheses
                        )
                )
        );
        return i;
    }

    @Override
    public C.If.Else visitElse(C.If.Else elze, P p) {
        C.If.Else e = super.visitElse(elze, p);
        e = e.getPadding().withBody(spaceBeforeRightPaddedElement(e.getPadding().getBody(), style.getBeforeLeftBrace().getElseLeftBrace()));
        e = spaceBefore(e, style.getBeforeKeywords().getElseKeyword());
        return e;
    }

    @Override
    public C.ForLoop visitForLoop(C.ForLoop forLoop, P p) {
        C.ForLoop f = super.visitForLoop(forLoop, p);
        C.ForLoop.Control control = f.getControl();
        control = spaceBefore(control, style.getBeforeParentheses().getForParentheses());

        Boolean padEmptyForInitializer = null;
        if(emptyForInitializerPadStyle != null) {
            padEmptyForInitializer = emptyForInitializerPadStyle.getSpace();
        }
        boolean spaceWithinForParens = style.getWithin().getForParentheses();
        boolean shouldPutSpaceOnInit;
        if(padEmptyForInitializer != null && f.getControl().getInit().get(0) instanceof C.Empty) {
            shouldPutSpaceOnInit = padEmptyForInitializer;
        } else {
            shouldPutSpaceOnInit = spaceWithinForParens;
        }
        control = control.withInit(
                ListUtils.mapFirst(f.getControl().getInit(), i -> spaceBefore(i, shouldPutSpaceOnInit))
        );
        boolean spaceBeforeSemicolon = style.getOther().getBeforeForSemicolon();
        boolean spaceAfterSemicolon = style.getOther().getAfterForSemicolon();
        control = control.getPadding().withInit(
                ListUtils.mapFirst(control.getPadding().getInit(), i -> spaceAfter(i, spaceBeforeSemicolon))
        );
        control = control.getPadding().withCondition(
                spaceAfter(control.getPadding().getCondition(), spaceBeforeSemicolon)
        );
        control = control.getPadding().withCondition(
                control.getPadding().getCondition().withElement(
                        spaceBefore(control.getPadding().getCondition().getElement(), spaceAfterSemicolon)
                )
        );
        int updateStatementsSize = f.getControl().getUpdate().size();
        Boolean padEmptyForIterator = (emptyForIteratorPadStyle == null) ? null : emptyForIteratorPadStyle.getSpace();
        if(padEmptyForIterator != null && updateStatementsSize == 1 && f.getControl().getUpdate().get(0) instanceof C.Empty) {
            control = control.getPadding().withUpdate(
                    ListUtils.map(control.getPadding().getUpdate(), (index, elemContainer) -> {
                        elemContainer = elemContainer.withElement(
                                spaceBefore(elemContainer.getElement(), padEmptyForIterator)
                        );
                        return elemContainer;
                    })
            );
        } else {
            control = control.getPadding().withUpdate(
                    ListUtils.map(control.getPadding().getUpdate(),
                            (index, elemContainer) -> {
                                if (index == 0) {
                                    elemContainer = elemContainer.withElement(
                                            spaceBefore(elemContainer.getElement(), spaceAfterSemicolon)
                                    );
                                } else {
                                    elemContainer = elemContainer.withElement(
                                            spaceBefore(elemContainer.getElement(), style.getOther().getAfterComma())
                                    );
                                }
                                if (index == updateStatementsSize - 1) {
                                    elemContainer = spaceAfter(elemContainer, spaceWithinForParens);
                                } else {
                                    elemContainer = spaceAfter(elemContainer, style.getOther().getBeforeComma());
                                }
                                return elemContainer;
                            }
                    )
            );
        }
        f = f.withControl(control);
        f = f.getPadding().withBody(spaceBeforeRightPaddedElement(f.getPadding().getBody(), style.getBeforeLeftBrace().getForLeftBrace()));
        return f;
    }

    @Override
    public C.ForEachLoop visitForEachLoop(C.ForEachLoop forLoop, P p) {
        C.ForEachLoop f = super.visitForEachLoop(forLoop, p);
        f = f.withControl(spaceBefore(f.getControl(), style.getBeforeParentheses().getForParentheses()));
        f = f.getPadding().withBody(spaceBeforeRightPaddedElement(f.getPadding().getBody(), style.getBeforeLeftBrace().getForLeftBrace()));
        boolean spaceWithinForParens = style.getWithin().getForParentheses();
        f = f.withControl(
                f.getControl().withVariable(
                        spaceBefore(f.getControl().getVariable(), spaceWithinForParens)
                )
        );
        f = f.withControl(
                f.getControl().getPadding().withIterable(
                        spaceAfter(f.getControl().getPadding().getIterable(), spaceWithinForParens)
                )
        );
        f = f.withControl(
                f.getControl().getPadding().withVariable(
                        spaceAfter(f.getControl().getPadding().getVariable(), style.getOther().getBeforeColonInForEach())
                )
        );
        return f;
    }

    @Override
    public C.WhileLoop visitWhileLoop(C.WhileLoop whileLoop, P p) {
        C.WhileLoop w = super.visitWhileLoop(whileLoop, p);
        w = w.withCondition(spaceBefore(w.getCondition(), style.getBeforeParentheses().getWhileParentheses()));
        w = w.getPadding().withBody(spaceBeforeRightPaddedElement(w.getPadding().getBody(), style.getBeforeLeftBrace().getWhileLeftBrace()));
        boolean spaceWithinWhileParens = style.getWithin().getWhileParentheses();
        w = w.withCondition(
                w.getCondition().withTree(
                        spaceBefore(w.getCondition().getTree(), spaceWithinWhileParens)
                )
        );
        w = w.withCondition(
                w.getCondition().getPadding().withTree(
                        spaceAfter(w.getCondition().getPadding().getTree(), spaceWithinWhileParens)
                )
        );
        return w;
    }

    @Override
    public C.DoWhileLoop visitDoWhileLoop(C.DoWhileLoop doWhileLoop, P p) {
        C.DoWhileLoop d = super.visitDoWhileLoop(doWhileLoop, p);
        d = d.getPadding().withWhileCondition(spaceBefore(d.getPadding().getWhileCondition(), style.getBeforeKeywords().getWhileKeyword()));
        d = d.getPadding().withWhileCondition(spaceBeforeLeftPaddedElement(d.getPadding().getWhileCondition(), style.getBeforeParentheses().getWhileParentheses()));
        d = d.getPadding().withBody(spaceBeforeRightPaddedElement(d.getPadding().getBody(), style.getBeforeLeftBrace().getDoLeftBrace()));
        boolean spaceWithinWhileParens = style.getWithin().getWhileParentheses();
        d = d.withWhileCondition(
                d.getWhileCondition().withTree(
                        spaceBefore(d.getWhileCondition().getTree(), spaceWithinWhileParens)
                )
        );
        d = d.withWhileCondition(
                d.getWhileCondition().getPadding().withTree(
                        spaceAfter(d.getWhileCondition().getPadding().getTree(), spaceWithinWhileParens)
                )
        );
        return d;
    }

    @Override
    public C.Switch visitSwitch(C.Switch _switch, P p) {
        C.Switch s = super.visitSwitch(_switch, p);
        s = s.withSelector(spaceBefore(s.getSelector(), style.getBeforeParentheses().getSwitchParentheses()));
        s = s.withCases(spaceBefore(s.getCases(), style.getBeforeLeftBrace().getSwitchLeftBrace()));
        boolean spaceWithinSwitchParens = style.getWithin().getSwitchParentheses();
        s = s.withSelector(
                s.getSelector().withTree(
                        spaceBefore(s.getSelector().getTree(), spaceWithinSwitchParens)
                )
        );
        s = s.withSelector(
                s.getSelector().getPadding().withTree(
                        spaceAfter(s.getSelector().getPadding().getTree(), spaceWithinSwitchParens)
                )
        );
        return s;
    }

    @Override
    public C.Try visitTry(C.Try _try, P p) {
        C.Try t = super.visitTry(_try, p);
        if (t.getPadding().getResources() != null) {
            t = t.getPadding().withResources(spaceBefore(t.getPadding().getResources(), style.getBeforeParentheses().getTryParentheses()));
        }
        t = t.withBody(spaceBefore(t.getBody(), style.getBeforeLeftBrace().getTryLeftBrace()));
        if (t.getPadding().getFinally() != null) {
            CLeftPadded<C.Block> f = spaceBefore(t.getPadding().getFinally(), style.getBeforeKeywords().getFinallyKeyword());
            f = spaceBeforeLeftPaddedElement(f, style.getBeforeLeftBrace().getFinallyLeftBrace());
            t = t.getPadding().withFinally(f);
        }
        boolean spaceWithinTryParentheses = style.getWithin().getTryParentheses();
        if (t.getResources() != null) {
            t = t.withResources(ListUtils.mapFirst(t.getResources(), res -> spaceBefore(res, spaceWithinTryParentheses)));
        }
        if (t.getPadding().getResources() != null) {
            t = t.getPadding().withResources(
                    t.getPadding().getResources().getPadding().withElements(
                            ListUtils.mapLast(t.getPadding().getResources().getPadding().getElements(),
                                    res -> spaceAfter(res, spaceWithinTryParentheses)
                            )
                    )
            );
        }
        return t;
    }

    @Override
    public C.Try.Catch visitCatch(C.Try.Catch _catch, P p) {
        C.Try.Catch c = super.visitCatch(_catch, p);
        c = spaceBefore(c, style.getBeforeKeywords().getCatchKeyword());
        c = c.withParameter(spaceBefore(c.getParameter(), style.getBeforeParentheses().getCatchParentheses()));
        c = c.withBody(spaceBefore(c.getBody(), style.getBeforeLeftBrace().getCatchLeftBrace()));
        boolean spaceWithinCatchParens = style.getWithin().getCatchParentheses();
        c = c.withParameter(
                c.getParameter().withTree(
                        spaceBefore(c.getParameter().getTree(), spaceWithinCatchParens)
                )
        );
        c = c.withParameter(
                c.getParameter().getPadding().withTree(
                        spaceAfter(c.getParameter().getPadding().getTree(), spaceWithinCatchParens)
                )
        );
        return c;
    }

    @Override
    public C.Synchronized visitSynchronized(C.Synchronized sync, P p) {
        C.Synchronized s = super.visitSynchronized(sync, p);
        s = s.withLock(spaceBefore(s.getLock(), style.getBeforeParentheses().getSynchronizedParentheses()));
        s = s.withBody(spaceBefore(s.getBody(), style.getBeforeLeftBrace().getSynchronizedLeftBrace()));
        boolean spaceWithinSynchronizedParens = style.getWithin().getSynchronizedParentheses();
        s = s.withLock(
                s.getLock().withTree(
                        spaceBefore(s.getLock().getTree(), spaceWithinSynchronizedParens)
                )
        );
        s = s.withLock(
                s.getLock().getPadding().withTree(
                        spaceAfter(s.getLock().getPadding().getTree(), spaceWithinSynchronizedParens)
                )
        );
        return s;
    }

    @Override
    public C.Annotation visitAnnotation(C.Annotation annotation, P p) {
        C.Annotation a = super.visitAnnotation(annotation, p);
        if (a.getPadding().getArguments() != null) {
            a = a.getPadding().withArguments(spaceBefore(a.getPadding().getArguments(),
                    style.getBeforeParentheses().getAnnotationParameters()));
        }
        boolean spaceWithinAnnotationParentheses = style.getWithin().getAnnotationParentheses();
        if (a.getPadding().getArguments() != null) {
            int argsSize = a.getPadding().getArguments().getElements().size();
            a = a.getPadding().withArguments(
                    a.getPadding().getArguments().getPadding().withElements(
                            ListUtils.map(a.getPadding().getArguments().getPadding().getElements(),
                                    (index, arg) -> {
                                        if (index == 0) {
                                            // don't overwrite changes made by before left brace annotation array
                                            // initializer setting when space within annotation parens is false
                                            if (spaceWithinAnnotationParentheses ||
                                                    !style.getBeforeLeftBrace().getAnnotationArrayInitializerLeftBrace()) {
                                                arg = arg.withElement(
                                                        spaceBefore(arg.getElement(), spaceWithinAnnotationParentheses)
                                                );
                                            }
                                        }
                                        if (index == argsSize - 1) {
                                            arg = spaceAfter(arg, spaceWithinAnnotationParentheses);
                                        }
                                        return arg;
                                    }
                            )
                    )
            );
        }
        return a;
    }

    @Override
    public C.Assignment visitAssignment(C.Assignment assignment, P p) {
        C.Assignment a = super.visitAssignment(assignment, p);
        a = a.getPadding().withAssignment(spaceBefore(a.getPadding().getAssignment(), style.getAroundOperators().getAssignment()));
        a = a.getPadding().withAssignment(
                a.getPadding().getAssignment().withElement(
                        spaceBefore(a.getPadding().getAssignment().getElement(), style.getAroundOperators().getAssignment())
                )
        );
        return a;
    }

    @Override
    public C.AssignmentOperation visitAssignmentOperation(C.AssignmentOperation assignOp, P p) {
        C.AssignmentOperation a = super.visitAssignmentOperation(assignOp, p);
        C.AssignmentOperation.Padding padding = a.getPadding();
        CLeftPadded<C.AssignmentOperation.Type> operator = padding.getOperator();
        String operatorBeforeWhitespace = operator.getBefore().getWhitespace();
        if (style.getAroundOperators().getAssignment() && StringUtils.isNullOrEmpty(operatorBeforeWhitespace)) {
            a = padding.withOperator(
                    operator.withBefore(
                            operator.getBefore().withWhitespace(" ")
                    )
            );
        } else if (!style.getAroundOperators().getAssignment() && operatorBeforeWhitespace.equals(" ")) {
            a = padding.withOperator(
                    operator.withBefore(
                            operator.getBefore().withWhitespace("")
                    )
            );
        }
        a = a.withAssignment(spaceBefore(a.getAssignment(), style.getAroundOperators().getAssignment()));
        return a;
    }

    @Override
    public C.VariableDeclarations.NamedVariable visitVariable(C.VariableDeclarations.NamedVariable variable, P p) {
        C.VariableDeclarations.NamedVariable v = super.visitVariable(variable, p);
        if (v.getPadding().getInitializer() != null) {
            v = v.getPadding().withInitializer(spaceBefore(v.getPadding().getInitializer(), style.getAroundOperators().getAssignment()));
        }
        if (v.getPadding().getInitializer() != null) {
            if (v.getPadding().getInitializer().getElement() != null) {
                v = v.getPadding().withInitializer(
                        v.getPadding().getInitializer().withElement(
                                spaceBefore(v.getPadding().getInitializer().getElement(), style.getAroundOperators().getAssignment())
                        )
                );
            }
        }
        return v;
    }

    @Override
    public C.Binary visitBinary(C.Binary binary, P p) {
        C.Binary b = super.visitBinary(binary, p);
        C.Binary.Type operator = b.getOperator();
        switch (operator) {
            case And:
            case Or:
                b = applyBinarySpaceAround(b, style.getAroundOperators().getLogical());
                break;
            case Equal:
            case NotEqual:
                b = applyBinarySpaceAround(b, style.getAroundOperators().getEquality());
                break;
            case LessThan:
            case LessThanOrEqual:
            case GreaterThan:
            case GreaterThanOrEqual:
                b = applyBinarySpaceAround(b, style.getAroundOperators().getRelational());
                break;
            case BitAnd:
            case BitOr:
            case BitXor:
                b = applyBinarySpaceAround(b, style.getAroundOperators().getBitwise());
                break;
            case Addition:
            case Subtraction:
                b = applyBinarySpaceAround(b, style.getAroundOperators().getAdditive());
                break;
            case Multiplication:
            case Division:
            case Modulo:
                b = applyBinarySpaceAround(b, style.getAroundOperators().getMultiplicative());
                break;
            case LeftShift:
            case RightShift:
            case UnsignedRightShift:
                b = applyBinarySpaceAround(b, style.getAroundOperators().getShift());
                break;
        }
        return b;
    }

    private C.Binary applyBinarySpaceAround(C.Binary binary, boolean useSpaceAround) {
        C.Binary.Padding padding = binary.getPadding();
        CLeftPadded<C.Binary.Type> operator = padding.getOperator();
        if (useSpaceAround) {
            if (StringUtils.isNullOrEmpty(operator.getBefore().getWhitespace())) {
                binary = padding.withOperator(
                        operator.withBefore(
                                operator.getBefore().withWhitespace(" ")
                        )
                );
            }
            if (StringUtils.isNullOrEmpty(binary.getRight().getPrefix().getWhitespace())) {
                binary = binary.withRight(
                        binary.getRight().withPrefix(
                                binary.getRight().getPrefix().withWhitespace(" ")
                        )
                );
            }
        } else {
            if (operator.getBefore().getWhitespace().equals(" ")) {
                binary = padding.withOperator(
                        operator.withBefore(
                                operator.getBefore().withWhitespace("")
                        )
                );
            }
            if (binary.getRight().getPrefix().getWhitespace().equals(" ")) {
                binary = binary.withRight(
                        binary.getRight().withPrefix(
                                binary.getRight().getPrefix().withWhitespace("")
                        )
                );
            }
        }
        return binary;
    }

    @Override
    public C.Unary visitUnary(C.Unary unary, P p) {
        C.Unary u = super.visitUnary(unary, p);
        switch (u.getOperator()) {
            case PostIncrement:
            case PostDecrement:
                u = applyUnaryOperatorBeforeSpace(u, style.getAroundOperators().getUnary());
                break;
            case PreIncrement:
            case PreDecrement:
            case Negative:
            case Positive:
            case Not:
            case Complement:
                u = applyUnaryOperatorBeforeSpace(u, style.getAroundOperators().getUnary());
                u = applyUnaryOperatorExprSpace(u, style.getAroundOperators().getUnary());
                break;
        }
        return u;
    }

    private C.Unary applyUnaryOperatorExprSpace(C.Unary unary, boolean useAroundUnaryOperatorSpace) {
        if (useAroundUnaryOperatorSpace && StringUtils.isNullOrEmpty(unary.getExpression().getPrefix().getWhitespace())) {
            unary = unary.withExpression(
                    unary.getExpression().withPrefix(
                            unary.getExpression().getPrefix().withWhitespace(" ")
                    )
            );
        } else if (!useAroundUnaryOperatorSpace && unary.getExpression().getPrefix().getWhitespace().equals(" ")) {
            unary = unary.withExpression(
                    unary.getExpression().withPrefix(
                            unary.getExpression().getPrefix().withWhitespace("")
                    )
            );
        }
        return unary;
    }

    private C.Unary applyUnaryOperatorBeforeSpace(C.Unary u, boolean useAroundUnaryOperatorSpace) {
        C.Unary.Padding padding = u.getPadding();
        CLeftPadded<C.Unary.Type> operator = padding.getOperator();
        if (useAroundUnaryOperatorSpace && StringUtils.isNullOrEmpty(operator.getBefore().getWhitespace())) {
            u = padding.withOperator(
                    operator.withBefore(
                            operator.getBefore().withWhitespace(" ")
                    )
            );
        } else if (!useAroundUnaryOperatorSpace && operator.getBefore().getWhitespace().equals(" ")) {
            u = padding.withOperator(
                    operator.withBefore(
                            operator.getBefore().withWhitespace("")
                    )
            );
        }
        return u;
    }

    @Override
    public C.Lambda visitLambda(C.Lambda lambda, P p) {
        C.Lambda l = super.visitLambda(lambda, p);
        boolean useSpaceAroundLambdaArrow = style.getAroundOperators().getLambdaArrow();
        if (useSpaceAroundLambdaArrow && StringUtils.isNullOrEmpty(l.getArrow().getWhitespace())) {
            l = l.withArrow(
                    l.getArrow().withWhitespace(" ")
            );
        } else if (!useSpaceAroundLambdaArrow && l.getArrow().getWhitespace().equals(" ")) {
            l = l.withArrow(
                    l.getArrow().withWhitespace("")
            );
        }
        l = l.withBody(spaceBefore(l.getBody(), style.getAroundOperators().getLambdaArrow()));
        if (!(l.getParameters().getParameters().iterator().next() instanceof C.Empty)) {
            int parametersSize = l.getParameters().getParameters().size();
            l = l.withParameters(
                    l.getParameters().getPadding().withParams(
                            ListUtils.map(l.getParameters().getPadding().getParams(),
                                    (index, elemContainer) -> {
                                        if (index != 0) {
                                            elemContainer = elemContainer.withElement(
                                                    spaceBefore(elemContainer.getElement(), style.getOther().getAfterComma())
                                            );
                                        }
                                        if (index != parametersSize - 1) {
                                            elemContainer = spaceAfter(elemContainer, style.getOther().getBeforeComma());
                                        }
                                        return elemContainer;
                                    }
                            )
                    )
            );
        }
        return l;
    }

    @Override
    public C.MemberReference visitMemberReference(C.MemberReference memberRef, P p) {
        C.MemberReference m = super.visitMemberReference(memberRef, p);
        m = m.getPadding().withContaining(
                spaceAfter(m.getPadding().getContaining(), style.getAroundOperators().getMethodReferenceDoubleColon())
        );
        if (m.getPadding().getTypeParameters() != null) {
            m.getPadding().withTypeParameters(spaceBefore(m.getPadding().getTypeParameters(), style.getAroundOperators().getMethodReferenceDoubleColon()));
        } else {
            m = m.getPadding().withReference(
                    spaceBefore(m.getPadding().getReference(), style.getAroundOperators().getMethodReferenceDoubleColon())
            );
        }
        return m;
    }

    @Override
    public C.NewArray visitNewArray(C.NewArray newArray, P p) {
        C.NewArray n = super.visitNewArray(newArray, p);
        if (getCursor().getParent() != null && getCursor().getParent().firstEnclosing(C.class) instanceof C.Annotation) {
            /*
             * IntelliJ setting
             * Spaces -> Within -> Annotation parentheses
             * when enabled supercedes
             * Spaces -> Before left brace -> Annotation array initializer left brace
             */
            if (!style.getWithin().getAnnotationParentheses()) {
                n = spaceBefore(n, style.getBeforeLeftBrace().getAnnotationArrayInitializerLeftBrace());
            }
        } else {
            if (n.getPadding().getInitializer() != null) {
                CContainer<Expression> initializer = spaceBefore(n.getPadding().getInitializer(), style.getBeforeLeftBrace().getArrayInitializerLeftBrace());
                n = n.getPadding().withInitializer(initializer);
            }
        }
        if (n.getPadding().getInitializer() != null) {
            CContainer<Expression> i = n.getPadding().getInitializer();
            if (style.getOther().getAfterComma()) {
                i = CContainer.withElements(i, ListUtils.map(i.getElements(), (idx, elem) -> idx == 0 ? elem : spaceBefore(elem, true)));
            }

            if (i.getElements().iterator().next() instanceof C.Empty) {
                boolean useSpaceWithinEmptyArrayInitializerBraces = style.getWithin().getEmptyArrayInitializerBraces();
                i = i.map(expr -> spaceBefore(expr, useSpaceWithinEmptyArrayInitializerBraces));
            } else {
                boolean useSpaceWithinArrayInitializerBraces = style.getWithin().getArrayInitializerBraces();
                int initializerElementsSize = i.getElements().size();
                i = i.getPadding().withElements(ListUtils.map(i.getPadding().getElements(), (index, elemContainer) -> {
                    if (index == 0) {
                        elemContainer = elemContainer.withElement(spaceBefore(elemContainer.getElement(), useSpaceWithinArrayInitializerBraces));
                    } else {
                        elemContainer = elemContainer.withElement(spaceBefore(elemContainer.getElement(), style.getOther().getAfterComma()));
                    }
                    if (index == initializerElementsSize - 1) {
                        elemContainer = spaceAfter(elemContainer, useSpaceWithinArrayInitializerBraces);
                    } else {
                        elemContainer = spaceAfter(elemContainer, style.getOther().getBeforeComma());
                    }
                    return elemContainer;
                }));
            }
            n = n.getPadding().withInitializer(i);
        }
        return n;
    }

    @Override
    public C.ArrayAccess visitArrayAccess(C.ArrayAccess arrayAccess, P p) {
        C.ArrayAccess a = super.visitArrayAccess(arrayAccess, p);
        boolean useSpaceWithinBrackets = style.getWithin().getBrackets();
        if (useSpaceWithinBrackets) {
            if (StringUtils.isNullOrEmpty(a.getDimension().getPadding().getIndex().getElement().getPrefix().getWhitespace())) {
                a = a.withDimension(
                        a.getDimension().getPadding().withIndex(
                                a.getDimension().getPadding().getIndex().withElement(
                                        a.getDimension().getPadding().getIndex().getElement().withPrefix(
                                                a.getDimension().getPadding().getIndex().getElement().getPrefix().withWhitespace(" ")
                                        )
                                )
                        )
                );
            }
            if (StringUtils.isNullOrEmpty(a.getDimension().getPadding().getIndex().getAfter().getWhitespace())) {
                a = a.withDimension(
                        a.getDimension().getPadding().withIndex(
                                a.getDimension().getPadding().getIndex().withAfter(
                                        a.getDimension().getPadding().getIndex().getAfter().withWhitespace(" ")
                                )
                        )
                );
            }
        } else {
            if (a.getDimension().getPadding().getIndex().getElement().getPrefix().getWhitespace().equals(" ")) {
                a = a.withDimension(
                        a.getDimension().getPadding().withIndex(
                                a.getDimension().getPadding().getIndex().withElement(
                                        a.getDimension().getPadding().getIndex().getElement().withPrefix(
                                                a.getDimension().getPadding().getIndex().getElement().getPrefix().withWhitespace("")
                                        )
                                )
                        )
                );
            }
            if (a.getDimension().getPadding().getIndex().getAfter().getWhitespace().equals(" ")) {
                a = a.withDimension(
                        a.getDimension().getPadding().withIndex(
                                a.getDimension().getPadding().getIndex().withAfter(
                                        a.getDimension().getPadding().getIndex().getAfter().withWhitespace("")
                                )
                        )
                );
            }
        }
        return a;
    }

    @Override
    public <T extends C> C.Parentheses<T> visitParentheses(C.Parentheses<T> parens, P p) {
        C.Parentheses<T> p2 = super.visitParentheses(parens, p);
        if (style.getWithin().getGroupingParentheses()) {
            if (StringUtils.isNullOrEmpty(p2.getPadding().getTree().getElement().getPrefix().getWhitespace())) {
                p2 = p2.getPadding().withTree(
                        p2.getPadding().getTree().withElement(
                                p2.getPadding().getTree().getElement().withPrefix(
                                        p2.getPadding().getTree().getElement().getPrefix().withWhitespace(" ")
                                )
                        )
                );
            }
            if (StringUtils.isNullOrEmpty(p2.getPadding().getTree().getAfter().getWhitespace())) {
                p2 = p2.getPadding().withTree(
                        p2.getPadding().getTree().withAfter(
                                p2.getPadding().getTree().getAfter().withWhitespace(" ")
                        )
                );
            }
        } else {
            if (p2.getPadding().getTree().getElement().getPrefix().getWhitespace().equals(" ")) {
                p2 = p2.getPadding().withTree(
                        p2.getPadding().getTree().withElement(
                                p2.getPadding().getTree().getElement().withPrefix(
                                        p2.getPadding().getTree().getElement().getPrefix().withWhitespace("")
                                )
                        )
                );
            }
            if (p2.getPadding().getTree().getAfter().getWhitespace().equals(" ")) {
                p2 = p2.getPadding().withTree(
                        p2.getPadding().getTree().withAfter(
                                p2.getPadding().getTree().getAfter().withWhitespace("")
                        )
                );
            }
        }
        return p2;
    }

    @Override
    public C.TypeCast visitTypeCast(C.TypeCast typeCast, P p) {
        C.TypeCast tc = super.visitTypeCast(typeCast, p);
        boolean spaceWithinTypeCastParens = style.getWithin().getTypeCastParentheses();
        tc = tc.withClazz(
                tc.getClazz().withTree(
                        spaceBefore(tc.getClazz().getTree(), spaceWithinTypeCastParens)
                )
        );
        tc = tc.withClazz(
                tc.getClazz().getPadding().withTree(
                        spaceAfter(tc.getClazz().getPadding().getTree(), spaceWithinTypeCastParens)
                )
        );
        tc = tc.withExpression(spaceBefore(tc.getExpression(), style.getOther().getAfterTypeCast()));
        return tc;
    }

    @Override
    public C.ParameterizedType visitParameterizedType(C.ParameterizedType type, P p) {
        C.ParameterizedType pt = super.visitParameterizedType(type, p);
        boolean spaceWithinAngleBrackets = style.getWithin().getAngleBrackets();
        if (pt.getPadding().getTypeParameters() != null) {
            pt = pt.getPadding().withTypeParameters(
                    spaceBefore(pt.getPadding().getTypeParameters(),
                            style.getTypeArguments().getBeforeOpeningAngleBracket())
            );
        }
        if (pt.getPadding().getTypeParameters() != null &&
                !(pt.getPadding().getTypeParameters().getElements().iterator().next() instanceof C.Empty)) {
            int typeParametersSize = pt.getPadding().getTypeParameters().getElements().size();
            pt = pt.getPadding().withTypeParameters(
                    pt.getPadding().getTypeParameters().getPadding().withElements(
                            ListUtils.map(pt.getPadding().getTypeParameters().getPadding().getElements(),
                                    (index, elemContainer) -> {
                                        if (index == 0) {
                                            elemContainer = elemContainer.withElement(
                                                    spaceBefore(elemContainer.getElement(), spaceWithinAngleBrackets)
                                            );
                                        } else {
                                            elemContainer = elemContainer.withElement(
                                                    spaceBefore(elemContainer.getElement(),
                                                            style.getTypeArguments().getAfterComma())
                                            );
                                        }
                                        if (index == typeParametersSize - 1) {
                                            elemContainer = spaceAfter(elemContainer, spaceWithinAngleBrackets);
                                        }
                                        return elemContainer;
                                    }
                            )
                    )
            );
        }
        return pt;
    }

    @Override
    public C.Ternary visitTernary(C.Ternary ternary, P p) {
        C.Ternary t = super.visitTernary(ternary, p);
        SpacesStyle.TernaryOperator ternaryOperatorSettings = style.getTernaryOperator();
        t = t.getPadding().withTruePart(spaceBefore(t.getPadding().getTruePart(), ternaryOperatorSettings.getBeforeQuestionMark()));
        t = t.withTruePart(spaceBefore(t.getTruePart(), ternaryOperatorSettings.getAfterQuestionMark()));
        t = t.getPadding().withFalsePart(spaceBefore(t.getPadding().getFalsePart(), ternaryOperatorSettings.getBeforeColon()));
        t = t.withFalsePart(spaceBefore(t.getFalsePart(), ternaryOperatorSettings.getAfterColon()));
        return t;
    }

    @Override
    public C.NewClass visitNewClass(C.NewClass newClass, P p) {
        C.NewClass nc = super.visitNewClass(newClass, p);
        if (nc.getPadding().getArguments() != null) {
            nc = nc.getPadding().withArguments(spaceBefore(nc.getPadding().getArguments(), style.getBeforeParentheses().getMethodCall()));
            int argsSize = nc.getPadding().getArguments().getElements().size();
            nc = nc.getPadding().withArguments(
                    nc.getPadding().getArguments().getPadding().withElements(
                            ListUtils.map(nc.getPadding().getArguments().getPadding().getElements(),
                                    (index, elemContainer) -> {
                                        if (index != 0) {
                                            elemContainer = elemContainer.withElement(
                                                    spaceBefore(elemContainer.getElement(), style.getOther().getAfterComma())
                                            );
                                        }
                                        if (index != argsSize - 1) {
                                            elemContainer = spaceAfter(elemContainer, style.getOther().getBeforeComma());
                                        }
                                        return elemContainer;
                                    }
                            )
                    )
            );
        }
        return nc;
    }

    @Override
    public C.EnumValue visitEnumValue(C.EnumValue _enum, P p) {
        C.EnumValue e = super.visitEnumValue(_enum, p);
        if (e.getInitializer() != null && e.getInitializer().getPadding().getArguments() != null) {
            int initializerArgumentsSize = e.getInitializer().getPadding().getArguments().getPadding().getElements().size();
            e = e.withInitializer(
                    e.getInitializer().getPadding().withArguments(
                            e.getInitializer().getPadding().getArguments().getPadding().withElements(
                                    ListUtils.map(e.getInitializer().getPadding().getArguments().getPadding().getElements(),
                                            (index, elemContainer) -> {
                                                if (index != 0) {
                                                    elemContainer = elemContainer.withElement(
                                                            spaceBefore(elemContainer.getElement(),
                                                                    style.getOther().getAfterComma())
                                                    );
                                                }
                                                if (index != initializerArgumentsSize - 1) {
                                                    elemContainer = spaceAfter(elemContainer,
                                                            style.getOther().getBeforeComma());
                                                }
                                                return elemContainer;
                                            }
                                    )
                            )
                    )
            );
        }
        return e;
    }

    @Override
    public C.TypeParameter visitTypeParameter(C.TypeParameter typeParam, P p) {
        C.TypeParameter tp = super.visitTypeParameter(typeParam, p);
        if (tp.getPadding().getBounds() != null) {
            boolean spaceAroundTypeBounds = style.getTypeParameters().getAroundTypeBounds();
            int typeBoundsSize = tp.getPadding().getBounds().getPadding().getElements().size();
            tp = tp.getPadding().withBounds(
                    tp.getPadding().getBounds().getPadding().withElements(
                            ListUtils.map(tp.getPadding().getBounds().getPadding().getElements(),
                                    (index, elemContainer) -> {
                                        if (index != 0) {
                                            elemContainer = elemContainer.withElement(spaceBefore(elemContainer.getElement(), spaceAroundTypeBounds));
                                        }
                                        if (index != typeBoundsSize - 1) {
                                            elemContainer = spaceAfter(elemContainer, spaceAroundTypeBounds);
                                        }
                                        return elemContainer;
                                    }
                            )
                    )
            );
        }
        return tp;
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
