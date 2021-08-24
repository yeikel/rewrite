package org.openrewrite.groovy.tree

import org.junit.jupiter.api.Test

class LiteralTest : GroovyTreeTest {

    @Test
    fun string() = assertParsePrintAndProcess(
        "def a = 'hello'"
    )

    @Test
    fun nullValue() = assertParsePrintAndProcess(
        "def a = null"
    )

    @Test
    fun boxedInt() = assertParsePrintAndProcess(
        "Integer a = 1"
    )
}
