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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.family.c.CIsoVisitor;
import org.openrewrite.family.c.tree.CSourceFile;
import org.openrewrite.family.c.style.IntelliJ;
import org.openrewrite.family.c.style.SpacesStyle;

public class Spaces extends Recipe {

    @Override
    public String getDisplayName() {
        return "Spaces";
    }

    @Override
    public String getDescription() {
        return "Format whitespace in Java code.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new SpacesFromCompilationUnitStyle();
    }

    private static class SpacesFromCompilationUnitStyle extends CIsoVisitor<ExecutionContext> {
        @Override
        public CSourceFile visitSourceFile(CSourceFile sourceFile, ExecutionContext ctx) {
            SpacesStyle style = sourceFile.getStyle(SpacesStyle.class);
            if (style == null) {
                style = IntelliJ.spaces();
            }
            doAfterVisit(new SpacesVisitor<>(style, sourceFile.getStyle(EmptyForInitializerPadStyle.class), sourceFile.getStyle(EmptyForIteratorPadStyle.class)));
            return sourceFile;
        }
    }
}
