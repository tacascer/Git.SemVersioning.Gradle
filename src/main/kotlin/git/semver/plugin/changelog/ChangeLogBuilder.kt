package git.semver.plugin.changelog

import git.semver.plugin.changelog.ChangeLogTexts.Companion.BREAKING_CHANGE
import java.util.*

open class ChangeLogBuilder(
    val groupKey: String? = null,
    private val commitInfos: List<ChangeLogFormatter.CommitInfo>,
    private val context: ChangeLogFormatter.Context,
    val constants: ChangeLogTexts
) : DocumentBuilder() {

    fun formatChanges(block: ChangeLogTextFormatter.() -> Unit) {
        for (commitInfo in remainingCommitInfos()) {
            val formatter = ChangeLogTextFormatter(commitInfo)
            formatter.block()
            append(formatter.build())
            context.flagCommit(commitInfo)
        }
    }

    fun skip() {
        skip(remainingCommitInfos())
    }

    private fun skip(commitInfoList: List<ChangeLogFormatter.CommitInfo>) {
        for (commitInfo in commitInfoList) {
            context.flagCommit(commitInfo)
        }
    }

    //<editor-fold desc="filter">
    fun withBreakingChanges(block: ChangeLogBuilder.() -> Unit) {
        with({ it.isBreaking }, BREAKING_CHANGE, block)
    }

    fun withScope(vararg scopes: String, block: ChangeLogBuilder.() -> Unit) {
        for (scope in scopes) {
            with({ it.scope == scope }, scope, block)
        }
    }

    fun withType(vararg types: String, block: ChangeLogBuilder.() -> Unit) {
        for (type in types) {
            with({ it.type == type }, type, block)
        }
    }

    fun otherwise(block: ChangeLogBuilder.() -> Unit) {
        with({ true }, block)
    }

    fun with(filter: (ChangeLogFormatter.CommitInfo) -> Boolean, block: ChangeLogBuilder.() -> Unit) =
        with(filter, null, block)

    fun with(
        filter: (ChangeLogFormatter.CommitInfo) -> Boolean,
        key: String?,
        block: ChangeLogBuilder.() -> Unit
    ) {
        val filteredCommits = remainingCommitInfos().filter(filter)
        if (filteredCommits.isNotEmpty()) {
            val builder = ChangeLogBuilder(key, filteredCommits, context, constants)
            builder.block()
            append(builder.build())
        }
    }
    //</editor-fold>

    //<editor-fold desc="grouping">
    fun groupByScope(block: ChangeLogBuilder.() -> Unit) {
        groupBySorted({ it.scope }, block)
    }

    fun groupByType(block: ChangeLogBuilder.() -> Unit) {
        groupBySorted({ it.type }, block)
    }

    fun groupBy(keySelector: (ChangeLogFormatter.CommitInfo) -> String?, block: ChangeLogBuilder.() -> Unit) {
        processGroups(remainingCommitInfos()
            .mapNotNull { keyMapper(keySelector, it) }
            .groupBy({ it.first }, { it.second }), block)
    }

    fun groupBySorted(
        keySelector: (ChangeLogFormatter.CommitInfo) -> String?,
        block: ChangeLogBuilder.() -> Unit
    ) {
        processGroups(remainingCommitInfos()
            .mapNotNull { keyMapper(keySelector, it) }
            .groupByTo(TreeMap(), { it.first }, { it.second }), block)
    }

    private fun keyMapper(
        keySelector: (ChangeLogFormatter.CommitInfo) -> String?,
        it: ChangeLogFormatter.CommitInfo
    ): Pair<String, ChangeLogFormatter.CommitInfo>? {
        return (keySelector(it) ?: return null) to it
    }

    private fun processGroups(
        groupedByScope: Map<String, List<ChangeLogFormatter.CommitInfo>>,
        block: ChangeLogBuilder.() -> Unit
    ) {
        for ((key, scopeCommits) in groupedByScope) {
            if (key.isNotEmpty()) {
                val builder = ChangeLogBuilder(key, scopeCommits, context, constants)
                block(builder)
                append(builder.build())
            }
            else {
                skip(scopeCommits)
            }
        }
    }
    //</editor-fold>

    fun remainingCommitInfos() = commitInfos.filter { !context.isCommitFlagged(it) }
}

class ChangeLogTextFormatter(
    val commitInfo: ChangeLogFormatter.CommitInfo
) : DocumentBuilder() {
    companion object {
        fun sanitizeHtml(s: String): String {
            val tokenizer = StringTokenizer(s, "`<", true)
            var inBacktick = false
            var inDoubleBackTick = false
            val result = StringBuilder(s.length)
            var lastToken = ""

            while (tokenizer.hasMoreTokens()) {
                val token = tokenizer.nextToken()
                if (token == "`") {
                    if (lastToken == "`" && inBacktick && !inDoubleBackTick) {
                        inDoubleBackTick = true
                    }
                    else if (!inDoubleBackTick || lastToken == "`"){
                        inBacktick = !inBacktick
                        inDoubleBackTick = false
                    }
                }

                if (!inBacktick && token == "<") {
                    result.append("\\<")
                } else {
                    result.append(token)
                }
                lastToken = token;
            }

            return result.toString()
        }
    }

    fun header() = (sanitizeHtml(commitInfo.message ?: commitInfo.text)).lineSequence().first()

    fun body() = sanitizeHtml(commitInfo.text).lineSequence()
        .drop(1)
        .dropWhile { it.isEmpty() }
        .takeWhile { it.isNotEmpty() }

    fun fullHeader() = sanitizeHtml(commitInfo.text).lineSequence().first()

    fun scope(format: String = "%s: ") = commitInfo.scope?.let { format.format(it) }.orEmpty()

    fun type(format: String = "%s: ") = commitInfo.type?.let { format.format(it) }.orEmpty()
    fun hash(format: String = "%s", len: Int = 40) =
        commitInfo.commits.joinToString(" ", "", " ") { format.format(it.sha.take(len)) }

    fun authorName(format: String = "%s") =
        commitInfo.commits.map{ format.format(it.authorName) }.distinct().joinToString(" ", "", "")
    fun authorNameAndEmail(format: String = "%s <%s>") =
        commitInfo.commits.map { format.format(it.authorName, it.authorEmail)}.distinct().joinToString(" ", "", "")
}

open class DocumentBuilder() {
    private val out = StringBuilder()

    fun build(): String {
        return out.toString()
    }

    fun append(t: String?): DocumentBuilder {
        out.append(t)
        return this
    }

    fun appendLine(t: String? = ""): DocumentBuilder {
        out.appendLine(t)
        return this
    }
}