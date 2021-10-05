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
package org.openrewrite.java

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.Issue
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JavaType
import org.openrewrite.java.tree.Space
import java.util.Comparator.comparing

@Suppress("Convert2MethodRef")
interface JavaTemplateTest : JavaRecipeTest {

    @Test
    fun innerEnumWithStaticMethod(jp: JavaParser) = assertChanged(
        jp,
        recipe = toRecipe {
            object : JavaVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor }, "new A()").build()

                override fun visitNewClass(newClass: J.NewClass, p: ExecutionContext): J = when(newClass.arguments!![0]) {
                    is J.Empty -> newClass
                    else -> newClass.withTemplate(t, newClass.coordinates.replace())
                }
            }
        },
        typeValidation = {
            identifiers = false
        },
        before = """
            class A {
                public enum Type {
                    One;
            
                    public Type(String t) {
                    }
            
                    String t;
            
                    public static Type fromType(String type) {
                        return null;
                    }
                }
            
                public A(Type type) {}
                public A() {}
            
                public void method(Type type) {
                    new A(type);
                }
            }
        """,
        after = """
            class A {
                public enum Type {
                    One;
            
                    public Type(String t) {
                    }
            
                    String t;
            
                    public static Type fromType(String type) {
                        return null;
                    }
                }
            
                public A(Type type) {}
                public A() {}
            
                public void method(Type type) {
                    new A();
                }
            }
        """
    )

    @Test
    fun replacePackage(jp: JavaParser) = assertChanged(
        jp,
        recipe = toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor }, "b").build()

                override fun visitPackage(pkg: J.Package, p: ExecutionContext): J.Package {
                    if (pkg.expression.printTrimmed() == "a") {
                        return pkg.withTemplate(t, pkg.coordinates.replace())
                    }
                    return super.visitPackage(pkg, p)
                }

                override fun visitClassDeclaration(
                    classDecl: J.ClassDeclaration,
                    p: ExecutionContext,
                ): J.ClassDeclaration {
                    var cd = super.visitClassDeclaration(classDecl, p)
                    if (classDecl.type!!.packageName == "a") {
                        cd = cd.withType(cd.type!!.withFullyQualifiedName("b.${cd.simpleName}"))
                    }
                    return cd
                }
            }
        },
        before = """
            package a;
            class Test {
            }
        """,
        after = """
            package b;
            class Test {
            }
        """
    )

    @Test
    fun replaceMethod(jp: JavaParser) = assertChanged(
        jp,
        recipe = toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor }, "int test2(int n) { return n; }").build()

                override fun visitMethodDeclaration(
                    method: J.MethodDeclaration,
                    p: ExecutionContext,
                ): J.MethodDeclaration {
                    if (method.simpleName == "test") {
                        return method.withTemplate(t, method.coordinates.replace())
                    }
                    return super.visitMethodDeclaration(method, p)
                }
            }
        },
        before = """
            class Test {
                void test() {
                }
            }
        """,
        after = """
            class Test {
            
                int test2(int n) {
                    return n;
                }
            }
        """,
        afterConditions = { cu ->
            val methodType = (cu.classes.first().body.statements.first() as J.MethodDeclaration).type!!
            assertThat(methodType.resolvedSignature?.returnType).isEqualTo(JavaType.Primitive.Int)
            assertThat(methodType.resolvedSignature?.paramTypes).containsExactly(JavaType.Primitive.Int)
            assertThat(methodType.genericSignature?.returnType).isEqualTo(JavaType.Primitive.Int)
            assertThat(methodType.genericSignature?.paramTypes).containsExactly(JavaType.Primitive.Int)
        }
    )

    @Test
    fun replaceLambdaWithMethodReference(jp: JavaParser) = assertChanged(
        jp,
        recipe = toRecipe {
            object : JavaVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor }, "Object::toString").build()

                override fun visitLambda(lambda: J.Lambda, p: ExecutionContext): J {
                    return lambda.withTemplate(t, lambda.coordinates.replace())
                }
            }
        },
        before = """
            import java.util.function.Function;

            class Test {
                Function<Object, String> toString = it -> it.toString();
            }
        """,
        after = """
            import java.util.function.Function;

            class Test {
                Function<Object, String> toString = Object::toString;
            }
        """
    )

    @Test
    fun replaceMethodInvocationWithArray(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(
            """
            package org.openrewrite;
            public class Test {
                public void method(int[] val) {}
                public void method(int[] val1, String val2) {}
            }
        """.trimIndent()
        ),
        recipe = toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor }, "#{anyArray(int)}").build()

                override fun visitMethodInvocation(
                    method: J.MethodInvocation,
                    p: ExecutionContext
                ): J.MethodInvocation {
                    var m: J.MethodInvocation = super.visitMethodInvocation(method, p)
                    if (m.simpleName.equals("method") && m.arguments.size == 2) {
                        m = m.withTemplate(t, m.coordinates.replaceArguments(), m.arguments[0])
                    }
                    return m
                }
            }
        },
        typeValidation = {
            identifiers = false
        },
        before = """
            import org.openrewrite.Test;
            class A {
                public void method() {
                    Test test = new Test();
                    int[] arr = new int[]{};
                    test.method(arr, null);
                }
            }
        """,
        after = """
            import org.openrewrite.Test;
            class A {
                public void method() {
                    Test test = new Test();
                    int[] arr = new int[]{};
                    test.method(arr);
                }
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/602")
    @Test
    fun replaceMethodInvocationWithMethodReference(jp: JavaParser) = assertChanged(
        jp,
        recipe = toRecipe {
            object : JavaVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor }, "Object::toString").build()

                override fun visitMethodInvocation(method: J.MethodInvocation, p: ExecutionContext): J {
                    return method.withTemplate(t, method.coordinates.replace())
                }

            }
        },
        before = """
            import java.util.function.Function;

            class Test {
                Function<Object, String> toString = getToString();
                
                static Function<Object, String> getToString() {
                    return Object::toString;
                } 
            }
        """,
        after = """
            import java.util.function.Function;

            class Test {
                Function<Object, String> toString = Object::toString;
                
                static Function<Object, String> getToString() {
                    return Object::toString;
                } 
            }
        """
    )

    @Test
    fun replaceMethodParameters(jp: JavaParser) = assertChanged(
        jp,
        recipe = toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor }, "int m, java.util.List<String> n")
                    .build()

                override fun visitMethodDeclaration(
                    method: J.MethodDeclaration,
                    p: ExecutionContext
                ): J.MethodDeclaration {
                    if (method.simpleName == "test" && method.parameters.size == 1) {
                        // insert in outer method
                        val m: J.MethodDeclaration = method.withTemplate(t, method.coordinates.replaceParameters())
                        val newRunnable = (method.body!!.statements[0] as J.NewClass)

                        // insert in inner method
                        val innerMethod = (newRunnable.body!!.statements[0] as J.MethodDeclaration)
                        return m.withTemplate(t, innerMethod.coordinates.replaceParameters())
                    }
                    return super.visitMethodDeclaration(method, p)
                }
            }
        },
        before = """
            class Test {
                void test() {
                    new Runnable() {
                        void inner() {
                        }
                    };
                }
            }
        """,
        after = """
            class Test {
                void test(int m, java.util.List<String> n) {
                    new Runnable() {
                        void inner(int m, java.util.List<String> n) {
                        }
                    };
                }
            }
        """,
        afterConditions = { cu ->
            val type = (cu.classes.first().body.statements.first() as J.MethodDeclaration).type!!

            assertThat(type.paramNames)
                .`as`("Changing the method's parameters should have also updated its type's parameter names")
                .containsExactly("m", "n")
            assertThat(type.resolvedSignature!!.paramTypes[0])
                .`as`("Changing the method's parameters should have resulted in the first parameter's type being 'int'")
                .isEqualTo(JavaType.Primitive.Int)
            assertThat(type.resolvedSignature!!.paramTypes[1])
                .`as`("Changing the method's parameters should have resulted in the second parameter's type being 'List<String>'")
                .matches {
                    it is JavaType.Parameterized
                            && it.type.fullyQualifiedName == "java.util.List"
                            && it.typeParameters.size == 1
                            && it.typeParameters.first().asFullyQualified()!!.fullyQualifiedName == "java.lang.String"
                }
        }
    )

    @Test
    fun replaceMethodParametersVariadicArray(jp: JavaParser) = assertChanged(
        jp,
        recipe = toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor }, "Object[]... values")
                    .build()

                override fun visitMethodDeclaration(
                    method: J.MethodDeclaration,
                    p: ExecutionContext
                ): J.MethodDeclaration {
                    if (method.simpleName == "test" && method.parameters.firstOrNull() is J.Empty) {
                        // insert in outer method
                        val m: J.MethodDeclaration = method.withTemplate(t, method.coordinates.replaceParameters())
                        val newRunnable = (method.body!!.statements[0] as J.NewClass)

                        // insert in inner method
                        val innerMethod = (newRunnable.body!!.statements[0] as J.MethodDeclaration)
                        return m.withTemplate(t, innerMethod.coordinates.replaceParameters())
                    }
                    return super.visitMethodDeclaration(method, p)
                }
            }
        },
        before = """
            class Test {
                void test() {
                    new Runnable() {
                        void inner() {
                        }
                    };
                }
            }
        """,
        after = """
            class Test {
                void test(Object[]... values) {
                    new Runnable() {
                        void inner(Object[]... values) {
                        }
                    };
                }
            }
        """,
        afterConditions = { cu ->
            val type = (cu.classes.first().body.statements.first() as J.MethodDeclaration).type!!

            assertThat(type.paramNames)
                .`as`("Changing the method's parameters should have also updated its type's parameter names")
                .containsExactly("values")
            assertThat(type.resolvedSignature!!.paramTypes[0])
                .`as`("Changing the method's parameters should have resulted in the first parameter's type being 'Object[]'")
                .matches {
                    it is JavaType.Array && it.elemType.hasElementType("java.lang.Object")
                }
        }
    )

    @Test
    fun replaceAndInterpolateMethodParameters(jp: JavaParser) = assertChanged(
        jp,
        recipe = toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor }, "int n, #{}")
                    .build()

                override fun visitMethodDeclaration(
                    method: J.MethodDeclaration,
                    p: ExecutionContext
                ): J.MethodDeclaration {
                    if (method.simpleName == "test" && method.parameters.size == 1) {
                        return method.withTemplate(
                            t,
                            method.coordinates.replaceParameters(),
                            method.parameters[0]
                        )
                    }
                    return method
                }
            }
        },
        before = """
            class Test {
                void test(String s) {
                }
            }
        """,
        after = """
            class Test {
                void test(int n, String s) {
                }
            }
        """,
        afterConditions = { cu ->
            val type = (cu.classes.first().body.statements.first() as J.MethodDeclaration).type!!

            assertThat(type.paramNames)
                .`as`("Changing the method's parameters should have also updated its type's parameter names")
                .containsExactly("n", "s")
            assertThat(type.resolvedSignature!!.paramTypes[0])
                .`as`("Changing the method's parameters should have resulted in the first parameter's type being 'int'")
                .isEqualTo(JavaType.Primitive.Int)
            assertThat(type.resolvedSignature!!.paramTypes[1])
                .`as`("Changing the method's parameters should have resulted in the second parameter's type being 'List<String>'")
                .matches { it is JavaType.FullyQualified && it.fullyQualifiedName == "java.lang.String" }
        }
    )

    @Test
    fun replaceLambdaParameters(jp: JavaParser) = assertChanged(
        jp,
        recipe = toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor }, "int m, int n")
                    .build()

                override fun visitLambda(lambda: J.Lambda, p: ExecutionContext): J.Lambda =
                    if (lambda.parameters.parameters.size == 1) {
                        lambda.withTemplate(t, lambda.parameters.coordinates.replace())
                    } else {
                        super.visitLambda(lambda, p)
                    }
            }
        },
        before = """
            class Test {
                void test() {
                    Object o = () -> 1;
                }
            }
        """,
        after = """
            class Test {
                void test() {
                    Object o = (int m, int n) -> 1;
                }
            }
        """
    )

    @Test
    fun replaceSingleStatement(jp: JavaParser) = assertChanged(
        jp,
        recipe = toRecipe {
            object : JavaVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder(
                    { cursor },
                    "if(n != 1) {\n" +
                            "  n++;\n" +
                            "}"
                )
                    .build()

                override fun visitAssert(_assert: J.Assert, p: ExecutionContext): J =
                    _assert.withTemplate(t, _assert.coordinates.replace())
            }
        },
        before = """
            class Test {
                int n;
                void test() {
                    assert n == 0;
                }
            }
        """,
        after = """
            class Test {
                int n;
                void test() {
                    if (n != 1) {
                        n++;
                    }
                }
            }
        """
    )

    @Suppress("UnusedAssignment")
    @Test
    fun replaceStatementInBlock(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.logCompilationWarningsAndErrors(true).build(),
        recipe = toRecipe {
            object : JavaVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor }, "n = 2;\nn = 3;")
                    .build()

                override fun visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): J {
                    val statement = method.body!!.statements[1]
                    if (statement is J.Unary) {
                        return method.withTemplate(t, statement.coordinates.replace())
                    }
                    return method
                }
            }
        },
        before = """
            class Test {
                int n;
                void test() {
                    n = 1;
                    n++;
                }
            }
        """,
        after = """
            class Test {
                int n;
                void test() {
                    n = 1;
                    n = 2;
                    n = 3;
                }
            }
        """
    )

    @Test
    fun beforeStatementInBlock(jp: JavaParser) = assertChanged(
        jp,
        recipe = toRecipe {
            object : JavaVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor }, "assert n == 0;")
                    .build()

                override fun visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): J {
                    val statement = method.body!!.statements[0]
                    if (statement is J.Assignment) {
                        return method.withTemplate(t, statement.coordinates.before())
                    }
                    return method
                }
            }
        },
        before = """
            class Test {
                int n;
                void test() {
                    n = 1;
                }
            }
        """,
        after = """
            class Test {
                int n;
                void test() {
                    assert n == 0;
                    n = 1;
                }
            }
        """
    )

    @Test
    fun afterStatementInBlock(jp: JavaParser) = assertChanged(
        jp,
        recipe = toRecipe {
            object : JavaVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor }, "n = 1;")
                    .build()

                override fun visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): J {
                    if (method.body!!.statements.size == 1) {
                        return method.withTemplate(t, method.body!!.statements[0].coordinates.after())
                    }
                    return method
                }
            }
        },
        before = """
            class Test {
                int n;
                void test() {
                    assert n == 0;
                }
            }
        """,
        after = """
            class Test {
                int n;
                void test() {
                    assert n == 0;
                    n = 1;
                }
            }
        """
    )

    @Test
    fun lastStatementInClassBlock(jp: JavaParser) = assertChanged(
        jp,
        recipe = toRecipe {
            object : JavaVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor }, "int n;")
                    .build()

                override fun visitClassDeclaration(classDecl: J.ClassDeclaration, p: ExecutionContext): J {
                    if (classDecl.body.statements.isEmpty()) {
                        return classDecl.withTemplate(t, classDecl.body.coordinates.lastStatement())
                    }
                    return classDecl
                }
            }
        },
        before = """
            class Test {
            }
        """,
        after = """
            class Test {
                int n;
            }
        """
    )

    @Test
    fun lastStatementInMethodBlock(jp: JavaParser) = assertChanged(
        jp,
        recipe = toRecipe {
            object : JavaVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor }, "n = 1;")
                    .build()

                override fun visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): J {
                    if (method.body!!.statements.size == 1) {
                        return method.withTemplate(t, method.body!!.coordinates.lastStatement())
                    }
                    return method
                }
            }
        },
        before = """
            class Test {
                int n;
                void test() {
                    assert n == 0;
                }
            }
        """,
        after = """
            class Test {
                int n;
                void test() {
                    assert n == 0;
                    n = 1;
                }
            }
        """
    )

    @Test
    fun replaceStatementRequiringNewImport(jp: JavaParser) = assertChanged(
        jp,
        recipe = toRecipe {
            object : JavaVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor }, "List<String> s = null;")
                    .imports("java.util.List")
                    .build()

                override fun visitAssert(_assert: J.Assert, p: ExecutionContext): J {
                    maybeAddImport("java.util.List")
                    return _assert.withTemplate(t, _assert.coordinates.replace())
                }
            }
        },
        before = """
            class Test {
                int n;
                void test() {
                    assert n == 0;
                }
            }
        """,
        after = """
            import java.util.List;
            
            class Test {
                int n;
                void test() {
                    List<String> s = null;
                }
            }
        """
    )

    @Suppress("UnnecessaryBoxing")
    @Test
    fun replaceArguments(jp: JavaParser) = assertChanged(
        jp,
        recipe = toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor }, "m, Integer.valueOf(n), \"foo\"")
                    .build()

                override fun visitMethodInvocation(
                    method: J.MethodInvocation,
                    p: ExecutionContext
                ): J.MethodInvocation {
                    if (method.arguments.size == 1) {
                        return method.withTemplate(t, method.coordinates.replaceArguments())
                    }
                    return method
                }
            }
        },
        before = """
            abstract class Test {
                abstract void test();
                abstract void test(int m, int n, String foo);
                void fred(int m, int n, String foo) {
                    test();
                }
            }
        """,
        after = """
            abstract class Test {
                abstract void test();
                abstract void test(int m, int n, String foo);
                void fred(int m, int n, String foo) {
                    test(m, Integer.valueOf(n), "foo");
                }
            }
        """,
        afterConditions = { cu ->
            val m = (cu.classes[0].body.statements[2] as J.MethodDeclaration).body!!.statements[0] as J.MethodInvocation
            val type = m.type!!
            assertThat(type.genericSignature!!.paramTypes[0]).isEqualTo(JavaType.Primitive.Int)
            assertThat(type.genericSignature!!.paramTypes[1]).isEqualTo(JavaType.Primitive.Int)
            assertThat(type.genericSignature!!.paramTypes[2])
                .matches { (it as JavaType.FullyQualified).fullyQualifiedName.equals("java.lang.String") }
        }
    )

    @Test
    fun replaceClassAnnotation(jp: JavaParser) = assertChanged(
        jp,
        recipe = toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor }, "@Deprecated")
                    .build()

                override fun visitAnnotation(annotation: J.Annotation, p: ExecutionContext): J.Annotation {
                    if (annotation.simpleName == "SuppressWarnings") {
                        return annotation.withTemplate(t, annotation.coordinates.replace())
                    }
                    return super.visitAnnotation(annotation, p)
                }
            }
        },
        before = "@SuppressWarnings(\"ALL\") class Test {}",
        after = "@Deprecated class Test {}"
    )

    @Test
    fun replaceMethodAnnotations(jp: JavaParser) = assertChanged(
        jp,
        recipe = toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor }, "@SuppressWarnings(\"other\")")
                    .build()

                override fun visitMethodDeclaration(
                    method: J.MethodDeclaration,
                    p: ExecutionContext
                ): J.MethodDeclaration {
                    if (method.leadingAnnotations.size == 0) {
                        return method.withTemplate(t, method.coordinates.replaceAnnotations())
                    }
                    return super.visitMethodDeclaration(method, p)
                }
            }
        },
        before = """
            class Test {
                static final String WARNINGS = "ALL";
            
                public @SuppressWarnings(WARNINGS) Test() {
                }
            
                public void test1() {
                }
            
                public @SuppressWarnings(WARNINGS) void test2() {
                }
            }
        """,
        after = """
            class Test {
                static final String WARNINGS = "ALL";
            
                @SuppressWarnings("other")
                public Test() {
                }
            
                @SuppressWarnings("other")
                public void test1() {
                }
            
                @SuppressWarnings("other")
                public void test2() {
                }
            }
        """
    )

    @Test
    fun replaceClassAnnotations(jp: JavaParser) = assertChanged(
        jp,
        recipe = toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor }, "@SuppressWarnings(\"other\")")
                    .build()

                override fun visitClassDeclaration(
                    classDecl: J.ClassDeclaration,
                    p: ExecutionContext
                ): J.ClassDeclaration {
                    if (classDecl.leadingAnnotations.size == 0 && classDecl.simpleName != "Test") {
                        return classDecl.withTemplate(t, classDecl.coordinates.replaceAnnotations())
                    }
                    return super.visitClassDeclaration(classDecl, p)
                }
            }
        },
        before = """
            class Test {
                static final String WARNINGS = "ALL";
                
                class Inner1 {
                }
            }
        """,
        after = """
            class Test {
                static final String WARNINGS = "ALL";
            
                @SuppressWarnings("other")
                class Inner1 {
                }
            }
        """
    )

    @Test
    fun replaceVariableAnnotations(jp: JavaParser) = assertChanged(
        jp,
        recipe = toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor }, "@SuppressWarnings(\"other\")")
                    .build()

                override fun visitVariableDeclarations(
                    multiVariable: J.VariableDeclarations,
                    p: ExecutionContext,
                ): J.VariableDeclarations {
                    if (multiVariable.leadingAnnotations.size == 0) {
                        return multiVariable.withTemplate(t, multiVariable.coordinates.replaceAnnotations())
                    }
                    return super.visitVariableDeclarations(multiVariable, p)
                }
            }
        },
        before = """
            class Test {
                void test() {
                    // the m
                    int m;
                    final @SuppressWarnings("ALL") int n;
                }
            }
        """,
        after = """
            class Test {
                void test() {
                    // the m
                    @SuppressWarnings("other")
                    int m;
                    @SuppressWarnings("other")
                    final int n;
                }
            }
        """
    )

    @Test
    fun addVariableAnnotationsToVariableAlreadyAnnotated(jp: JavaParser) = assertChanged(
        jp,
        recipe = toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor }, "@Deprecated")
                    .build()

                override fun visitVariableDeclarations(
                    multiVariable: J.VariableDeclarations,
                    p: ExecutionContext,
                ): J.VariableDeclarations {
                    if (multiVariable.leadingAnnotations.size == 1) {
                        return multiVariable.withTemplate(t, multiVariable.coordinates.addAnnotation(comparing { 0 }))
                    }
                    return super.visitVariableDeclarations(multiVariable, p)
                }
            }
        },
        before = """
            class Test {
                void test() {
                    @SuppressWarnings("ALL") /* hello */
                    Boolean z;
                    @SuppressWarnings("ALL") private final int m, a;
                    // comment n
                    @SuppressWarnings("ALL")
                    int n;
                    @SuppressWarnings("ALL") final Boolean b;
                    @SuppressWarnings("ALL")
                    // comment x, y
                    private Boolean x, y;
                }
            }
        """,
        after = """
            class Test {
                void test() {
                    @SuppressWarnings("ALL")
                    @Deprecated /* hello */
                    Boolean z;
                    @SuppressWarnings("ALL")
                    @Deprecated
                    private final int m, a;
                    // comment n
                    @SuppressWarnings("ALL")
                    @Deprecated
                    int n;
                    @SuppressWarnings("ALL")
                    @Deprecated
                    final Boolean b;
                    @SuppressWarnings("ALL")
                    @Deprecated
                    // comment x, y
                    private Boolean x, y;
                }
            }
        """
    )

    @Test
    fun addVariableAnnotationsToVariableNotAnnotated(jp: JavaParser) = assertChanged(
        jp,
        recipe = toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor }, "@SuppressWarnings(\"ALL\")")
                    .build()

                override fun visitVariableDeclarations(
                    multiVariable: J.VariableDeclarations,
                    p: ExecutionContext,
                ): J.VariableDeclarations {
                    if (multiVariable.leadingAnnotations.size == 0) {
                        return multiVariable.withTemplate(
                            t,
                            multiVariable.coordinates.addAnnotation(comparing { it.simpleName })
                        )
                    }
                    return super.visitVariableDeclarations(multiVariable, p)
                }
            }
        },
        before = """
            class Test {
                void test() {
                    final int m;
                    int n;
                }
            }
        """,
        after = """
            class Test {
                void test() {
                    @SuppressWarnings("ALL")
                    final int m;
                    @SuppressWarnings("ALL")
                    int n;
                }
            }
        """
    )

    @Test
    fun addMethodAnnotations(jp: JavaParser) = assertChanged(
        jp,
        recipe = toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor }, "@SuppressWarnings(\"other\")")
                    .build()

                override fun visitMethodDeclaration(
                    method: J.MethodDeclaration,
                    p: ExecutionContext
                ): J.MethodDeclaration {
                    if (method.leadingAnnotations.size == 0) {
                        return method.withTemplate(t, method.coordinates.addAnnotation(comparing { it.simpleName }))
                    }
                    return super.visitMethodDeclaration(method, p)
                }
            }
        },
        before = """
            class Test {
                public void test0() {
                }

                static final String WARNINGS = "ALL";

                public void test1() {
                }
            }
        """,
        after = """
            class Test {
                @SuppressWarnings("other")
                public void test0() {
                }

                static final String WARNINGS = "ALL";

                @SuppressWarnings("other")
                public void test1() {
                }
            }
        """
    )

    @Test
    fun addClassAnnotations(jp: JavaParser) = assertChanged(
        jp,
        recipe = toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor }, "@SuppressWarnings(\"other\")")
                    .build()

                override fun visitClassDeclaration(
                    classDecl: J.ClassDeclaration,
                    p: ExecutionContext
                ): J.ClassDeclaration {
                    if (classDecl.leadingAnnotations.size == 0 && classDecl.simpleName != "Test") {
                        return classDecl.withTemplate(
                            t,
                            classDecl.coordinates.addAnnotation(comparing { it.simpleName })
                        )
                    }
                    return super.visitClassDeclaration(classDecl, p)
                }
            }
        },
        before = """
            class Test {
                class Inner1 {
                }
            }
        """,
        after = """
            class Test {
            
                @SuppressWarnings("other")
                class Inner1 {
                }
            }
        """
    )

    @Test
    fun replaceClassImplements(jp: JavaParser) = assertChanged(
        jp,
        recipe = toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor }, "Serializable, Closeable")
                    .imports("java.io.*")
                    .build()

                override fun visitClassDeclaration(
                    classDecl: J.ClassDeclaration,
                    p: ExecutionContext
                ): J.ClassDeclaration {
                    if (classDecl.implements == null) {
                        maybeAddImport("java.io.Closeable")
                        maybeAddImport("java.io.Serializable")
                        return classDecl.withTemplate(t, classDecl.coordinates.replaceImplementsClause())
                    }
                    return super.visitClassDeclaration(classDecl, p)
                }
            }
        },
        before = """
            class Test {
            }
        """,
        after = """
            import java.io.Closeable;
            import java.io.Serializable;
            
            class Test implements Serializable, Closeable {
            }
        """
    )

    @Test
    fun replaceClassExtends(jp: JavaParser) = assertChanged(
        jp,
        recipe = toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor }, "List<String>")
                    .imports("java.util.*")
                    .build()

                override fun visitClassDeclaration(
                    classDecl: J.ClassDeclaration,
                    p: ExecutionContext
                ): J.ClassDeclaration {
                    if (classDecl.extends == null) {
                        maybeAddImport("java.util.List")
                        return classDecl.withTemplate(t, classDecl.coordinates.replaceExtendsClause())
                    }
                    return super.visitClassDeclaration(classDecl, p)
                }
            }
        },
        before = """
            class Test {
            }
        """,
        after = """
            import java.util.List;
            
            class Test extends List<String> {
            }
        """
    )

    @Suppress("RedundantThrows")
    @Test
    fun replaceThrows(jp: JavaParser) = assertChanged(
        jp,
        recipe = toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor }, "Exception")
                    .build()

                override fun visitMethodDeclaration(
                    method: J.MethodDeclaration,
                    p: ExecutionContext
                ): J.MethodDeclaration {
                    if (method.throws == null) {
                        return method.withTemplate(t, method.coordinates.replaceThrows())
                    }
                    return super.visitMethodDeclaration(method, p)
                }
            }
        },
        before = """
            class Test {
                void test() {}
            }
        """,
        after = """
            class Test {
                void test() throws Exception {}
            }
        """,
        afterConditions = { cu ->
            val testMethodDecl = cu.classes.first().body.statements.first() as J.MethodDeclaration
            assertThat(testMethodDecl.type!!.thrownExceptions.map { it.fullyQualifiedName })
                .containsExactly("java.lang.Exception")
        }
    )

    @Test
    fun replaceMethodTypeParameters(jp: JavaParser) = assertChanged(
        jp,
        recipe = toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                val typeParamsTemplate = JavaTemplate.builder({ cursor }, "T, U")
                    .build()

                val methodArgsTemplate = JavaTemplate.builder({ cursor }, "List<T> t, U u")
                    .imports("java.util.List")
                    .build()

                override fun visitMethodDeclaration(
                    method: J.MethodDeclaration,
                    p: ExecutionContext
                ): J.MethodDeclaration {
                    if (method.typeParameters == null) {
                        return method.withTemplate<J.MethodDeclaration>(
                            typeParamsTemplate,
                            method.coordinates.replaceTypeParameters()
                        )
                            .withTemplate(methodArgsTemplate, method.coordinates.replaceParameters())
                    }
                    return super.visitMethodDeclaration(method, p)
                }
            }
        },
        before = """
            import java.util.List;
            
            class Test {
            
                void test() {
                }
            }
        """,
        after = """
            import java.util.List;
            
            class Test {
            
                <T, U> void test(List<T> t, U u) {
                }
            }
        """,
        typeValidation = {
            identifiers = false
        },
        afterConditions = { cu ->
            val type = (cu.classes.first().body.statements.first() as J.MethodDeclaration).type!!
            assertThat(type).isNotNull
            val paramTypes = type.genericSignature!!.paramTypes

            assertThat(paramTypes[0])
                .`as`("The method declaration's type's genericSignature first argument should have have type 'java.util.List'")
                .matches { tType ->
                    tType is JavaType.FullyQualified && tType.fullyQualifiedName == "java.util.List"
                }

            assertThat(paramTypes[1])
                .`as`("The method declaration's type's genericSignature second argument should have type 'U' with bound 'java.lang.Object'")
                .matches { uType ->
                    uType is JavaType.GenericTypeVariable &&
                            uType.fullyQualifiedName == "U" &&
                            uType.bound!!.fullyQualifiedName == "java.lang.Object"
                }
        }
    )

    @Test
    fun replaceClassTypeParameters(jp: JavaParser) = assertChanged(
        jp,
        recipe = toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor }, "T, U")
                    .build()

                override fun visitClassDeclaration(
                    classDecl: J.ClassDeclaration,
                    p: ExecutionContext
                ): J.ClassDeclaration {
                    if (classDecl.typeParameters == null) {
                        return classDecl.withTemplate(t, classDecl.coordinates.replaceTypeParameters())
                    }
                    return super.visitClassDeclaration(classDecl, p)
                }
            }
        },
        before = """
            class Test {
            }
        """,
        after = """
            class Test<T, U> {
            }
        """
    )

    @Test
    fun replaceBody(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.logCompilationWarningsAndErrors(true).build(),
        recipe = toRecipe {
            object : JavaVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor }, "n = 1;")
                    .build()

                override fun visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): J {
                    val statement = method.body!!.statements[0]
                    if (statement is J.Unary) {
                        return method.withTemplate(t, method.coordinates.replaceBody())
                    }
                    return method
                }
            }
        },
        before = """
            class Test {
                int n;
                void test() {
                    n++;
                }
            }
        """,
        after = """
            class Test {
                int n;
                void test() {
                    n = 1;
                }
            }
        """
    )

    @Test
    fun replaceMissingBody(jp: JavaParser.Builder<*, *>) = assertChanged(
        jp.logCompilationWarningsAndErrors(true).build(),
        recipe = toRecipe {
            object : JavaVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor }, "")
                    .build()

                override fun visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): J {
                    var m = method
                    if (!m.isAbstract) {
                        return m
                    }
                    m = m.withReturnTypeExpression(m.returnTypeExpression!!.withPrefix(Space.EMPTY))
                    m = m.withModifiers(emptyList())

                    m = m.withTemplate(t, m.coordinates.replaceBody())

                    return m
                }
            }
        },
        before = """
            abstract class Test {
                abstract void test();
            }
        """,
        after = """
            abstract class Test {
                void test(){
                }
            }
        """
    )
}