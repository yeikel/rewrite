package org.openrewrite.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.openrewrite.internal.lang.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public abstract class ConfigurableRewriteMojo extends AbstractMojo {

  @SuppressWarnings("NotNullFieldNotInitialized")
  @Parameter(property = "rewrite.configLocation", alias = "configLocation", defaultValue = "${maven.multiModuleProjectDirectory}/rewrite.yml")
  String configLocation;

  @Parameter(property = "activeRecipes")
  private Set<String> activeRecipes = Collections.emptySet();

  @Nullable
  @Parameter(property = "rewrite.activeRecipes")
  private String rewriteActiveRecipes;

  @Parameter(property = "activeStyles")
  private Set<String> activeStyles = Collections.emptySet();

  @Nullable
  @Parameter(property = "rewrite.activeStyles")
  private String rewriteActiveStyles;

  @Nullable
  @Parameter(property = "rewrite.metricsUri", alias = "metricsUri")
  protected String metricsUri;

  @Nullable
  @Parameter(property = "rewrite.metricsUsername", alias = "metricsUsername")
  protected String metricsUsername;

  @Nullable
  @Parameter(property = "rewrite.metricsPassword", alias = "metricsPassword")
  protected String metricsPassword;

  @Parameter(property = "rewrite.pomCacheEnabled", alias = "pomCacheEnabled", defaultValue = "true")
  protected boolean pomCacheEnabled;

  @Nullable
  @Parameter(property = "rewrite.pomCacheDirectory", alias = "pomCacheDirectory")
  protected String pomCacheDirectory;

  /**
   * When enabled, skip parsing Maven `pom.xml`s, and any transitive poms, as source files.
   * This can be an efficiency improvement in certain situations.
   */
  @Parameter(property = "skipMavenParsing", defaultValue = "false")
  protected boolean skipMavenParsing;

  @Nullable
  @Parameter(property = "rewrite.checkstyleConfigFile", alias = "checkstyleConfigFile")
  protected String checkstyleConfigFile;

  @Parameter(property="exclusions")
  private Set<String> exclusions = Collections.emptySet();

  @Nullable
  @Parameter(property = "rewrite.exclusions")
  private String rewriteExclusions;

  protected Set<String> getExclusions() {
    if(rewriteExclusions == null) {
      return exclusions;
    } else {
      Set<String> allExclusions = toSet(rewriteExclusions);
      allExclusions.addAll(exclusions);
      return allExclusions;
    }
  }

  @Nullable
  @Parameter(property = "sizeThresholdMb", defaultValue = "10")
  protected int sizeThresholdMb;

  /**
   * Whether to throw an exception if an activeRecipe fails configuration validation.
   * This may happen if the activeRecipe is improperly configured, or any downstream recipes are improperly configured.
   * <p>
   * For the time, this default is "false" to prevent one improperly recipe from failing the build.
   * In the future, this default may be changed to "true" to be more restrictive.
   */
  @Parameter(property = "rewrite.failOnInvalidActiveRecipes", alias = "failOnInvalidActiveRecipes", defaultValue = "false")
  protected boolean failOnInvalidActiveRecipes;

  @Nullable
  @Parameter(property = "rewrite.recipeArtifactCoordinates")
  private String recipeArtifactCoordinates;

  @Nullable
  private volatile Set<String> computedRecipes;

  @Nullable
  private volatile Set<String> computedStyles;

  @Nullable
  private volatile Set<String> computedRecipeArtifactCoordinates;

  protected Set<String> getActiveRecipes() {
    if (computedRecipes == null) {
      synchronized (this) {
        if (computedRecipes == null) {
          Set<String> res = toSet(rewriteActiveRecipes);
          res.addAll(activeRecipes);
          computedRecipes = Collections.unmodifiableSet(res);
        }
      }
    }

    return computedRecipes;
  }

  protected Set<String> getActiveStyles() {
    if (computedStyles == null) {
      synchronized (this) {
        if (computedStyles == null) {
          Set<String> res = toSet(rewriteActiveStyles);
          res.addAll(activeStyles);
          computedStyles = Collections.unmodifiableSet(res);
        }
      }
    }

    return computedStyles;
  }

  protected Set<String> getRecipeArtifactCoordinates() {
    if (computedRecipeArtifactCoordinates == null) {
      synchronized (this) {
        if (computedRecipeArtifactCoordinates == null) {
          computedRecipeArtifactCoordinates = Collections.unmodifiableSet(toSet(recipeArtifactCoordinates));
        }
      }
    }

    return computedRecipeArtifactCoordinates;
  }

  private Set<String> toSet(@Nullable String propertyValue) {
    return Optional.ofNullable(propertyValue)
        .filter(s -> !s.isEmpty())
        .map(s -> new HashSet<>(Arrays.asList(s.split(","))))
        .orElseGet(HashSet::new);
  }
}