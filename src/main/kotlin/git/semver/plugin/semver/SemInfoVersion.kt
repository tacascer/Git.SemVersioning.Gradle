package git.semver.plugin.semver

import java.io.Serializable

data class SemInfoVersion(
    val sha: String = "",
    override val major: Int = 0,
    override val minor: Int = 0,
    override val patch: Int = 0,
    override val preRelease: PreRelease = PreRelease.noPreRelease,
    val commitCount: Int = 0
) : Serializable, Version {
    override val isPreRelease
        get() = preRelease.isPreRelease

    fun toSemVersion() : SemVersion {
        return SemVersion(major, minor, patch, preRelease)
    }

    fun toVersionString(v2: Boolean = true): String {
        return toInfoVersionString("", 0, v2)
    }

    fun toInfoVersionString(
        commitCountStringFormat: String = "%03d",
        shaLength: Int = 0,
        v2: Boolean = true,
        appendPreReleaseLast: Boolean = false
    ): String {
        val builder = StringBuilder().append(major).append('.').append(minor).append('.').append(patch)
        if (v2) {
            if (isPreRelease && !appendPreReleaseLast) {
                builder.append('-').append(preRelease)
            }
            var metaSeparator = '+'
            if (this.commitCount > 0 && commitCountStringFormat.isNotEmpty()) {
                builder.append(metaSeparator).append(commitCountStringFormat.format(this.commitCount))
                metaSeparator = '.'
            }
            val shaTake = sha.take(shaLength)
            if (shaTake.isNotEmpty()) {
                builder.append(metaSeparator).append("sha.").append(shaTake)
            }
            if (isPreRelease && appendPreReleaseLast) {
                builder.append('-').append(preRelease)
            }
        } else if (isPreRelease) {
            builder.append("-")
                .append(preRelease.prefix.replace("[^0-9A-Za-z-]".toRegex(), ""))
                .append(preRelease.number ?: "")
        }
        return builder.toString()
    }

    override fun toString(): String {
        return toInfoVersionString(shaLength = 7)
    }
}
