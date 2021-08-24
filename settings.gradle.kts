include(
    "rewrite-core",
    "rewrite-hcl",
    "rewrite-json",
    "rewrite-maven",
    "rewrite-properties",
    "rewrite-xml",
    "rewrite-yaml",
    "rewrite-test",
    "rewrite-benchmarks"
)

listOf("family-c", "java", "java-8", "java-11", "groovy").forEach { lang ->
    include("rewrite-$lang")
    project(":rewrite-$lang").projectDir = File(rootProject.projectDir, "c-family-languages/rewrite-$lang")
}
