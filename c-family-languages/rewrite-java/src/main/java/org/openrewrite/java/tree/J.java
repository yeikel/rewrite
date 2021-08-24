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
package org.openrewrite.java.tree;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.family.c.CVisitor;
import org.openrewrite.family.c.internal.TypeCache;
import org.openrewrite.family.c.tree.*;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.AddImport;
import org.openrewrite.java.JavaPrinter;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.RemoveImport;
import org.openrewrite.marker.Markers;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

@SuppressWarnings("unused")
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
public interface J extends C {

    @SuppressWarnings("unchecked")
    @Override
    default <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        if(v instanceof JavaVisitor) {
            return (R) acceptJava((JavaVisitor<P>) v, p);
        } else {
            return (R) acceptC((CVisitor<P>) v, p);
        }
    }

    @Override
    default <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
        return new JavaPrinter<>();
    }

    static String print(C tree) {
        PrintOutputCapture<Integer> outputCapture = new PrintOutputCapture<>(0);
        new JavaPrinter<Integer>().visit(tree, outputCapture);
        return outputCapture.out.toString();
    }

    static String printTrimmed(C tree) {
        return StringUtils.trimIndent(print(tree));
    }

    @Nullable
    default <P> C acceptJava(JavaVisitor<P> v, P p) {
        return v.defaultValue(this, p);
    }

    @ToString
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class CompilationUnit implements J, CSourceFile {
        @Nullable
        @NonFinal
        transient SoftReference<TypeCache> typesInUse;

        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        @Getter
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        @With
        @Getter
        Path sourcePath;

        @Nullable
        CRightPadded<Package> packageDeclaration;

        @Nullable
        public Package getPackageDeclaration() {
            return packageDeclaration == null ? null : packageDeclaration.getElement();
        }

        public J.CompilationUnit withPackageDeclaration(Package packageDeclaration) {
            return getPadding().withPackageDeclaration(CRightPadded.withElement(this.packageDeclaration, packageDeclaration));
        }

        List<CRightPadded<Import>> imports;

        public List<Import> getImports() {
            return CRightPadded.getElements(imports);
        }

        public J.CompilationUnit withImports(List<Import> imports) {
            return getPadding().withImports(CRightPadded.withElements(this.imports, imports));
        }

        @Override
        public <P> TreeVisitor<C, P> maybeAddImport(String fullyQualifiedName) {
            return new AddImport<>(fullyQualifiedName, null, true);
        }

        @Override
        public <P> TreeVisitor<C, P> maybeRemoveImport(String fullyQualifiedName) {
            return new RemoveImport<>(fullyQualifiedName, false);
        }

        @With
        @Getter
        List<ClassDeclaration> classes;

        @With
        @Getter
        Space eof;

        @Override
        public <P> C acceptJava(JavaVisitor<P> v, P p) {
            return v.visitCompilationUnit(this, p);
        }

        @Override
        public <P> C acceptC(CVisitor<P> v, P p) {
            return v.visit(this, p);
        }

        @Override
        public Set<CType> getTypesInUse() {
            return typeCache().getTypesInUse();
        }

        public Set<CType.Method> getDeclaredMethods() {
            return typeCache().getDeclaredMethods();
        }

        private TypeCache typeCache() {
            TypeCache cache;
            if (this.typesInUse == null) {
                cache = TypeCache.build(this);
                this.typesInUse = new SoftReference<>(cache);
            } else {
                cache = this.typesInUse.get();
                if (cache == null || cache.getCu() != this) {
                    cache = TypeCache.build(this);
                    this.typesInUse = new SoftReference<>(cache);
                }
            }
            return cache;
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final J.CompilationUnit t;

            @Nullable
            public org.openrewrite.family.c.tree.CRightPadded<Package> getPackageDeclaration() {
                return t.packageDeclaration;
            }

            public J.CompilationUnit withPackageDeclaration(@Nullable CRightPadded<Package> packageDeclaration) {
                return t.packageDeclaration == packageDeclaration ? t : new J.CompilationUnit(t.id, t.prefix, t.markers, t.sourcePath, packageDeclaration, t.imports, t.classes, t.eof);
            }

            public List<CRightPadded<Import>> getImports() {
                return t.imports;
            }

            public J.CompilationUnit withImports(List<CRightPadded<Import>> imports) {
                return t.imports == imports ? t : new J.CompilationUnit(t.id, t.prefix, t.markers, t.sourcePath, t.packageDeclaration, imports, t.classes, t.eof);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    final class Import implements J, Statement, Comparable<Import> {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @Getter
        @EqualsAndHashCode.Include
        UUID id;

        @Getter
        @With
        Space prefix;

        @Getter
        @With
        Markers markers;

        CLeftPadded<Boolean> statik;

        @With
        @Getter
        FieldAccess qualid;

        public boolean isStatic() {
            return statik.getElement();
        }

        public Import withStatic(boolean statik) {
            return getPadding().withStatic(this.statik.withElement(statik));
        }

        @Override
        public <P> C acceptJava(JavaVisitor<P> v, P p) {
            return v.visitImport(this, p);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        public boolean isFromType(String clazz) {
            if ("*".equals(qualid.getSimpleName())) {
                return J.printTrimmed(qualid.getTarget()).equals(Arrays.stream(clazz.split("\\."))
                        .filter(pkgOrNam -> Character.isLowerCase(pkgOrNam.charAt(0)))
                        .collect(Collectors.joining("."))
                );
            }
            return (isStatic() ? J.printTrimmed(qualid.getTarget()) : J.printTrimmed(qualid)).equals(clazz);
        }

        public String getTypeName() {
            return isStatic() ? J.printTrimmed(qualid.getTarget()) : J.printTrimmed(qualid);
        }

        /**
         * Retrieve just the package from the import.
         * e.g.:
         * <code>
         * import org.foo.A;            == "org.foo"
         * import static org.foo.A.bar; == "org.foo"
         * import org.foo.*;            == "org.foo"
         * </code>
         */
        public String getPackageName() {
            CType.FullyQualified importType = TypeUtils.asFullyQualified(qualid.getType());
            if (importType != null) {
                return importType.getPackageName();
            }

            AtomicBoolean takeWhile = new AtomicBoolean(true);
            return Arrays.stream(J.printTrimmed(qualid.getTarget()).split("\\."))
                    .filter(pkg -> {
                        takeWhile.set(takeWhile.get() && !pkg.isEmpty() && Character.isLowerCase(pkg.charAt(0)));
                        return takeWhile.get();
                    })
                    .collect(joining("."));
        }

        public String getClassName() {
            String pkg = getPackageName();
            return pkg.length() > 0 ? getTypeName().substring(pkg.length() + 1) : getTypeName();
        }

        @Override
        public int compareTo(Import o) {
            String p1 = this.getPackageName();
            String p2 = o.getPackageName();

            String[] p1s = p1.split("\\.");
            String[] p2s = p2.split("\\.");

            for (int i = 0; i < p1s.length; i++) {
                String s = p1s[i];
                if (p2s.length < i + 1) {
                    return 1;
                }
                if (!s.equals(p2s[i])) {
                    return s.compareTo(p2s[i]);
                }
            }

            return p1s.length < p2s.length ? -1 :
                    this.getQualid().getSimpleName().compareTo(o.getQualid().getSimpleName());
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            throw new UnsupportedOperationException("Imports are not a valid target for templating.");
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final Import t;

            public CLeftPadded<Boolean> getStatic() {
                return t.statik;
            }

            public Import withStatic(CLeftPadded<Boolean> statik) {
                return t.statik == statik ? t : new Import(t.id, t.prefix, t.markers, statik, t.qualid);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    final class Package implements J {
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        Expression expression;

        @With
        List<Annotation> annotations;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitPackage(this, p);
        }
    }
}
