package git.semver.plugin.changelog

class ChangeLogFormat {
    companion object {
        /**
         * Creates a change log breaking changes at the top and grouping by typ or scope.
         */
        val defaultChangeLog = ChangeLogFormatter {
            appendLine(constants.header).appendLine()

            withType("release") {
                skip()
            }

            // Breaking changes
            withBreakingChanges {
                appendLine(constants.breakingChange)
                formatChanges {
                    append("- ").append(hash()).appendLine(fullHeader())
                }
                appendLine()
            }

            // Fixes and then Features from typesOrder
            withType(types = constants.typesOrder.toTypedArray()) {
                appendLine(constants.headerTexts[groupKey])
                with({ constants.headerTexts.containsKey(it.scope) }) {
                    formatChanges {
                        append("- ").append(hash()).append(type()).appendLine(header())
                    }
                }
                formatChanges {
                    append("- ").append(hash()).append(scope()).appendLine(header())
                }
                appendLine()
            }

            // Other defined types and scopes in headerTexts
            groupBySorted({ constants.headerTexts[it.scope] ?: constants.headerTexts[it.type] }) {
                appendLine(groupKey)
                with({ constants.headerTexts.containsKey(it.scope) }) {
                    formatChanges {
                        append("- ").append(hash()).append(type()).appendLine(header())
                    }
                }
                formatChanges {
                    append("- ").append(hash()).append(scope()).appendLine(header())
                }
                appendLine()
            }

            // Other changes
            otherwise {
                appendLine(constants.otherChange)
                formatChanges {
                    append("- ").append(hash()).appendLine(fullHeader())
                }
                appendLine()
            }
            
            appendLine(constants.footer)
        }

        /**
         * Creates a simple change log only containing breaking changes, features and fixes
         */
        val simpleChangeLog = ChangeLogFormatter {
            appendLine(constants.header).appendLine()

            withBreakingChanges {
                appendLine(constants.breakingChange)
                formatChanges {
                    append("- ").appendLine(fullHeader())
                }
                appendLine()
            }
            withType(types = constants.typesOrder.toTypedArray()) {
                appendLine(constants.headerTexts[groupKey])
                formatChanges {
                    append("- ").append(scope()).appendLine(header())
                }
                appendLine()
            }

            appendLine(constants.footer)
        }

        /**
         * Creates a change log grouping on type and then on scope
         */
        val scopeChangeLog = ChangeLogFormatter {
            appendLine(constants.header).appendLine()

            withType("release") {
                skip()
            }

            withBreakingChanges(formatGroupByScopeDisplayType(constants.breakingChange))
            groupBySorted({ constants.headerTexts[it.type] }, formatGroupByScope())
            otherwise (formatGroupByScopeDisplayType(constants.otherChange))

            appendLine(constants.footer)
        }

        private fun formatGroupByScope(): ChangeLogBuilder.() -> Unit = {
            appendLine(groupKey).appendLine()
            groupByScope {
                append("#### ").appendLine(groupKey)
                formatChanges {
                    append("- ").append(hash()).appendLine(header())
                }
                appendLine()
            }
            otherwise {
                appendLine("#### Missing scope")
                formatChanges {
                    append("- ").append(hash()).appendLine(header())
                }
                appendLine()
            }
            appendLine()
        }

        private fun formatGroupByScopeDisplayType(header: String?): ChangeLogBuilder.() -> Unit = {
            appendLine(header).appendLine()
            groupByScope {
                append("#### ").appendLine(groupKey)
                formatChanges {
                    append("- ").append(hash()).append(type()).appendLine(header())
                }
                appendLine()
            }
            otherwise {
                appendLine("#### Missing scope")
                formatChanges {
                    append("- ").append(hash()).append(type()).appendLine(header())
                }
                appendLine()
            }
            appendLine()
        }
    }
}