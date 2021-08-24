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

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.RequiredArgsConstructor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;

import java.util.List;
import java.util.function.UnaryOperator;

import static java.util.Collections.emptyList;

/**
 * AST elements that contain lists of trees with some delimiter like parentheses, e.g. method arguments,
 * annotation arguments, catch variable declarations.
 * <p>
 * Sometimes the delimiter surrounds the list. Parentheses surround method arguments. Sometimes the delimiter only
 * precedes the list. Throws statements on method declarations are preceded by the "throws" keyword.
 * <p>
 * Sometimes containers are optional in the grammar, as in the
 * case of annotation arguments. Sometimes they are required, as in the case of method invocation arguments.
 *
 * @param <T> The type of the inner list of elements.
 */
public class CContainer<T> {
    private transient Padding<T> padding;

    private static final CContainer<?> EMPTY = new CContainer<>(Space.EMPTY, emptyList(), Markers.EMPTY);

    private final Space before;
    private final List<CRightPadded<T>> elements;
    private final Markers markers;

    private CContainer(Space before, List<CRightPadded<T>> elements, Markers markers) {
        this.before = before;
        this.elements = elements;
        this.markers = markers;
    }

    public static <T> CContainer<T> build(List<CRightPadded<T>> elements) {
        return build(Space.EMPTY, elements, Markers.EMPTY);
    }

    @JsonCreator
    public static <T> CContainer<T> build(Space before, List<CRightPadded<T>> elements, Markers markers) {
        if (before.isEmpty() && elements.isEmpty()) {
            return empty();
        }
        return new CContainer<>(before, elements, markers);
    }

    @SuppressWarnings("unchecked")
    public static <T> CContainer<T> empty() {
        return (CContainer<T>) EMPTY;
    }

    public CContainer<T> withBefore(Space before) {
        return getBefore() == before ? this : build(before, elements, markers);
    }

    public CContainer<T> withMarkers(Markers markers) {
        return getMarkers() == markers ? this : build(before, elements, markers);
    }

    public Markers getMarkers() {
        return markers;
    }

    public List<T> getElements() {
        return CRightPadded.getElements(elements);
    }

    public Space getBefore() {
        return before;
    }

    public CContainer<T> map(UnaryOperator<T> map) {
        return getPadding().withElements(ListUtils.map(elements, t -> t.map(map)));
    }

    public Space getLastSpace() {
        return elements.isEmpty() ? Space.EMPTY : elements.get(elements.size() - 1).getAfter();
    }

    public enum Location {
        ANNOTATION_ARGUMENTS(Space.Location.ANNOTATION_ARGUMENTS, CRightPadded.Location.ANNOTATION_ARGUMENT),
        CASE(Space.Location.CASE, CRightPadded.Location.CASE),
        IMPLEMENTS(Space.Location.IMPLEMENTS, CRightPadded.Location.IMPLEMENTS),
        METHOD_DECLARATION_PARAMETERS(Space.Location.METHOD_DECLARATION_PARAMETERS, CRightPadded.Location.METHOD_DECLARATION_PARAMETER),
        METHOD_INVOCATION_ARGUMENTS(Space.Location.METHOD_INVOCATION_ARGUMENTS, CRightPadded.Location.METHOD_INVOCATION_ARGUMENT),
        NEW_ARRAY_INITIALIZER(Space.Location.NEW_ARRAY_INITIALIZER, CRightPadded.Location.NEW_ARRAY_INITIALIZER),
        NEW_CLASS_ARGUMENTS(Space.Location.NEW_CLASS_ARGUMENTS, CRightPadded.Location.NEW_CLASS_ARGUMENTS),
        THROWS(Space.Location.THROWS, CRightPadded.Location.THROWS),
        TRY_RESOURCES(Space.Location.TRY_RESOURCES, CRightPadded.Location.TRY_RESOURCE),
        TYPE_BOUNDS(Space.Location.TYPE_BOUNDS, CRightPadded.Location.TYPE_BOUND),
        TYPE_PARAMETERS(Space.Location.TYPE_PARAMETERS, CRightPadded.Location.TYPE_PARAMETER);

        private final Space.Location beforeLocation;
        private final CRightPadded.Location elementLocation;

        Location(Space.Location beforeLocation, CRightPadded.Location elementLocation) {
            this.beforeLocation = beforeLocation;
            this.elementLocation = elementLocation;
        }

        public Space.Location getBeforeLocation() {
            return beforeLocation;
        }

        public CRightPadded.Location getElementLocation() {
            return elementLocation;
        }
    }

    public Padding<T> getPadding() {
        if (padding == null) {
            this.padding = new Padding<>(this);
        }
        return padding;
    }

    @RequiredArgsConstructor
    public static class Padding<T> {
        private final CContainer<T> c;

        public List<CRightPadded<T>> getElements() {
            return c.elements;
        }

        public CContainer<T> withElements(List<CRightPadded<T>> elements) {
            return c.elements == elements ? c : build(c.before, elements, c.markers);
        }
    }

    @Nullable
    public static <J2 extends C> CContainer<J2> withElementsNullable(@Nullable CContainer<J2> before, @Nullable List<J2> elements) {
        if (before == null) {
            if (elements == null || elements.isEmpty()) {
                return null;
            }
            return CContainer.build(Space.EMPTY, CRightPadded.withElements(emptyList(), elements), Markers.EMPTY);
        }
        if (elements == null || elements.isEmpty()) {
            return null;
        }
        return before.getPadding().withElements(CRightPadded.withElements(before.elements, elements));
    }

    public static <J2 extends C> CContainer<J2> withElements(CContainer<J2> before, @Nullable List<J2> elements) {
        if (elements == null) {
            return before.getPadding().withElements(emptyList());
        }
        return before.getPadding().withElements(CRightPadded.withElements(before.elements, elements));
    }

    @Override
    public String toString() {
        return "CContainer(before=" + before + ", elementCount=" + elements.size() + ')';
    }
}
