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

import org.openrewrite.internal.lang.Nullable;

import java.util.List;

import static java.util.Collections.emptyList;

public class TypeUtils {
    private TypeUtils() {
    }

    public static List<CType.Variable> getVisibleSupertypeMembers(@Nullable CType type) {
        CType.FullyQualified classType = TypeUtils.asFullyQualified(type);
        return classType == null ? emptyList() : classType.getVisibleSupertypeMembers();
    }

    public static boolean isString(@Nullable CType type) {
        return type == CType.Primitive.String ||
                ( type instanceof CType.FullyQualified &&
                        "java.lang.String".equals(((CType.FullyQualified) type).getFullyQualifiedName())
                );
    }

    public static boolean isOfType(@Nullable CType type1, @Nullable CType type2) {
        if(type1 == null || type2 == null) {
            return false;
        }
        // Strings, uniquely amongst all other types, can be either primitives or classes depending on the context
        if(TypeUtils.isString(type1) && TypeUtils.isString(type2)) {
            return true;
        }
        if(type1 instanceof CType.Primitive && type2 instanceof CType.Primitive) {
            return ((CType.Primitive) type1).getKeyword().equals(((CType.Primitive)type2).getKeyword());
        }
        if(type1 instanceof CType.FullyQualified && type2 instanceof CType.FullyQualified) {
            return ((CType.FullyQualified) type1).getFullyQualifiedName().equals(((CType.FullyQualified) type2).getFullyQualifiedName());
        }
        if(type1 instanceof CType.Array && type2 instanceof CType.Array) {
            return isOfType(((CType.Array)type1).getElemType(), ((CType.Array)type2).getElemType());
        }

        return type1.deepEquals(type2);
    }

    public static boolean isOfClassType(@Nullable CType type, String fqn) {
        CType.FullyQualified classType = asFullyQualified(type);
        return classType != null && classType.getFullyQualifiedName().equals(fqn);
    }

    public static boolean isAssignableTo(@Nullable CType to, @Nullable CType from) {
        if (from == CType.Class.OBJECT) {
            return to == CType.Class.OBJECT;
        }

        CType.FullyQualified classTo = asFullyQualified(to);
        CType.FullyQualified classFrom = asFullyQualified(from);

        if (classTo == null || classFrom == null) {
            return false;
        }

        return classTo.getFullyQualifiedName().equals(classFrom.getFullyQualifiedName()) ||
                isAssignableTo(to, classFrom.getSupertype()) ||
                classFrom.getInterfaces().stream().anyMatch(i -> isAssignableTo(to, i));
    }

    /**
     * @deprecated This method is being deprecated, please use asFullyQualified() instead.
     */
    @Nullable
    @Deprecated
    public static CType.Class asClass(@Nullable CType type) {
        return type instanceof CType.Class ? (CType.Class) type : null;
    }

    @Nullable
    public static CType.Parameterized asParameterized(@Nullable CType type) {
        return type instanceof CType.Parameterized ? (CType.Parameterized) type : null;
    }

    @Nullable
    public static CType.Array asArray(@Nullable CType type) {
        return type instanceof CType.Array ? (CType.Array) type : null;
    }

    @Nullable
    public static CType.GenericTypeVariable asGeneric(@Nullable CType type) {
        return type instanceof CType.GenericTypeVariable ? (CType.GenericTypeVariable) type : null;
    }

    @Nullable
    public static CType.Method asMethod(@Nullable CType type) {
        return type instanceof CType.Method ? (CType.Method) type : null;
    }

    @Nullable
    public static CType.Primitive asPrimitive(@Nullable CType type) {
        return type instanceof CType.Primitive ? (CType.Primitive) type : null;
    }

    @Nullable
    public static CType.FullyQualified asFullyQualified(@Nullable CType type) {
        return type instanceof CType.FullyQualified ? (CType.FullyQualified) type : null;
    }

    public static boolean hasElementTypeAssignable(@Nullable CType type, String fullyQualifiedName) {
        if (type instanceof CType.Array) {
            return hasElementType(((CType.Array) type).getElemType(), fullyQualifiedName);
        }
        if (type instanceof CType.Class || type instanceof CType.GenericTypeVariable) {
            return isAssignableTo(CType.Class.build(fullyQualifiedName), type);
        }
        return false;
    }

    public static boolean hasElementType(@Nullable CType type, String fullyQualifiedName) {
        if (type instanceof CType.Array) {
            return hasElementType(((CType.Array) type).getElemType(), fullyQualifiedName);
        }
        if (type instanceof CType.Class || type instanceof CType.GenericTypeVariable) {
            return fullyQualifiedName.equals(((CType.FullyQualified) type).getFullyQualifiedName());
        }
        return false;
    }

    static boolean deepEquals(@Nullable List<? extends CType> ts1, @Nullable List<? extends CType> ts2) {

        if (ts1 == null || ts2 == null) {
            return ts1 == null && ts2 == null;
        }

        if (ts1.size() != ts2.size()) {
            return false;
        }

        for (int i = 0; i < ts1.size(); i++) {
            CType t1 = ts1.get(i);
            CType t2 = ts2.get(i);
            if (t1 == null) {
                if (t2 != null) {
                    return false;
                }
            } else if (!deepEquals(t1, t2)) {
                return false;
            }
        }

        return true;
    }

    static boolean deepEquals(@Nullable CType t, @Nullable CType t2) {
        return t == null ? t2 == null : t == t2 || t.deepEquals(t2);
    }
}
