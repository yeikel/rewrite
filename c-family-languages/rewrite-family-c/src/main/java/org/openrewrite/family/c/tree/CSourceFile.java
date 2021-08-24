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
package org.openrewrite.family.c.tree;

import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.family.c.CVisitor;
import org.openrewrite.marker.Markers;

import java.nio.file.Path;
import java.util.Set;

public interface CSourceFile extends C, SourceFile {
    <P> TreeVisitor<C, P> maybeAddImport(String fullyQualifiedName);
    <P> TreeVisitor<C, P> maybeRemoveImport(String fullyQualifiedName);

    Path getSourcePath();
    CSourceFile withSourcePath(Path sourcePath);

    @SuppressWarnings("unchecked")
    CSourceFile withMarkers(Markers markers);

    Space getEof();

    CSourceFile withEof(Space eof);

    <P> C acceptC(CVisitor<P> v, P p);

    Set<CType> getTypesInUse();
}
