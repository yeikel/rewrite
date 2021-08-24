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
package org.openrewrite.family.c.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.family.c.AnnotationMatcher;
import org.openrewrite.family.c.CIsoVisitor;
import org.openrewrite.family.c.CVisitor;
import org.openrewrite.family.c.marker.JavaSearchResult;
import org.openrewrite.family.c.tree.C;

import java.util.HashSet;
import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@Value
public class FindAnnotations extends Recipe {
    /**
     * An annotation pattern, expressed as a pointcut expression.
     * See {@link AnnotationMatcher} for syntax.
     */
    @Option(displayName = "Annotation pattern",
            description = "An annotation pattern, expressed as a pointcut expression.",
            example = "@java.lang.SuppressWarnings(\"deprecation\")")
    String annotationPattern;

    @Override
    public String getDisplayName() {
        return "Find annotations";
    }

    @Override
    public String getDescription() {
        return "Find all annotations matching the annotation pattern.";
    }

    @Override
    protected CVisitor<ExecutionContext> getSingleSourceApplicableTest() {
        AnnotationMatcher annotationMatcher = new AnnotationMatcher(annotationPattern);
        return new UsesType<>(annotationMatcher.getAnnotationName());
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        AnnotationMatcher annotationMatcher = new AnnotationMatcher(annotationPattern);
        return new CIsoVisitor<ExecutionContext>() {
            @Override
            public C.Annotation visitAnnotation(C.Annotation annotation, ExecutionContext ctx) {
                C.Annotation a = super.visitAnnotation(annotation, ctx);
                if (annotationMatcher.matches(annotation)) {
                    a = a.withMarkers(a.getMarkers().addIfAbsent(new JavaSearchResult(FindAnnotations.this)));
                }
                return a;
            }
        };
    }

    public static Set<C.Annotation> find(C c, String annotationPattern) {
        AnnotationMatcher annotationMatcher = new AnnotationMatcher(annotationPattern);
        CIsoVisitor<Set<C.Annotation>> findVisitor = new CIsoVisitor<Set<C.Annotation>>() {
            @Override
            public C.Annotation visitAnnotation(C.Annotation annotation, Set<C.Annotation> as) {
                if (annotationMatcher.matches(annotation)) {
                    as.add(annotation);
                }
                return super.visitAnnotation(annotation, as);
            }
        };

        Set<C.Annotation> as = new HashSet<>();
        findVisitor.visit(c, as);
        return as;
    }
}
