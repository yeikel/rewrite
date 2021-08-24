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

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.openrewrite.family.c.internal.grammar.AnnotationSignatureLexer;
import org.openrewrite.family.c.internal.grammar.AnnotationSignatureParser;
import org.openrewrite.family.c.tree.Expression;
import org.openrewrite.family.c.tree.C;
import org.openrewrite.family.c.tree.CType;
import org.openrewrite.family.c.tree.TypeUtils;

/**
 * This matcher will find all annotations matching the annotation pattern
 * <p>
 * The annotation pattern, expressed as a pointcut expression, is used to find matching annotations. The format of the
 * expression is as follows:
 * <P><P><B>
 * {@literal @}#annotationClass#(#parameterName#=#parameterValue#, #parameterName#=#parameterValue#...)
 * </B><P>
 * <li>The annotationClass must be fully qualified.</li>
 * <li>The parameter name/value pairs can be in any order</li>
 *
 * <P><PRE>
 * EXAMPLES:
 * <p>
 * {@literal @}java.lang.SuppressWarnings                                 - Matches java.lang.SuppressWarnings with no parameters.
 * {@literal @}myhttp.Get(serviceName="payments", path="recentPayments")  - Matches references to myhttp.Get where the parameters are also matched.
 * {@literal @}myhttp.Get(path="recentPayments", serviceName="payments")  - Exaclty the same results from the previous example, order of parameters does not matter.
 * {@literal @}java.lang.SuppressWarnings("deprecation")                  - Matches java.langSupressWarning with a single parameter.
 * {@literal @}org.junit.runner.RunWith(org.junit.runners.JUnit4.class)   - Matches JUnit4's @RunWith(JUnit4.class)
 * </PRE>
 */
public class AnnotationMatcher {
    private final AnnotationSignatureParser.AnnotationContext match;

    public AnnotationMatcher(String signature) {
        this.match = new AnnotationSignatureParser(new CommonTokenStream(new AnnotationSignatureLexer(CharStreams.fromString(signature))))
                .annotation();
    }

    public boolean matches(C.Annotation annotation) {
        return matchesAnnotationName(annotation) &&
                matchesSingleParameter(annotation) &&
                matchesNamedParameters(annotation);
    }

    private boolean matchesAnnotationName(C.Annotation annotation) {
        CType.FullyQualified typeAsClass = TypeUtils.asFullyQualified(annotation.getType());
        return getAnnotationName().equals(typeAsClass == null ? null : typeAsClass.getFullyQualifiedName());
    }

    public String getAnnotationName() {
        return match.annotationName().getText();
    }

    private boolean matchesNamedParameters(C.Annotation annotation) {
        AnnotationSignatureParser.ElementValuePairsContext pairs = match.elementValuePairs();
        if (pairs == null || pairs.elementValuePair() == null) {
            return true;
        }

        if (annotation.getArguments() == null) {
            return false;
        }

        for (AnnotationSignatureParser.ElementValuePairContext elementValuePair : pairs.elementValuePair()) {
            String argumentName = elementValuePair.Identifier().getText();
            String matchText = elementValuePair.elementValue().getText();
            if (annotation.getArguments().stream().noneMatch(arg -> argumentValueMatches(argumentName, arg, matchText))) {
                return false;
            }
        }

        return true;
    }

    private boolean matchesSingleParameter(C.Annotation annotation) {
        if (match.elementValue() == null) {
            return true;
        }

        return annotation.getArguments() == null || annotation.getArguments().stream()
                .findAny()
                .map(arg -> argumentValueMatches("value", arg, match.elementValue().getText()))
                .orElse(true);
    }

    private boolean argumentValueMatches(String matchOnArgumentName, Expression arg, String matchText) {
        if (matchOnArgumentName.equals("value")) {
            if (arg instanceof C.Literal) {
                String valueSource = ((C.Literal) arg).getValueSource();
                return valueSource != null && valueSource.equals(matchText);
            }
            if (arg instanceof C.FieldAccess && ((C.FieldAccess) arg).getSimpleName().equals("class") &&
                    matchText.endsWith(".class")) {
                CType argType = ((C.FieldAccess) arg).getTarget().getType();
                if (argType instanceof CType.FullyQualified) {
                    String queryTypeFqn = CType.Class.build(matchText.replaceAll("\\.class$", "")).getFullyQualifiedName();
                    String targetTypeFqn = ((CType.FullyQualified) argType).getFullyQualifiedName();
                    return queryTypeFqn.equals(targetTypeFqn);
                }
                return false;
            }
            if (arg instanceof C.NewArray) {
                C.NewArray na = (C.NewArray) arg;
                if (na.getInitializer() == null || na.getInitializer().size() != 1) {
                    return false;
                }
                return argumentValueMatches("value", na.getInitializer().get(0), matchText);
            }
        }

        if (!(arg instanceof C.Assignment)) {
            return false;
        }

        C.Assignment assignment = (C.Assignment) arg;
        if (!(assignment.getVariable() instanceof C.Identifier) ||
                !((C.Identifier) assignment.getVariable()).getSimpleName().equals(matchOnArgumentName)) {
            return false;
        }

        // we've already matched the argument name, so recursively we just check the value matches match text.
        return argumentValueMatches("value", assignment.getAssignment(), matchText);
    }
}
