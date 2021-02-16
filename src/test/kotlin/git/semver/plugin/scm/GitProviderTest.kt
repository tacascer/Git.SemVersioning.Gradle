package git.semver.plugin.scm

import git.semver.plugin.semver.SemVersion
import git.semver.plugin.semver.SemverSettings
import org.eclipse.jgit.api.Git
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GitProviderTest {
    @Test
    fun testGetSemVersion() {
        val actual = gitProvider().getSemVersion(File("."))

        assertNotNull(actual)
        println(actual)
    }

    @Test
    fun testCommits() {
        val gitDir = File("build/integrationTest2")
        gitDir.mkdirs()

        println(
            "| %-30s | %-20s | %-38s|".format(
                "Commit Text",
                "Calculated version",
                " Using release task: gradle ..."
            )
        )
        println("| ------------------------------ | -------------------- | ------------------------------------- |")

        val gitProvider = GitProvider(SemverSettings().apply { groupVersionIncrements = true })
        Git.init().setDirectory(gitDir).call().use {
            it.commit().setMessage("Initial commit").call()
            val last = it.log().all().call().last()
            it.reset().setRef(last.name).call()
            it.gc().call()
            printC("Initial commit", gitProvider, it)
            commit(it, "some changes", gitProvider)
            release(gitProvider, it)
            commit(it, "some changes", gitProvider)
            release(gitProvider, it)
            commit(it, "fix: a fix", gitProvider)
            commit(it, "fix: another fix", gitProvider)
            release(gitProvider, it)
            commit(it, "feat: a feature", gitProvider)
            commit(it, "feat: another feature", gitProvider)
            commit(it, "feat!: breaking feature", gitProvider)
            commit(it, "some changes", gitProvider)
            commit(it, "feat: changes", gitProvider)
            release(gitProvider, it)
            commit(it, "some changes", gitProvider)
            release(gitProvider, it, "alpha.1")
            commit(it, "some changes", gitProvider)
            release(gitProvider, it)
            commit(it, "fix: a fix", gitProvider)
            commit(it, "fix: another fix", gitProvider)
            commit(it, "feat: a feature", gitProvider)
            release(gitProvider, it)
            commit(it, "feat: another feature", gitProvider)
            commit(it, "feat!: breaking feature", gitProvider)

            val actual = release(gitProvider, it, "")

            assertEquals("2.0.0", actual.toVersionString())
        }
    }

    private fun commit(it: Git, msg: String, gitProvider: GitProvider) {
        it.commit().setMessage(msg).call()
        printC(msg, gitProvider, it)
    }

    private fun printC(msg: String, gitProvider: GitProvider, it: Git) {
        println("| %-30s | %-20s | %-38s|".format(msg, gitProvider.semVersion(it).toInfoVersionString(), ""))
    }

    private fun release(gitProvider: GitProvider, it: Git, preRelease: String? = null): SemVersion {
        gitProvider.createRelease(it, false, commit = true, preRelease = preRelease, noDirtyCheck = false)
        val semVersion = gitProvider.semVersion(it)
        println(
            "| %-30s | %-20s | %-38s|".format(
                it.log().setMaxCount(1).call().first().fullMessage,

                semVersion.toInfoVersionString(),
                "releaseVersion " + if (preRelease == null) "" else "--preRelease=\"$preRelease\""
            )
        )
        return semVersion
    }

    @Test
    fun testCreateReleaseCommit() {
        val gitDir = File("build/integrationTest")
        gitDir.mkdirs()

        val gitProvider = gitProvider()

        Git.init().setDirectory(gitDir).call().use {
            commit(it, "some changes", gitProvider)
        }
        gitProvider.createRelease(gitDir, true, commit = false, preRelease = "alpha.1", noDirtyCheck = false)

        Git.open(gitDir).use {
            commit(it, "feat: some feature", gitProvider)
            commit(it, "docs: some documentation", gitProvider)
        }
        gitProvider.createRelease(gitDir, false, true, "beta.1", "some message", false)

        Git.open(gitDir).use {
            commit(it, "fix: some fixes", gitProvider)
            commit(it, "docs: some documentation", gitProvider)
        }
        gitProvider.createRelease(gitDir, true, commit = true, preRelease = null, noDirtyCheck = false)

        Git.open(gitDir).use {
            commit(it, "some changes", gitProvider)
            commit(it, "docs: some documentation", gitProvider)
        }
        gitProvider.createRelease(gitDir, false, commit = true, preRelease = "", noDirtyCheck = false)

        Git.open(gitDir).use {
            assertTrue(gitProvider.getHeadCommit(it.repository).text.startsWith("release: v0."))
        }
    }

    private fun gitProvider() = GitProvider(SemverSettings())
}