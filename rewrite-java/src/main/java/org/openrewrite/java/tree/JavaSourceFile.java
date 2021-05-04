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
package org.openrewrite.java.tree;

import org.openrewrite.SourceFile;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.marker.Markers;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * NOTE: this is an interface to permit JVM language variants like Groovy to still extend this
 * AST with additional language constructs at the top level {@link SourceFile}.
 */
public interface JavaSourceFile extends J, SourceFile {
    @Nullable
    J.Package getPackageDeclaration();

    JavaSourceFile withPackageDeclaration(Package packageDeclaration);

    List<Import> getImports();

    JavaSourceFile withImports(List<Import> imports);

    <P extends Padding> P getPadding();

    Path getSourcePath();

    JavaSourceFile withSourcePath(Path sourcePath);

    List<ClassDeclaration> getClasses();

    JavaSourceFile withClasses(List<ClassDeclaration> classes);

    @SuppressWarnings("unchecked")
    JavaSourceFile withMarkers(Markers markers);

    Space getEof();

    JavaSourceFile withEof(Space eof);

    <P> J acceptJava(JavaVisitor<P> v, P p);

    Set<NameTree> findType(String clazz);

    Set<JavaType> getTypesInUse();

    interface Padding {
        @Nullable
        JRightPadded<Package> getPackageDeclaration();
        JavaSourceFile withPackageDeclaration(@Nullable JRightPadded<Package> packageDeclaration);

        List<JRightPadded<Import>> getImports();
        JavaSourceFile withImports(List<JRightPadded<Import>> imports);
    }
}
