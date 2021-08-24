package org.openrewrite.java.style;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.openrewrite.style.NamedStyles;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptySet;
import static org.openrewrite.Tree.randomId;

public class IntelliJ extends NamedStyles {
    private static final IntelliJ INSTANCE = new IntelliJ();

    private IntelliJ() {
        super(randomId(),
                "org.openrewrite.java.IntelliJ",
                "IntelliJ IDEA",
                "IntelliJ IDEA defaults for styles.",
                emptySet(),
                Stream.concat(
                        org.openrewrite.family.c.style.IntelliJ.defaults().getStyles().stream(),
                        Stream.of(importLayout())
                ).collect(Collectors.toList())
        );
    }

    @JsonCreator
    public static IntelliJ defaults() {
        return INSTANCE;
    }

    public static ImportLayoutStyle importLayout() {
        return ImportLayoutStyle.builder()
                .importAllOthers()
                .blankLine()
                .importPackage("javax.*")
                .importPackage("java.*")
                .blankLine()
                .importStaticAllOthers()
                .build();
    }
}
