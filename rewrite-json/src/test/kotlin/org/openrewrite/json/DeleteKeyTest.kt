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
package org.openrewrite.json

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.Issue

class DeleteKeyTest : JsonRecipeTest {
    @Test
    fun deleteNestedKey() = assertChanged(
        recipe = DeleteKey("$.metadata.name", null),
        before = """
            {
              "apiVersion": "v1",
              "metadata": {
                "name": "monitoring-tools",
                "namespace": "monitoring-tools"
              }
            }
        """,
        after = """
            {
              "apiVersion": "v1",
              "metadata": {
                "namespace": "monitoring-tools"
              }
            }
        """
    )

    @Test
    fun deleteArrayKey() = assertChanged(
        recipe = DeleteKey("$.subjects.kind", null),
        before = """
            {
              "subjects": [
                {
                  "kind": "ServiceAccount",
                  "name": "monitoring-tools"
                }
              ]
            }
        """,
        after = """
            {
              "subjects": [
                {
                  "name": "monitoring-tools"
                }
              ]
            }
        """
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1175")
    @Disabled
    fun deleteNestedKeyRemovingUnusedKeysRecursively() = assertChanged(
        recipe = DeleteKey("$.b.c.d", null),
        before = """
            {
              "a": "a-value",
              "b": {
                "c": {
                  "d": "d-value"
                }
              }
            }
        """,
        after = """
            {
              "a": "a-value"
            }
        """
    )

}