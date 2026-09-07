package com.nxyn.aiclient.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

object SyntaxHighlighter {
    private val keywordColors = mapOf(
        "kotlin" to setOf(
            "fun", "val", "var", "class", "object", "interface", "return", "if", "else",
            "when", "for", "while", "import", "package", "private", "public", "internal",
            "suspend", "data", "enum", "sealed", "true", "false", "null", "in", "is", "as"
        ),
        "java" to setOf(
            "public", "private", "protected", "class", "interface", "return", "if", "else",
            "for", "while", "import", "package", "static", "void", "new", "true", "false", "null"
        ),
        "python" to setOf(
            "def", "class", "return", "if", "elif", "else", "for", "while", "import", "from",
            "True", "False", "None", "with", "as", "lambda", "pass", "break", "continue"
        ),
        "javascript" to setOf(
            "function", "const", "let", "var", "return", "if", "else", "for", "while", "import",
            "export", "class", "new", "true", "false", "null", "undefined", "async", "await"
        ),
        "typescript" to setOf(
            "function", "const", "let", "var", "return", "if", "else", "for", "while", "import",
            "export", "class", "interface", "type", "new", "true", "false", "null", "async", "await"
        ),
        "json" to emptySet(),
        "text" to emptySet()
    )

    fun highlight(code: String, language: String): AnnotatedString {
        val keywords = keywordColors[language.lowercase()] ?: keywordColors["text"].orEmpty()
        val stringRegex = Regex("""(".*?")|('.*?')""")
        val commentRegex = Regex("""(//.*$|#.*$)""", RegexOption.MULTILINE)
        val numberRegex = Regex("""\b\d+(?:\.\d+)?\b""")

        return AnnotatedString.Builder().apply {
            val tokens = code.split(Regex("""(\s+|\b)""")).filter { it.isNotEmpty() }
            tokens.forEach { token ->
                when {
                    token in keywords -> withStyle(SpanStyle(color = Color(0xFF7C3AED), fontWeight = FontWeight.SemiBold)) {
                        append(token)
                    }
                    stringRegex.matches(token) -> withStyle(SpanStyle(color = Color(0xFF34D399))) {
                        append(token)
                    }
                    commentRegex.matches(token) -> withStyle(SpanStyle(color = Color(0xFF94A3B8))) {
                        append(token)
                    }
                    numberRegex.matches(token) -> withStyle(SpanStyle(color = Color(0xFFF59E0B))) {
                        append(token)
                    }
                    else -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) {
                        append(token)
                    }
                }
            }
        }.toAnnotatedString()
    }
}
