package com.rajpawardotin.kosh.domain.agent

import com.rajpawardotin.kosh.domain.provider.AIProvider
import com.rajpawardotin.kosh.ui.chat.ResponseParser
import com.rajpawardotin.kosh.ui.chat.ToolCall

class AgentLoopExecutor(
    private val aiProvider: AIProvider,
    private val skills: List<Skill>
) {
    companion object {
        const val MAX_AGENT_TURNS = 5
    }

    suspend fun executeAgentLoop(
        initialPrompt: String,
        onStatusUpdate: suspend (String) -> Unit,
        onTokenReceived: suspend (String) -> Unit
    ): String {
        var currentPrompt = initialPrompt
        var turnCount = 0
        var finalTextResponse = ""

        while (turnCount < MAX_AGENT_TURNS) {
            turnCount++
            onStatusUpdate("Thinking (Turn $turnCount)...")
            
            val responseBuffer = StringBuilder()
            
            // Run LLM call
            aiProvider.sendMessage(currentPrompt).collect { chunk ->
                responseBuffer.append(chunk)
                // If it is not a tool call pattern, stream the tokens immediately to the user
                if (!rawResponseContainsJson(responseBuffer.toString())) {
                    onTokenReceived(chunk)
                }
            }

            val rawResponse = responseBuffer.toString().trim()
            val toolCalls = ResponseParser.parseToolCalls(rawResponse)
            
            if (toolCalls.isEmpty()) {
                // No tool calls - this is the final answer!
                finalTextResponse = rawResponse
                break
            }

            // Process tool calls
            val toolOutputs = StringBuilder()
            for (toolCall in toolCalls) {
                val skill = skills.find { it.name.lowercase() == toolCall.name.lowercase() }
                if (skill == null) {
                    toolOutputs.append("Error: Tool '${toolCall.name}' not found. Available tools: ${skills.map { it.name }.joinToString(", ")}\n")
                    continue
                }
                
                // Construct a clean parameter string for UI status
                val paramStr = toolCall.arguments.entries.joinToString(", ") { "${it.key}=${it.value}" }
                onStatusUpdate("Executing: ${skill.name}($paramStr)...")
                
                val output = try {
                    skill.execute(toolCall.arguments)
                } catch (e: Exception) {
                    "Error executing ${skill.name}: ${e.message}"
                }
                toolOutputs.append("Tool '${skill.name}' returned:\n$output\n")
            }

            // Build the next prompt context
            currentPrompt += "\n\nAssistant Tool Invocation:\n$rawResponse\n\nSystem Tool Response:\n$toolOutputs"
            
            onStatusUpdate("Analyzing tool results...")
        }

        if (turnCount >= MAX_AGENT_TURNS) {
            finalTextResponse = "[Agent Halt: Turn limit reached]\n\n" + finalTextResponse
        }

        return finalTextResponse
    }

    private fun rawResponseContainsJson(text: String): Boolean {
        val trimmed = text.trim()
        return trimmed.startsWith("{") || trimmed.contains("```json")
    }
}
