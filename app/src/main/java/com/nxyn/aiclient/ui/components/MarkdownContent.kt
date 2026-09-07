package com.nxyn.aiclient.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.node.AbstractVisitor
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.Link
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text
import org.commonmark.parser.Parser

@Composable
fun MarkdownContent(
    markdown: String,
    modifier: Modifier = Modifier,
    onCopyMessage: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }
    val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Code -> CodeBlock(code = block.code, language = block.language)
                is MarkdownBlock.Paragraph -> SelectionContainer {
                    Text(
                        text = block.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        if (onCopyMessage != null) {
            TextButton(onClick = {
                copyToClipboard(context, markdown)
                copied = true
                onCopyMessage()
            }) {
                Text(if (copied) "✓ Copied" else "Copy message")
            }
        }
    }
}

private sealed interface MarkdownBlock {
    data class Paragraph(val text: androidx.compose.ui.text.AnnotatedString) : MarkdownBlock
    data class Code(val language: String, val code: String) : MarkdownBlock
}

private fun parseMarkdownBlocks(markdown: String): List<MarkdownBlock> {
    val parser = Parser.builder()
        .extensions(listOf(TablesExtension.create(), StrikethroughExtension.create()))
        .build()
    val document = parser.parse(markdown)
    val blocks = mutableListOf<MarkdownBlock>()
    var node: Node? = document.firstChild
    while (node != null) {
        when (node) {
            is FencedCodeBlock -> blocks.add(MarkdownBlock.Code(node.info.orEmpty(), node.literal.orEmpty()))
            is Paragraph -> blocks.add(MarkdownBlock.Paragraph(renderInline(node)))
            is Heading -> blocks.add(MarkdownBlock.Paragraph(renderInline(node)))
            is BulletList, is OrderedList, is BlockQuote -> blocks.add(MarkdownBlock.Paragraph(renderBlock(node)))
            else -> node.accept(object : AbstractVisitor() {
                override fun visit(text: Text) {
                    blocks.add(MarkdownBlock.Paragraph(androidx.compose.ui.text.AnnotatedString(text.literal)))
                }
            })
        }
        node = node.next
    }
    return blocks.ifEmpty { listOf(MarkdownBlock.Paragraph(androidx.compose.ui.text.AnnotatedString(markdown))) }
}

private fun renderBlock(node: Node): androidx.compose.ui.text.AnnotatedString {
    val builder = androidx.compose.ui.text.AnnotatedString.Builder()
    node.accept(object : AbstractVisitor() {
        override fun visit(text: Text) {
            builder.append(text.literal)
        }

        override fun visit(softLineBreak: SoftLineBreak) {
            builder.append("\n")
        }

        override fun visit(hardLineBreak: HardLineBreak) {
            builder.append("\n")
        }
    })
    return builder.toAnnotatedString()
}

private fun renderInline(node: Node): androidx.compose.ui.text.AnnotatedString {
    val builder = androidx.compose.ui.text.AnnotatedString.Builder()
    node.accept(object : AbstractVisitor() {
        override fun visit(text: Text) {
            builder.append(text.literal)
        }

        override fun visit(strongEmphasis: StrongEmphasis) {
            val start = builder.length
            visitChildren(strongEmphasis)
            builder.addStyle(
                androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold),
                start,
                builder.length
            )
        }

        override fun visit(emphasis: Emphasis) {
            val start = builder.length
            visitChildren(emphasis)
            builder.addStyle(
                androidx.compose.ui.text.SpanStyle(fontStyle = FontStyle.Italic),
                start,
                builder.length
            )
        }

        override fun visit(code: Code) {
            builder.append(code.literal)
        }

        override fun visit(link: Link) {
            builder.append(link.destination)
        }

        override fun visit(softLineBreak: SoftLineBreak) {
            builder.append("\n")
        }

        override fun visit(hardLineBreak: HardLineBreak) {
            builder.append("\n")
        }
    })
    return builder.toAnnotatedString()
}
