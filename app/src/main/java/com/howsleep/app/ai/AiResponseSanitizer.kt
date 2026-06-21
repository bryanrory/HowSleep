package com.howsleep.app.ai

object AiResponseSanitizer {

    private val CODE_BLOCK_PATTERN = Regex("```(?:json)?\\s*([\\s\\S]*?)```")
    private val JSON_OBJECT_PATTERN = Regex("\\{[\\s\\S]*\\}")

    fun sanitize(raw: String): String {
        val trimmed = raw.trim()

        // Extract from code block if present
        val fromCodeBlock = CODE_BLOCK_PATTERN.find(trimmed)?.groupValues?.get(1)?.trim()
        if (fromCodeBlock != null) return fromCodeBlock

        // Extract first JSON object found in the text
        val jsonObject = JSON_OBJECT_PATTERN.find(trimmed)?.value
        if (jsonObject != null) return jsonObject

        return trimmed
    }
}
