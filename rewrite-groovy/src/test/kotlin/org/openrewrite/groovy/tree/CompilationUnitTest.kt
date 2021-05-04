package org.openrewrite.groovy.tree

import org.junit.jupiter.api.Test

class CompilationUnitTest : GroovyTreeTest {

    @Test
    fun packageDecl() = assertParsePrintAndProcess(
        """
            package org.openrewrite
            def a = 'hello'
        """.trimIndent()
    )

    @Test
    fun mixedImports() = assertParsePrintAndProcess(
        """
            def a = 'hello'
            import java.util.List
            List l = null
        """.trimIndent()
    )
}
