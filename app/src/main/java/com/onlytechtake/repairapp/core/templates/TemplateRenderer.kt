package com.onlytechtake.repairapp.core.templates

class TemplateRenderer {
    fun render(
        template: String,
        variables: Map<String, String>,
    ): String =
        variablePattern.replace(template) { match ->
            val variableName = match.groupValues[1].trim()
            require(variableName in variables) { "Variabile template non supportata: $variableName" }
            variables.getValue(variableName)
        }

    fun extractVariables(template: String): Set<String> =
        variablePattern
            .findAll(template)
            .map { it.groupValues[1].trim() }
            .filter { it.isNotBlank() }
            .toSet()

    fun requireSupportedVariables(
        template: String,
        supportedVariables: Set<String>,
    ) {
        val unsupportedVariables = extractVariables(template) - supportedVariables
        require(unsupportedVariables.isEmpty()) {
            "Variabili template non supportate: ${unsupportedVariables.sorted().joinToString(", ")}"
        }
    }

    private companion object {
        val variablePattern = Regex("""\{\{\s*([A-Za-z0-9_]+)\s*\}\}""")
    }
}
