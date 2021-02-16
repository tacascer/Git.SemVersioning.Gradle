/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package git.semver.plugin.gradle

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jgit.api.Git
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File
import kotlin.test.Test

/**
 * A simple functional test for the plugin.
 */
class GitSemverPluginFunctionalTest {
    @Test
    fun `can run task`() {
        // Setup the test build

        val projectDir = File("build/functionalTest")
        projectDir.mkdirs()
        projectDir.resolve(".gitignore").writeText(".gradle")
        projectDir.resolve("settings.gradle").writeText("include ':sub1'")
        projectDir.resolve("build.gradle").writeText(
            """
            plugins {
              id('com.github.jmongard.git-semver-plugin')
            }
            
            semver {
              groupVersionIncrements = false
            }
            
            allprojects {
              version = semver.version
            }
            
            task testTask(dependsOn:printVersion) {
              doLast {
                 println "ProjVer: " + project.version 
              }
            }
        """.trimIndent()
        )

        val subProjectDir = File("build/functionalTest/sub1")
        subProjectDir.mkdirs()
        subProjectDir.resolve("build.gradle").writeText(
            """
            println("Sub1: " + project.version)            
        """.trimIndent()
        )

        // Add the build files to a new git repository
        Git.init().setDirectory(projectDir).call().use {
            it.add().addFilepattern(".").call()
            it.commit().setMessage("test files").call()
        }

        // Run the build - release
        val releaseResult = run(projectDir, "release", "-PdefaultPreRelease=NEXT")
        assertThat(releaseResult.output)
            .doesNotContain("FAILED")
            .containsPattern("Sub1: \\d+\\.\\d+\\.\\d+-NEXT")

        // Run the build - release with options
        val releaseResult2 = run(
            projectDir, "release",
            "--no-tag", "--no-commit",
            "--preRelease=NEXT.1",
            "--message=test"
        )
        assertThat(releaseResult2.output).doesNotContain("FAILED")

        // Run the build - testTask
        val result = run(projectDir, "testTask")

        // Verify the result
        assertThat(result.output).containsPattern("Version: \\d+\\.\\d+\\.\\d+")
        assertThat(result.output).containsPattern("ProjVer: \\d+\\.\\d+\\.\\d+")
    }

    private fun run(projectDir: File, vararg args: String): BuildResult {
        return GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments(args.toList())
            .withProjectDir(projectDir)
            .build()
    }
}
