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
 * A simple functional test for the 'git.semver.plugin.gradle.greeting' plugin.
 */
class GitSemverPluginFunctionalTest {
    @Test fun `can run task`() {
        // Setup the test build

        val projectDir = File("build/functionalTest")
        projectDir.mkdirs()
        projectDir.resolve(".gitignore").writeText(".*")
        projectDir.resolve("settings.gradle").writeText("include ':sub1'")
        projectDir.resolve("build.gradle").writeText("""
            plugins {
                id('com.github.jmongard.git-semver-plugin')
            }
            
            semver {
                defaultPreRelease='NEXT'
            }
            
            allprojects {
              version = semver.version
            }
            
            task testTask(dependsOn:printVersion) {
              doLast {
                 println "ProjVer: " + project.version 
              }
            }
        """)

        val subProjectDir = File("build/functionalTest/sub1")
        subProjectDir.mkdirs()
        subProjectDir.resolve("build.gradle").writeText("""
            println("Sub1: " + project.version)            
        """.trimIndent())


        Git.init().setDirectory(projectDir).call().use {
            it.add().addFilepattern(".").call()
            it.commit().setMessage("test: files").call()
        }

        // Run the build
        val releaseResult = run(projectDir, "release")

        assertThat(releaseResult.output).doesNotContain("Exception")

        // Run the build
        val result = run(projectDir, "testTask")

        // Verify the result
        assertThat(result.output).containsPattern("Version: \\d+\\.\\d+\\.\\d+")
        assertThat(result.output).containsPattern("ProjVer: \\d+\\.\\d+\\.\\d+")
        //assertThat(result.output).containsPattern("Sub1: \\d+\\.\\d+\\.\\d+")
    }

    private fun run(projectDir: File, vararg args: String): BuildResult {
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("--stacktrace")
        args.iterator().forEach {  runner.withArguments(it) }
        runner.withProjectDir(projectDir)
        return runner.build()
    }
}
