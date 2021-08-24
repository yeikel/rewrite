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
package org.openrewrite.family.c.style;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.openrewrite.Tree;
import org.openrewrite.family.c.CIsoVisitor;
import org.openrewrite.family.c.tree.C;
import org.openrewrite.family.c.tree.CSourceFile;
import org.openrewrite.family.c.tree.Space;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.style.Style;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;

public class Autodetect extends NamedStyles {
    @JsonCreator
    public Autodetect(UUID id, Collection<Style> styles) {
        super(id,
                "org.openrewrite.family.c.Autodetect",
                "Auto-detected",
                "Automatically detected styles from a repository's existing code.",
                emptySet(), styles);
    }

    public static Autodetect detect(List<? extends CSourceFile> sourceFiles) {
        IndentStatistics indentStatistics = new IndentStatistics();
        SpacesStatistics spacesStatistics = new SpacesStatistics();

        for (CSourceFile sourceFile : sourceFiles) {
            new FindIndentJavaVisitor().visit(sourceFile, indentStatistics);
            new FindSpacesStyle().visit(sourceFile, spacesStatistics);
        }

        return new Autodetect(Tree.randomId(), Arrays.asList(
                indentStatistics.getTabsAndIndentsStyle(),
                spacesStatistics.getSpacesStyle()));
    }

    private static class IndentStatistics {
        private final Map<Integer, Long> indentFrequencies = new HashMap<>();
        private int linesWithSpaceIndents = 0;
        private int linesWithTabIndents = 0;
        private int linesWithCRLFNewLines = 0;
        private int linesWithLFNewLines = 0;

        public boolean isIndentedWithSpaces() {
            return linesWithSpaceIndents >= linesWithTabIndents;
        }

        public boolean isIndentedWithLFNewLines() {
            return linesWithLFNewLines >= linesWithCRLFNewLines;
        }

        public TabsAndIndentsStyle getTabsAndIndentsStyle() {
            boolean useTabs = !isIndentedWithSpaces();
            boolean useCRLF = !isIndentedWithLFNewLines();

            Map.Entry<Integer, Long> i1 = null;
            Map.Entry<Integer, Long> i2 = null;

            for (Map.Entry<Integer, Long> sample : indentFrequencies.entrySet()) {
                if (sample.getKey() == 0) {
                    continue;
                }
                if (i1 == null || i1.getValue() < sample.getValue()) {
                    i1 = sample;
                } else if (i2 == null || i2.getValue() < sample.getValue()) {
                    i2 = sample;
                }
            }

            int indent1 = i1 == null ? 4 : i1.getKey();
            int indent2 = i2 == null ? indent1 : i2.getKey();

            int indent = Math.min(indent1, indent2);
            int continuationIndent = Math.max(indent1, indent2);

            return new TabsAndIndentsStyle(
                    useTabs,
                    useTabs ? indent : 1,
                    useTabs ? 1 : indent,
                    continuationIndent,
                    false,
                    useCRLF
            );
        }
    }

    private static class FindIndentJavaVisitor extends CIsoVisitor<IndentStatistics> {
        @Override
        public Space visitSpace(Space space, Space.Location loc, IndentStatistics stats) {
            Integer lastIndent = getCursor().getNearestMessage("lastIndent");
            if (lastIndent == null) {
                lastIndent = 0;
            }

            String prefix = space.getWhitespace();
            char[] chars = prefix.toCharArray();

            int indent = 0;
            // Note: new lines in multiline comments will not be counted.
            for (int i = 0; i < chars.length; i++) {
                char c = chars[i];
                if (c == '\n' || c == '\r') {
                    if (c == '\n') {
                        if (i == 0 || chars[i - 1] != '\r') {
                            stats.linesWithLFNewLines++;
                        } else {
                            stats.linesWithCRLFNewLines++;
                        }
                    }

                    indent = 0;
                    continue;
                }
                if (Character.isWhitespace(c)) {
                    indent++;
                }
            }

            AtomicBoolean takeWhile = new AtomicBoolean(true);
            if (prefix.chars()
                    .filter(c -> {
                        takeWhile.set(takeWhile.get() && (c == '\n' || c == '\r'));
                        return takeWhile.get();
                    })
                    .count() > 0) {
                stats.indentFrequencies.merge(indent - lastIndent, 1L, Long::sum);
                getCursor().putMessage("lastIndent", indent);

                AtomicBoolean dropWhile = new AtomicBoolean(false);
                takeWhile.set(true);
                Map<Boolean, Long> indentTypeCounts = prefix.chars()
                        .filter(c -> {
                            dropWhile.set(dropWhile.get() || !(c == '\n' || c == '\r'));
                            return dropWhile.get();
                        })
                        .filter(c -> {
                            takeWhile.set(takeWhile.get() && Character.isWhitespace(c));
                            return takeWhile.get();
                        })
                        .mapToObj(c -> c == ' ')
                        .collect(Collectors.groupingBy(identity(), counting()));

                if (indentTypeCounts.getOrDefault(true, 0L) >= indentTypeCounts.getOrDefault(false, 0L)) {
                    stats.linesWithSpaceIndents++;
                } else {
                    stats.linesWithTabIndents++;
                }
            }

            return space;
        }
    }

    private static class SpacesStatistics {
        int beforeIf = 1;
        int beforeMethodCall = 0;
        int beforeMethodDeclaration = 0;
        int beforeFor = 1;
        int beforeWhile = 1;
        int beforeSwitch = 1;
        int beforeTry = 1;
        int beforeCatch = 1;
        int beforeSynchronized = 1;

        public SpacesStyle getSpacesStyle() {
            SpacesStyle spaces = IntelliJ.spaces();
            return spaces
                    .withBeforeParentheses(
                            new SpacesStyle.BeforeParentheses(
                                    beforeMethodDeclaration > 0,
                                    beforeMethodCall > 0,
                                    beforeIf > 0,
                                    beforeFor > 0 || beforeWhile > 0,
                                    beforeWhile > 0 || beforeFor > 0,
                                    beforeSwitch > 0,
                                    beforeTry > 0 || beforeCatch > 0,
                                    beforeTry > 0 || beforeCatch > 0,
                                    beforeSynchronized > 0,
                                    false
                            )
                    );
        }
    }

    private static class FindSpacesStyle extends CIsoVisitor<SpacesStatistics> {
        @Override
        public C.Try.Catch visitCatch(C.Try.Catch _catch, SpacesStatistics stats) {
            stats.beforeCatch += hasSpace(_catch.getParameter().getPrefix());
            return super.visitCatch(_catch, stats);
        }

        @Override
        public C.DoWhileLoop visitDoWhileLoop(C.DoWhileLoop doWhileLoop, SpacesStatistics stats) {
            stats.beforeWhile += hasSpace(doWhileLoop.getWhileCondition().getPrefix());
            return super.visitDoWhileLoop(doWhileLoop, stats);
        }

        @Override
        public C.ForEachLoop visitForEachLoop(C.ForEachLoop forLoop, SpacesStatistics stats) {
            stats.beforeFor += hasSpace(forLoop.getControl().getPrefix());
            return super.visitForEachLoop(forLoop, stats);
        }

        @Override
        public C.ForLoop visitForLoop(C.ForLoop forLoop, SpacesStatistics stats) {
            stats.beforeFor += hasSpace(forLoop.getControl().getPrefix());
            return super.visitForLoop(forLoop, stats);
        }

        @Override
        public C.If visitIf(C.If iff, SpacesStatistics stats) {
            stats.beforeIf += hasSpace(iff.getIfCondition().getPrefix());
            return super.visitIf(iff, stats);
        }

        @Override
        public C.MethodDeclaration visitMethodDeclaration(C.MethodDeclaration method, SpacesStatistics stats) {
            stats.beforeMethodDeclaration += hasSpace(method.getPadding().getParameters().getBefore());
            return super.visitMethodDeclaration(method, stats);
        }

        @Override
        public C.MethodInvocation visitMethodInvocation(C.MethodInvocation method, SpacesStatistics stats) {
            stats.beforeMethodCall += hasSpace(method.getPadding().getArguments().getBefore());
            return super.visitMethodInvocation(method, stats);
        }

        @Override
        public C.Switch visitSwitch(C.Switch _switch, SpacesStatistics stats) {
            stats.beforeSwitch += hasSpace(_switch.getSelector().getPrefix());
            return super.visitSwitch(_switch, stats);
        }

        @Override
        public C.Synchronized visitSynchronized(C.Synchronized _sync, SpacesStatistics stats) {
            stats.beforeSynchronized += hasSpace(_sync.getLock().getPrefix());
            return super.visitSynchronized(_sync, stats);
        }

        @Override
        public C.Try visitTry(C.Try _try, SpacesStatistics stats) {
            if(_try.getPadding().getResources() != null) {
                stats.beforeTry += hasSpace(_try.getPadding().getResources().getBefore());
            }
            return super.visitTry(_try, stats);
        }

        @Override
        public C.WhileLoop visitWhileLoop(C.WhileLoop whileLoop, SpacesStatistics stats) {
            stats.beforeWhile += hasSpace(whileLoop.getCondition().getPrefix());
            return super.visitWhileLoop(whileLoop, stats);
        }

        private int hasSpace(Space space) {
            return space.getWhitespace().contains(" ") ? 1 : -1;
        }
    }
}
