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

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.family.c.tree.C;
import org.openrewrite.family.c.tree.CType;
import org.openrewrite.family.c.tree.TypeUtils;
import org.openrewrite.marker.Markers;

@EqualsAndHashCode(callSuper = true)
@Data
public class ChangeFieldType<P> extends CIsoVisitor<P> {
    private final String oldFullyQualifiedTypeName;
    private final CType.FullyQualified newFieldType;

    public ChangeFieldType(CType.FullyQualified oldFieldType, CType.FullyQualified newFieldType) {
        this.oldFullyQualifiedTypeName = oldFieldType.getFullyQualifiedName();
        this.newFieldType  = newFieldType;
    }

    @Override
    public C.VariableDeclarations visitVariableDeclarations(C.VariableDeclarations multiVariable, P p) {
        CType.FullyQualified typeAsClass = multiVariable.getTypeAsFullyQualified();
        C.VariableDeclarations mv = super.visitVariableDeclarations(multiVariable, p);
        if (typeAsClass != null && oldFullyQualifiedTypeName.equals(typeAsClass.getFullyQualifiedName())) {

            maybeAddImport(newFieldType);
            maybeRemoveImport(typeAsClass);

            mv = mv.withTypeExpression(mv.getTypeExpression() == null ?
                    null :
                    C.Identifier.build(mv.getTypeExpression().getId(),
                            mv.getTypeExpression().getPrefix(),
                            Markers.EMPTY,
                            newFieldType.getClassName(),
                            newFieldType)
            );

            mv = mv.withVariables(ListUtils.map(mv.getVariables(), var -> {
                CType.FullyQualified varType = TypeUtils.asFullyQualified(var.getType());
                if (varType != null && !varType.equals(newFieldType)) {
                    return var.withType(newFieldType).withName(var.getName().withType(newFieldType));
                }
                return var;
            }));
        }

        return mv;
    }
}
