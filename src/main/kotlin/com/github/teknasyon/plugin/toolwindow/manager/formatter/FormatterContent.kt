package com.github.teknasyon.plugin.toolwindow.manager.formatter

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatAlignLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.teknasyon.plugin.common.Constants
import com.github.teknasyon.plugin.components.TPActionCard
import com.github.teknasyon.plugin.components.TPActionCardType
import com.github.teknasyon.plugin.components.TPTabRow
import com.github.teknasyon.plugin.components.TPText
import com.github.teknasyon.plugin.service.SettingsService
import com.github.teknasyon.plugin.theme.TPTheme
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.xml.sax.InputSource
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.StringReader
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

@Composable
fun FormatterContent() {
    val settings = SettingsService.getInstance()

    var selectedFormat by remember { mutableStateOf(settings.getFormatterSelectedFormat()) }
    var inputText by remember {
        mutableStateOf(
            if (settings.getFormatterInputText().isNotEmpty()) {
                settings.getFormatterInputText()
            } else {
                if (selectedFormat == "JSON") getSampleJson() else getSampleXml()
            }
        )
    }
    var outputText by remember { mutableStateOf("") }
    var isValidInput by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf(settings.getFormatterErrorMessage()) }

    LaunchedEffect(selectedFormat, inputText, errorMessage) {
        settings.saveFormatterState(selectedFormat, inputText, errorMessage)
    }

    LaunchedEffect(inputText) {
        if (inputText.isNotEmpty()) {
            val result = if (selectedFormat == "JSON") formatJson(inputText) else formatXml(inputText)
            outputText = if (result.second) result.first else Constants.EMPTY
            isValidInput = result.second
            errorMessage = result.third
        }
    }

    LaunchedEffect(selectedFormat) {
        if (inputText.isNotEmpty()) {
            val result = if (selectedFormat == "JSON") formatJson(inputText) else formatXml(inputText)
            outputText = if (result.second) result.first else Constants.EMPTY
            isValidInput = result.second
            errorMessage = result.third
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TPTheme.colors.black)
            .padding(24.dp)
    ) {
        TPText(
            modifier = Modifier.fillMaxWidth(),
            text = "JSON/XML Formatter",
            style = TextStyle(
                color = TPTheme.colors.blue,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TPTabRow(
                text = "JSON",
                isSelected = selectedFormat == "JSON",
                color = TPTheme.colors.blue,
                onTabSelected = {
                    selectedFormat = "JSON"
                    outputText = ""
                    errorMessage = ""
                }
            )
            TPTabRow(
                text = "XML",
                isSelected = selectedFormat == "XML",
                color = TPTheme.colors.blue,
                onTabSelected = {
                    selectedFormat = "XML"
                    outputText = ""
                    errorMessage = ""
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TPActionCard(
                title = "Format",
                icon = Icons.AutoMirrored.Rounded.FormatAlignLeft,
                actionColor = TPTheme.colors.blue,
                type = TPActionCardType.SMALL,
                onClick = {
                    val result = if (selectedFormat == "JSON") {
                        formatJson(inputText)
                    } else {
                        formatXml(inputText)
                    }
                    outputText = result.first
                    isValidInput = result.second
                    errorMessage = result.third
                }
            )

            TPActionCard(
                title = "Minify",
                icon = Icons.Rounded.Compress,
                actionColor = TPTheme.colors.blue,
                type = TPActionCardType.SMALL,
                onClick = {
                    val result = if (selectedFormat == "JSON") {
                        minifyJson(inputText)
                    } else {
                        minifyXml(inputText)
                    }
                    outputText = result.first
                    isValidInput = result.second
                    errorMessage = result.third
                }
            )

            TPActionCard(
                title = "Clear",
                icon = Icons.Rounded.Clear,
                actionColor = TPTheme.colors.blue,
                type = TPActionCardType.SMALL,
                onClick = {
                    inputText = if (selectedFormat == "JSON") {
                        getSampleJson()
                    } else {
                        getSampleXml()
                    }
                    outputText = ""
                    errorMessage = ""
                    isValidInput = true
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                backgroundColor = if (isValidInput) TPTheme.colors.blue.copy(alpha = 0.1f) else TPTheme.colors.blue.copy(
                    alpha = 0.1f
                ),
                elevation = 0.dp,
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isValidInput) Icons.Rounded.CheckCircle else Icons.Rounded.Error,
                        contentDescription = null,
                        tint = if (isValidInput) TPTheme.colors.blue else TPTheme.colors.blue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TPText(
                        text = errorMessage,
                        color = if (isValidInput) TPTheme.colors.blue else TPTheme.colors.blue,
                        style = TextStyle(fontSize = 12.sp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                TPText(
                    text = "Input",
                    color = TPTheme.colors.white,
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(8.dp))

                CollapsibleJsonTextArea(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = "Enter your $selectedFormat here...",
                    syntaxHighlighting = selectedFormat,
                    readOnly = false
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TPText(
                        text = "Output",
                        color = TPTheme.colors.white,
                        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    )
                    TPActionCard(
                        title = "Copy",
                        icon = Icons.Rounded.ContentCopy,
                        actionColor = TPTheme.colors.blue,
                        type = TPActionCardType.SMALL,
                        onClick = {
                            try {
                                val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                                clipboard.setContents(StringSelection(outputText), null)
                                errorMessage = "Copied to clipboard!"
                                isValidInput = true
                            } catch (_: Exception) {
                                errorMessage = "Failed to copy to clipboard"
                                isValidInput = false
                                val result = if (selectedFormat == "JSON") {
                                    formatJson(inputText)
                                } else {
                                    formatXml(inputText)
                                }
                                outputText = result.first
                                isValidInput = result.second
                                errorMessage = result.third
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                CollapsibleJsonTextArea(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    value = outputText,
                    placeholder = "Formatted output will appear here...",
                    syntaxHighlighting = selectedFormat,
                    readOnly = true
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (inputText.isNotEmpty() || outputText.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (inputText.isNotEmpty()) {
                    TPText(
                        text = "Input: ${inputText.lines().size} lines",
                        color = TPTheme.colors.lightGray,
                        style = TextStyle(fontSize = 12.sp)
                    )
                }

                if (outputText.isNotEmpty()) {
                    TPText(
                        text = "Output: ${outputText.lines().size} lines",
                        color = TPTheme.colors.lightGray,
                        style = TextStyle(fontSize = 12.sp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CollapsibleJsonTextArea(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit = {},
    placeholder: String = "",
    syntaxHighlighting: String = "JSON",
    readOnly: Boolean = false,
) {
    var collapsedRanges by remember { mutableStateOf(setOf<Int>()) }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        backgroundColor = TPTheme.colors.gray,
        elevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            val scrollState = rememberScrollState()

            if (value.isEmpty()) {
                TPText(
                    text = placeholder,
                    color = TPTheme.colors.lightGray.copy(alpha = 0.6f),
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )
                )
            } else {
                if (readOnly && syntaxHighlighting == "JSON") {
                    CollapsibleJsonRenderer(
                        jsonText = value,
                        collapsedRanges = collapsedRanges,
                        onToggleCollapse = { lineIndex ->
                            collapsedRanges = if (collapsedRanges.contains(lineIndex)) {
                                collapsedRanges - lineIndex
                            } else {
                                collapsedRanges + lineIndex
                            }
                        },
                        scrollState = scrollState
                    )
                } else {
                    BasicTextField(
                        value = value,
                        onValueChange = if (readOnly) {
                            {}
                        } else onValueChange,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState),
                        textStyle = TextStyle(
                            color = TPTheme.colors.white,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                        ),
                        cursorBrush = SolidColor(TPTheme.colors.blue),
                        readOnly = readOnly,
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                if (value.isEmpty()) {
                                    TPText(
                                        text = placeholder,
                                        color = TPTheme.colors.lightGray.copy(alpha = 0.6f),
                                        style = TextStyle(
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CollapsibleJsonRenderer(
    jsonText: String,
    collapsedRanges: Set<Int>,
    onToggleCollapse: (Int) -> Unit,
    scrollState: ScrollState,
) {
    val lines = jsonText.lines()
    val objectRanges = remember(jsonText) { findObjectRanges(lines) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        lines.forEachIndexed { index, line ->
            val isObjectStart = objectRanges.any { it.start == index }
            val isInCollapsedRange = objectRanges.any { range ->
                collapsedRanges.contains(range.start) && index in range.start..range.end && index != range.start
            }

            if (!isInCollapsedRange) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isObjectStart) {
                        val isCollapsed = collapsedRanges.contains(index)

                        Icon(
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { onToggleCollapse(index) },
                            imageVector = if (isCollapsed) Icons.AutoMirrored.Rounded.KeyboardArrowRight else Icons.Rounded.KeyboardArrowDown,
                            contentDescription = if (isCollapsed) "Expand" else "Collapse",
                            tint = TPTheme.colors.blue,
                        )
                    } else {
                        Spacer(modifier = Modifier.width(24.dp))
                    }

                    SyntaxHighlightedText(
                        text = line,
                        modifier = Modifier.weight(1f),
                    )
                }

                if (isObjectStart && collapsedRanges.contains(index)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TPText(
                            text = "...",
                            color = TPTheme.colors.lightGray,
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TPText(
                            text = "(${objectRanges.first { it.start == index }.end - index} lines hidden)",
                            color = TPTheme.colors.lightGray.copy(alpha = 0.6f),
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SyntaxHighlightedText(
    text: String,
    modifier: Modifier = Modifier,
) {
    val trimmedText = text.trim()
    val color = when {
        trimmedText.startsWith("\"") && trimmedText.contains(":") -> TPTheme.colors.blue
        trimmedText.startsWith("\"") -> TPTheme.colors.blue
        trimmedText.matches(Regex("\\d+")) || trimmedText.matches(Regex("\\d+\\.\\d+")) -> TPTheme.colors.blue
        trimmedText in listOf("true", "false") -> TPTheme.colors.blue

        trimmedText == "null" -> TPTheme.colors.lightGray

        trimmedText.contains("{") || trimmedText.contains("}") || trimmedText.contains("[") || trimmedText.contains("]") -> TPTheme.colors.white

        else -> TPTheme.colors.white
    }

    TPText(
        text = text,
        color = color,
        style = TextStyle(
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
        ),
        modifier = modifier
    )
}

private data class LineRange(val start: Int, val end: Int)

private fun findObjectRanges(lines: List<String>): List<LineRange> {
    val ranges = mutableListOf<LineRange>()
    val stack = mutableListOf<Int>()

    lines.forEachIndexed { index, line ->
        val trimmed = line.trim()

        if (trimmed.contains("{") || trimmed.contains("[")) {
            if (trimmed.endsWith("{") || trimmed.endsWith("[")) {
                stack.add(index)
            }
        }

        if (trimmed.startsWith("}") || trimmed.startsWith("]")) {
            if (stack.isNotEmpty()) {
                val start = stack.removeLastOrNull()
                if (start != null && index > start) {
                    ranges.add(LineRange(start, index))
                }
            }
        }
    }

    return ranges.filter { it.end - it.start > 1 }
}

@OptIn(ExperimentalSerializationApi::class)
private fun formatJson(input: String): Triple<String, Boolean, String> {
    if (input.isEmpty()) return Triple("", true, "")

    return try {
        val jsonElement = Json.parseToJsonElement(input)
        val formatted = Json {
            prettyPrint = true
            prettyPrintIndent = " ".repeat(2)
        }
        val formattedJson = formatted.encodeToString(JsonElement.serializer(), jsonElement)
        Triple(formattedJson, true, "JSON formatted successfully!")
    } catch (e: Exception) {
        Triple("", false, "Invalid JSON: ${e.message}")
    }
}

private fun minifyJson(input: String): Triple<String, Boolean, String> {
    if (input.isEmpty()) return Triple("", true, "")

    return try {
        val jsonElement = Json.parseToJsonElement(input)
        val minified = Json.encodeToString(JsonElement.serializer(), jsonElement)
        Triple(minified, true, "JSON minified successfully!")
    } catch (e: Exception) {
        Triple("", false, "Invalid JSON: ${e.message}")
    }
}

private fun formatXml(input: String): Triple<String, Boolean, String> {
    if (input.isEmpty()) return Triple("", true, "")

    return try {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val document = builder.parse(InputSource(StringReader(input)))

        val transformer = TransformerFactory.newInstance().newTransformer()
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", 2.toString())
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no")

        val source = DOMSource(document)
        val writer = StringWriter()
        val result = StreamResult(writer)
        transformer.transform(source, result)

        Triple(writer.toString(), true, "✅ XML formatted successfully!")
    } catch (e: Exception) {
        Triple("", false, "❌ Invalid XML: ${e.message}")
    }
}

private fun minifyXml(input: String): Triple<String, Boolean, String> {
    if (input.isEmpty()) return Triple("", true, "")

    return try {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val document = builder.parse(InputSource(StringReader(input)))

        val transformer = TransformerFactory.newInstance().newTransformer()
        transformer.setOutputProperty(OutputKeys.INDENT, "no")
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")

        val source = DOMSource(document)
        val writer = StringWriter()
        val result = StreamResult(writer)
        transformer.transform(source, result)

        val minified = writer.toString().replace(">\\s+<".toRegex(), "><")
        Triple(minified, true, "✅ XML minified successfully!")
    } catch (e: Exception) {
        Triple("", false, "❌ Invalid XML: ${e.message}")
    }
}

private fun getSampleJson(): String = """
{
  "name": "John Doe",
  "age": 30,
  "isEmployed": true,
  "address": {
    "street": "123 Main St",
    "city": "New York",
    "zipCode": "10001"
  },
  "hobbies": [
    "reading",
    "swimming",
    "coding"
  ],
  "spouse": null,
  "children": [
    {
      "name": "Jane Doe",
      "age": 8
    },
    {
      "name": "Bob Doe",
      "age": 12
    }
  ]
}
""".trimIndent()

private fun getSampleXml(): String = """
<?xml version="1.0" encoding="UTF-8"?>
<person>
  <name>John Doe</name>
  <age>30</age>
  <isEmployed>true</isEmployed>
  <address>
    <street>123 Main St</street>
    <city>New York</city>
    <zipCode>10001</zipCode>
  </address>
  <hobbies>
    <hobby>reading</hobby>
    <hobby>swimming</hobby>
    <hobby>coding</hobby>
  </hobbies>
  <children>
    <child>
      <name>Jane Doe</name>
      <age>8</age>
    </child>
    <child>
      <name>Bob Doe</name>
      <age>12</age>
    </child>
  </children>
</person>
""".trimIndent()
