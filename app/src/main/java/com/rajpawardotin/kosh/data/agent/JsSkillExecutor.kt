package com.rajpawardotin.kosh.data.agent

import android.content.Context
import android.webkit.WebView
import com.rajpawardotin.kosh.domain.agent.Skill
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

class JsSkill(
    private val context: Context,
    override val name: String,
    override val description: String,
    private val jsCode: String
) : Skill {

    override fun getSchema(): String {
        // Evaluate getSchema from JS script or return a dynamic one
        val script = "$jsCode\n;typeof getSchema === 'function' ? getSchema() : '{}';"
        
        // Run blocking here ONLY if called from background thread, but getSchema() is usually lightweight
        // To be safe, we return a simple JSON representing the name/description if JS getSchema fails or blocks.
        return try {
            kotlinx.coroutines.runBlocking(Dispatchers.Main) {
                evaluateJsSuspend(script)
            }
        } catch (e: Exception) {
            """
            {
              "name": "$name",
              "description": "$description",
              "parameters": {
                "type": "object",
                "properties": {},
                "required": []
              }
            }
            """.trimIndent()
        }
    }

    override suspend fun execute(arguments: Map<String, Any>): String {
        val argsJson = JSONObject(arguments).toString()
        val escapedArgs = argsJson.replace("\\", "\\\\").replace("'", "\\'")
        val script = "$jsCode\n;typeof execute === 'function' ? execute('$escapedArgs') : 'Error: execute() function not defined';"
        return evaluateJsSuspend(script)
    }

    private suspend fun evaluateJsSuspend(script: String): String = withContext(Dispatchers.Main) {
        val deferred = CompletableDeferred<String>()
        var webView: WebView? = null
        try {
            webView = WebView(context)
            webView.settings.javaScriptEnabled = true
            webView.evaluateJavascript(script) { result ->
                val cleanResult = if (result != null && result.startsWith("\"") && result.endsWith("\"") && result.length >= 2) {
                    result.substring(1, result.length - 1)
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\")
                        .replace("\\n", "\n")
                        .replace("\\t", "\t")
                } else if (result == "null") {
                    ""
                } else {
                    result ?: ""
                }
                deferred.complete(cleanResult)
                try {
                    webView?.destroy()
                } catch (e: Exception) {
                    // Ignore
                }
            }
        } catch (e: Exception) {
            deferred.complete("Error: ${e.message}")
            try {
                webView?.destroy()
            } catch (ex: Exception) {
                // Ignore
            }
        }

        try {
            withTimeoutOrNull(5000L) {
                deferred.await()
            } ?: "Error: JS execution timed out after 5 seconds"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}
