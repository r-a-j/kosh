package com.rajpawardotin.kosh.domain.usecase

import com.rajpawardotin.kosh.data.ModelTag

class ModelRouter {
    fun detectIntent(prompt: String, hasDocuments: Boolean): ModelTag {
        if (hasDocuments) {
            return ModelTag.RAG_READER
        }
        
        val coderKeywords = setOf(
            "code", "program", "function", "class", "compile", "syntax", "bug", 
            "kotlin", "java", "python", "javascript", "rust", "c++", "html", "css", 
            "database", "sql", "api", "json", "xml", "git", "script", "algorithm",
            "recursive", "regex", "array", "pointer", "object", "interface", "struct",
            "implement", "abstract", "override", "lambda", "binary", "hex", "parse",
            "refactor", "snippet", "compiler", "runtime", "thread", "coroutine"
        )
        
        val words = prompt.lowercase().split(Regex("[^a-zA-Z+#]")).filter { it.isNotEmpty() }
        val matchCount = words.count { it in coderKeywords }
        
        return if (matchCount >= 2 || (matchCount >= 1 && prompt.length < 100)) {
            ModelTag.CODER
        } else {
            ModelTag.GENERAL
        }
    }
}
