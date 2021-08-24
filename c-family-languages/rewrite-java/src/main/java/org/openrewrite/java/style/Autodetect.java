package org.openrewrite.java.style;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import org.openrewrite.family.c.tree.CType;
import org.openrewrite.family.c.tree.TypeUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.style.Style;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptySet;
import static org.openrewrite.Tree.randomId;

public class Autodetect extends NamedStyles {
    @JsonCreator
    public Autodetect(UUID id, Collection<Style> styles) {
        super(id,
                "org.openrewrite.java.Autodetect",
                "Auto-detected",
                "Automatically detected styles from a repository's existing code.",
                emptySet(), styles);
    }

    public static Autodetect detect(List<J.CompilationUnit> cus) {
        ImportLayoutStatistics importLayoutStatistics = new ImportLayoutStatistics();
        importLayoutStatistics.mapBlockPatterns(cus);
        for (J.CompilationUnit cu : cus) {
            new FindImportLayout().visit(cu, importLayoutStatistics);
        }

        return new Autodetect(
                randomId(),
                Stream.concat(
                        org.openrewrite.family.c.style.Autodetect.detect(cus).getStyles().stream(),
                        Stream.of(importLayoutStatistics.getImportLayoutStyle())
                ).collect(Collectors.toList())
        );
    }

    private static class ImportLayoutStatistics {
        List<List<Block>> blocksPerSourceFile = new ArrayList<>();
        Map<String, String> pkgToBlockPattern = new LinkedHashMap<>();
        int staticAtTopCount = 0;
        int staticAtBotCount = 0;
        int javaBeforeJavaxCount = 0;
        int javaxBeforeJavaCount = 0;
        int minimumFoldedImports = Integer.MAX_VALUE;
        int minimumFoldedStaticImports = Integer.MAX_VALUE;

        public boolean isStaticImportsAtBot() {
            return staticAtBotCount >= staticAtTopCount;
        }

        public boolean isJavaxBeforeJava() {
            return javaxBeforeJavaCount >= javaBeforeJavaxCount;
        }

        enum BlockType {
            Import,
            ImportStatic
        }

        public ImportLayoutStyle getImportLayoutStyle() {
            // the simplest heuristic is just to take the single longest block sequence and
            // assume that represents the most variation in the project
            return blocksPerSourceFile.stream()
                    .max(Comparator
                            .<List<Block>, Integer>comparing(List::size)
                            .thenComparing(blocks -> blocks.stream()
                                    .filter(b -> "all other imports".equals(b.pattern))
                                    .count()
                            )
                    )
                    .map(longestBlocks -> {
                        ImportLayoutStyle.Builder builder = ImportLayoutStyle.builder();
                        boolean insertAllOthers = false;
                        boolean insertStaticAllOthers = false;
                        boolean containsJava = false;
                        boolean containsJavax = false;

                        int insertAllOtherAtIndex = 0;
                        int insertStaticAllOtherAtIndex = 0;
                        int nonStaticMaxCount = Integer.MIN_VALUE;
                        int staticMaxCount = Integer.MIN_VALUE;
                        int nonStaticCountPos = 0;
                        int staticCountPos = 0;
                        int nonStaticPos = 0;
                        int staticPos = 0;

                        List<Block> nonStaticBlocks = new ArrayList<>(); // Isolate static imports to add at top or bottom of layout.
                        List<Block> staticBlocks = new ArrayList<>(); // Isolate static imports to add at top or bottom of layout.
                        List<Integer> countOfBlocksInNonStaticGroups = new ArrayList<>();
                        List<Integer> countOfBlocksInStaticGroups = new ArrayList<>();

                        for (Block block : longestBlocks) {
                            if (BlockType.ImportStatic.equals(block.type)) {
                                staticBlocks.add(block);
                                countOfBlocksInStaticGroups.add(0);
                                countOfBlocksInStaticGroups.set(staticCountPos, countOfBlocksInStaticGroups.get(staticCountPos) + 1);
                                if (staticMaxCount < countOfBlocksInStaticGroups.get(staticCountPos)) {
                                    staticMaxCount = countOfBlocksInStaticGroups.get(staticCountPos);
                                    insertStaticAllOtherAtIndex = staticCountPos;
                                    insertStaticAllOthers = true;
                                }

                                if (block.addBlankLine) {
                                    staticCountPos = staticPos + 1;
                                }
                                staticPos++;
                            } else {
                                if (!containsJava && block.pattern.equals("java.*")) {
                                    containsJava = true;
                                }

                                if (!containsJavax && block.pattern.equals("javax.*")) {
                                    containsJavax = true;
                                }

                                nonStaticBlocks.add(block);
                                countOfBlocksInNonStaticGroups.add(0);
                                countOfBlocksInNonStaticGroups.set(nonStaticCountPos, countOfBlocksInNonStaticGroups.get(nonStaticCountPos) + 1);
                                if (nonStaticMaxCount < countOfBlocksInNonStaticGroups.get(nonStaticCountPos)) {
                                    nonStaticMaxCount = countOfBlocksInNonStaticGroups.get(nonStaticCountPos);
                                    insertAllOtherAtIndex = nonStaticCountPos;
                                    insertAllOthers = true;
                                }

                                if (block.addBlankLine) {
                                    nonStaticCountPos = nonStaticPos + 1;
                                }
                                nonStaticPos++;
                            }
                        }

                        // Add static imports at the top if it's the standard.
                        boolean addNewLine = false;
                        if (!isStaticImportsAtBot()) {
                            // There are no static imports, add an all other import block.
                            if (!insertStaticAllOthers) {
                                builder = builder.importStaticAllOthers();
                            }

                            for (int i = 0; i < staticBlocks.size(); i++) {
                                // Insert the static all others block.
                                if (insertStaticAllOthers) {
                                    if (i == insertStaticAllOtherAtIndex) {
                                        builder = builder.importStaticAllOthers();
                                        addNewLine = true;
                                        continue;
                                    } else {
                                        if (countOfBlocksInStaticGroups.get(i) == 0) {
                                            continue;
                                        } else {
                                            insertStaticAllOthers = false;
                                        }
                                    }
                                }

                                if (addNewLine) {
                                    builder = builder.blankLine();
                                    addNewLine = false;
                                }

                                Block block = staticBlocks.get(i);
                                builder.staticImportPackage(block.pattern);
                                if (block.addBlankLine && i != staticBlocks.size() - 1) {
                                    builder = builder.blankLine();
                                }
                            }
                        }
                        addNewLine = !isStaticImportsAtBot();

                        // There are no non-static imports, add a block of all other import.
                        if (!insertAllOthers) {
                            if (addNewLine) {
                                builder = builder.blankLine();
                            }

                            builder = builder.importAllOthers();
                            // Add java/javax if they're missing from the block that is being used as a template.
                            if (!containsJava && !containsJavax) {
                                builder = builder.blankLine();
                                if (isJavaxBeforeJava()) {
                                    builder = builder.importPackage("javax.*");
                                    builder = builder.importPackage("java.*");
                                } else {
                                    builder = builder.importPackage("java.*");
                                    builder = builder.importPackage("javax.*");
                                }
                            }
                            addNewLine = true;
                        }

                        boolean addJavaOrJavax = true; // Used to normalize the pos of java and javax imports.
                        for (int i = 0; i < nonStaticBlocks.size(); i++) {
                            if (insertAllOthers) {
                                // Insert the all others block.
                                if (i == insertAllOtherAtIndex) {
                                    if (addNewLine) {
                                        builder = builder.blankLine();
                                        addNewLine = false;
                                    }
                                    builder = builder.importAllOthers();
                                    // Add java/javax if they're missing from the block that is being used as a template.
                                    if (!containsJava && !containsJavax) {
                                        builder = builder.blankLine();
                                        if (isJavaxBeforeJava()) {
                                            builder = builder.importPackage("javax.*");
                                            builder = builder.importPackage("java.*");
                                        } else {
                                            builder = builder.importPackage("java.*");
                                            builder = builder.importPackage("javax.*");
                                        }
                                    }
                                    continue;
                                } else if (i > insertAllOtherAtIndex) {
                                    if (countOfBlocksInNonStaticGroups.get(i) == 0) {
                                        continue;
                                    } else {
                                        insertAllOthers = false;
                                        addNewLine = true;
                                    }
                                }
                            }

                            Block block = nonStaticBlocks.get(i);
                            if (addJavaOrJavax && block.pattern.equals("java.*")) {
                                if (addNewLine) {
                                    builder = builder.blankLine();
                                    addNewLine = false;
                                }

                                if (!(i - 1 >= 0 && nonStaticBlocks.get(i - 1).pattern.equals("javax.*") ||
                                        i + 1 < nonStaticBlocks.size() && nonStaticBlocks.get(i + 1).pattern.equals("javax.*"))) {
                                    if (isJavaxBeforeJava()) {
                                        builder = builder.importPackage("javax.*");
                                        builder = builder.importPackage("java.*");
                                    } else {
                                        builder = builder.importPackage("java.*");
                                        builder = builder.importPackage("javax.*");
                                    }
                                    addNewLine = true;
                                    addJavaOrJavax = false;
                                } else {
                                    builder = builder.importPackage(block.pattern);
                                }
                            } else if (addJavaOrJavax && block.pattern.equals("javax.*")) {
                                if (addNewLine) {
                                    builder = builder.blankLine();
                                    addNewLine = false;
                                }

                                if (!(i - 1 >= 0 && nonStaticBlocks.get(i - 1).pattern.equals("java.*") ||
                                        i + 1 < nonStaticBlocks.size() - 1 && nonStaticBlocks.get(i + 1).pattern.equals("java.*"))) {
                                    if (isJavaxBeforeJava()) {
                                        builder = builder.importPackage("javax.*");
                                        builder = builder.importPackage("java.*");
                                    } else {
                                        builder = builder.importPackage("java.*");
                                        builder = builder.importPackage("javax.*");
                                    }
                                    addNewLine = true;
                                    addJavaOrJavax = false;
                                } else {
                                    builder = builder.importPackage(block.pattern);
                                }
                            } else {
                                if (addNewLine) {
                                    builder = builder.blankLine();
                                    addNewLine = false;
                                }

                                builder = builder.importPackage(block.pattern);
                            }
                            if (block.addBlankLine && i != nonStaticBlocks.size() - 1) {
                                builder = builder.blankLine();
                            }
                        }

                        // Add statics at bottom.
                        if (isStaticImportsAtBot()) {
                            builder = builder.blankLine();
                            addNewLine = false;

                            // There are no static imports, add an all other import block.
                            if (!insertStaticAllOthers) {
                                builder = builder.importStaticAllOthers();
                            }

                            for (int i = 0; i < staticBlocks.size(); i++) {
                                // Insert the static all others block.
                                if (insertStaticAllOthers) {
                                    if (i == insertStaticAllOtherAtIndex) {
                                        builder = builder.importStaticAllOthers();
                                        continue;
                                    } else if (i > insertStaticAllOtherAtIndex) {
                                        if (countOfBlocksInStaticGroups.get(i) == 0) {
                                            continue;
                                        } else {
                                            insertStaticAllOthers = false;
                                            addNewLine = true;
                                        }
                                    }
                                }

                                Block block = staticBlocks.get(i);
                                if (addNewLine || i > 0 && staticBlocks.get(i - 1).addBlankLine) {
                                    builder = builder.blankLine();
                                    addNewLine = false;
                                }

                                builder = builder.staticImportPackage(block.pattern);
                            }
                        }

                        if (longestBlocks.isEmpty()) {
                            builder.importAllOthers();
                            builder.blankLine();
                            if (isJavaxBeforeJava()) {
                                builder = builder.importPackage("javax.*");
                                builder = builder.importPackage("java.*");
                            } else {
                                builder = builder.importPackage("java.*");
                                builder = builder.importPackage("javax.*");
                            }
                            builder.blankLine();
                            builder.importStaticAllOthers();
                        }

                        // set lower limits in case type attribution is really messed up on the project
                        // and we can't effectively count star imports
                        builder.classCountToUseStarImport(Math.max(minimumFoldedImports, 5));
                        builder.nameCountToUseStarImport(Math.max(minimumFoldedStaticImports, 3));

                        return builder.build();
                    })
                    .orElse(IntelliJ.importLayout());
        }

        @EqualsAndHashCode
        static class Block {
            private final BlockType type;
            private final String pattern;
            private final boolean addBlankLine;

            Block(BlockType type, String pattern, boolean addBlankLine) {
                this.type = type;
                this.pattern = pattern;
                this.addBlankLine = addBlankLine;
            }
        }

        /**
         * Maps the imported packages to patterns used to create Blocks in the ImportLayout.
         * Patterns are generated early to prevent block patterns that are too specific.
         * Ex. org.openrewrite.* vs. org.openrewrite.java.test.*
         *
         * @param cus list of compilation units to create Block patterns from.
         */
        public void mapBlockPatterns(List<J.CompilationUnit> cus) {
            Set<String> importedPackages = new TreeSet<>();
            for (J.CompilationUnit cu : cus) {
                for (J.Import anImport : cu.getImports()) {
                    importedPackages.add(anImport.getPackageName() + ".");
                }
            }

            String longestCommonPrefix = null;
            String prevLCP = null;
            List<String> prevPackages = new ArrayList<>();

            for (String pkg : importedPackages) {
                longestCommonPrefix = longestCommonPrefix(pkg, longestCommonPrefix);
                if (!prevPackages.isEmpty() && longestCommonPrefix.chars().filter(c -> c == '.').count() <= 1 && !StringUtils.isNullOrEmpty(prevLCP)) {
                    for (String prev : prevPackages) {
                        if (prevLCP.startsWith("java.")) {
                            prevLCP = "java.";
                        } else if (prevLCP.startsWith("javax.")) {
                            prevLCP = "javax.";
                        }
                        this.pkgToBlockPattern.put(prev, prevLCP + "*");
                    }
                    longestCommonPrefix = pkg;
                    prevPackages.clear();
                }

                prevPackages.add(pkg);
                prevLCP = longestCommonPrefix;
            }

            for (String prev : prevPackages) {
                this.pkgToBlockPattern.put(prev, prevLCP + "*");
            }
        }

        private String longestCommonPrefix(String pkg, @Nullable String lcp) {
            if (lcp == null) {
                return pkg;
            }

            char[] p1 = pkg.toCharArray();
            char[] p2 = lcp.toCharArray();
            int i = 0;
            for (; i < p1.length && i < p2.length; i++) {
                if (p1[i] != p2[i]) {
                    break;
                }
            }
            return lcp.substring(0, i);
        }
    }

    private static class FindImportLayout extends JavaIsoVisitor<ImportLayoutStatistics> {
        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ImportLayoutStatistics importLayoutStatistics) {
            Set<ImportLayoutStatistics.Block> blocks = new LinkedHashSet<>();

            importLayoutStatistics.staticAtBotCount += (cu.getImports().size() > 0 &&
                    cu.getImports().get(cu.getImports().size() - 1).isStatic()) ? 1 : 0;
            importLayoutStatistics.staticAtTopCount += (cu.getImports().size() > 0 &&
                    cu.getImports().get(0).isStatic()) ? 1 : 0;

            boolean staticBlock = false;
            int blockStart = 0;
            int i = 0;
            String previousPkg = "";
            int previousPkgCount = 1;
            int javaPos = Integer.MAX_VALUE;
            int javaxPos = Integer.MAX_VALUE;
            Map<ImportLayoutStatistics.Block, Integer> referenceCount = new HashMap<>();
            for (J.Import anImport : cu.getImports()) {
                previousPkgCount += previousPkg.equals(importLayoutStatistics.pkgToBlockPattern.get(anImport.getPackageName() + ".")) ? 1 : 0;
                boolean containsNewLine = anImport.getPrefix().getWhitespace().contains("\n\n") || anImport.getPrefix().getWhitespace().contains("\r\n\r\n");
                if (containsNewLine ||
                        i > 0 && importLayoutStatistics.pkgToBlockPattern.containsKey(anImport.getPackageName() + ".") &&
                                !previousPkg.equals(importLayoutStatistics.pkgToBlockPattern.get(anImport.getPackageName() + "."))) {
                    if (i - blockStart > 0) {
                        ImportLayoutStatistics.Block block = new ImportLayoutStatistics.Block(
                                staticBlock ?
                                        ImportLayoutStatistics.BlockType.ImportStatic :
                                        ImportLayoutStatistics.BlockType.Import,
                                previousPkg,
                                containsNewLine);

                        javaPos = block.pattern.equals("java.*") && javaPos > blockStart ? blockStart : javaPos;
                        javaxPos = block.pattern.equals("javax.*") && javaxPos > blockStart ? blockStart : javaxPos;

                        if (blocks.contains(block) && previousPkgCount > referenceCount.get(block)) {
                            blocks.remove(block);
                        }
                        blocks.add(block);
                        referenceCount.put(block, previousPkgCount + 1);
                        previousPkgCount = 1;
                    }

                    blockStart = i;
                }

                if (anImport.getQualid().getSimpleName().equals("*")) {
                    if (anImport.isStatic()) {
                        int count = 0;
                        for (CType type : cu.getTypesInUse()) {
                            if (type instanceof CType.Variable) {
                                CType.FullyQualified fq = TypeUtils.asFullyQualified(((CType.Variable) type).getType());
                                if (fq != null && anImport.getTypeName().equals(fq.getFullyQualifiedName())) {
                                    count++;
                                }
                            }
                        }

                        importLayoutStatistics.minimumFoldedStaticImports = Math.min(
                                importLayoutStatistics.minimumFoldedStaticImports,
                                count
                        );
                    } else {
                        Set<String> fqns = new HashSet<>();
                        for (CType type : cu.getTypesInUse()) {
                            if (type instanceof CType.FullyQualified) {
                                CType.FullyQualified fq = (CType.FullyQualified) type;
                                if (anImport.getPackageName().equals(fq.getPackageName())) {
                                    // don't count directly, as CType.Parameterized can
                                    // CONTAIN a FullyQualified that matches a raw FullyQualified
                                    fqns.add(fq.getFullyQualifiedName());
                                }
                            }
                        }

                        importLayoutStatistics.minimumFoldedImports = Math.min(
                                importLayoutStatistics.minimumFoldedImports,
                                fqns.size()
                        );
                    }
                }

                staticBlock = anImport.isStatic();
                i++;
                previousPkg = importLayoutStatistics.pkgToBlockPattern.get(anImport.getPackageName() + ".");
            }

            if (i - blockStart > 0) {
                ImportLayoutStatistics.Block block = new ImportLayoutStatistics.Block(
                        staticBlock ?
                                ImportLayoutStatistics.BlockType.ImportStatic :
                                ImportLayoutStatistics.BlockType.Import,
                        previousPkg,
                        false);

                if (blocks.contains(block) && previousPkgCount > referenceCount.get(block)) {
                    blocks.remove(block);
                }

                javaPos = block.pattern.equals("java.*") ? blockStart : javaPos;
                javaxPos = block.pattern.equals("javax.*") ? blockStart : javaxPos;
                blocks.add(block);
            }

            if (javaPos != Integer.MAX_VALUE && javaxPos != Integer.MAX_VALUE) {
                importLayoutStatistics.javaBeforeJavaxCount += javaPos < javaxPos ? 1 : 0;
                importLayoutStatistics.javaxBeforeJavaCount += javaxPos < javaPos ? 1 : 0;
            }

            importLayoutStatistics.blocksPerSourceFile.add(new ArrayList<>(blocks));

            return cu;
        }
    }
}
