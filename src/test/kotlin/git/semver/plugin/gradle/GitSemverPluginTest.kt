/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package git.semver.plugin.gradle

import git.semver.plugin.semver.SemverSettings
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * A simple unit test for the 'git.semver.plugin.gradle.greeting' plugin.
 */
class GitSemverPluginTest {
    @Test fun `plugin registers task`() {
        // Create a test project and apply the plugin
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("com.github.jmongard.git-semver-plugin")

        // Verify the result
        assertThat(project.tasks.findByName("printVersion")).isNotNull();
        assertThat(project.tasks.findByName("printSemVersion")).isNotNull();
        assertThat(project.tasks.findByName("printInfoVersion")).isNotNull();
        assertThat(project.tasks.findByName("printChangeLog")).isNotNull();
        assertThat(project.tasks.findByName("releaseVersion")).isNotNull();
    }
}
