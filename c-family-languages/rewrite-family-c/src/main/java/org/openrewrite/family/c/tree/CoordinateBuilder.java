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
package org.openrewrite.family.c.tree;

import java.util.Comparator;

public abstract class CoordinateBuilder {
    C tree;

    CoordinateBuilder(C tree) {
        this.tree = tree;
    }

    CCoordinates before(Space.Location location) {
        return new CCoordinates(tree, location, CCoordinates.Mode.BEFORE, null);
    }

    CCoordinates after(Space.Location location) {
        return new CCoordinates(tree, location, CCoordinates.Mode.AFTER, null);
    }

    CCoordinates replace(Space.Location location) {
        return new CCoordinates(tree, location, CCoordinates.Mode.REPLACEMENT, null);
    }

    public static class Statement extends CoordinateBuilder {
        Statement(org.openrewrite.family.c.tree.Statement tree) {
            super(tree);
        }

        public CCoordinates after() {
            return after(Space.Location.STATEMENT_PREFIX);
        }

        public CCoordinates before() {
            return before(Space.Location.STATEMENT_PREFIX);
        }

        public CCoordinates replace() {
            return replace(Space.Location.STATEMENT_PREFIX);
        }
    }

    public static class Annotation extends CoordinateBuilder {
        Annotation(C.Annotation tree) {
            super(tree);
        }

        public CCoordinates replace() {
            return replace(Space.Location.ANNOTATION_PREFIX);
        }
    }

    public static class Block extends Statement {
        Block(C.Block tree) {
            super(tree);
        }

        public CCoordinates lastStatement() {
            return before(Space.Location.BLOCK_END);
        }
    }

    public static class ClassDeclaration extends Statement {
        ClassDeclaration(C.ClassDeclaration tree) {
            super(tree);
        }

        /**
         * @param idealOrdering The new annotation will be inserted in as close to an ideal ordering
         *                      as possible, understanding that the existing annotations may not be
         *                      ordered according to the comparator.
         * @return A variable with a new annotation, inserted before the annotation it would appear
         * before in an ideal ordering, or as the last annotation if it would not appear before any
         * existing annotations in an ideal ordering.
         */
        public CCoordinates addAnnotation(Comparator<C.Annotation> idealOrdering) {
            return new CCoordinates(tree, Space.Location.ANNOTATIONS, CCoordinates.Mode.BEFORE, idealOrdering);
        }

        public CCoordinates replaceAnnotations() {
            return replace(Space.Location.ANNOTATIONS);
        }

        public CCoordinates replaceTypeParameters() {
            return replace(Space.Location.TYPE_PARAMETERS);
        }

        public CCoordinates replaceExtendsClause() {
            return replace(Space.Location.EXTENDS);
        }

        public CCoordinates replaceImplementsClause() {
            return replace(Space.Location.IMPLEMENTS);
        }
    }

    public static class Lambda {
        public static class Parameters extends CoordinateBuilder {
            Parameters(C.Lambda.Parameters tree) {
                super(tree);
            }

            public CCoordinates replace() {
                return replace(Space.Location.LAMBDA_PARAMETERS_PREFIX);
            }
        }
    }

    public static class MethodDeclaration extends Statement {
        MethodDeclaration(C.MethodDeclaration tree) {
            super(tree);
        }

        /**
         * @param idealOrdering The new annotation will be inserted in as close to an ideal ordering
         *                      as possible, understanding that the existing annotations may not be
         *                      ordered according to the comparator.
         * @return A method with a new annotation, inserted before the annotation it would appear
         * before in an ideal ordering, or as the last annotation if it would not appear before any
         * existing annotations in an ideal ordering.
         */
        public CCoordinates addAnnotation(Comparator<C.Annotation> idealOrdering) {
            return new CCoordinates(tree, Space.Location.ANNOTATIONS, CCoordinates.Mode.BEFORE, idealOrdering);
        }

        public CCoordinates replaceAnnotations() {
            return replace(Space.Location.ANNOTATIONS);
        }

        public CCoordinates replaceTypeParameters() {
            return replace(Space.Location.TYPE_PARAMETERS);
        }

        public CCoordinates replaceParameters() {
            return replace(Space.Location.METHOD_DECLARATION_PARAMETERS);
        }

        public CCoordinates replaceThrows() {
            return replace(Space.Location.THROWS);
        }

        public CCoordinates replaceBody() {
            return replace(Space.Location.BLOCK_PREFIX);
        }
    }

    public static class MethodInvocation extends Statement {
        MethodInvocation(C.MethodInvocation tree) {
            super(tree);
        }

        public CCoordinates replaceArguments() {
            return replace(Space.Location.METHOD_INVOCATION_ARGUMENTS);
        }
    }

    public static class VariableDeclarations extends Statement {
        VariableDeclarations(C.VariableDeclarations tree) {
            super(tree);
        }

        public CCoordinates replaceAnnotations() {
            return replace(Space.Location.ANNOTATIONS);
        }

        public CCoordinates addAnnotation(Comparator<C.Annotation> idealOrdering) {
            return new CCoordinates(tree, Space.Location.ANNOTATIONS, CCoordinates.Mode.BEFORE, idealOrdering);
        }
    }
}
