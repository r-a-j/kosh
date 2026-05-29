package com.rajpawardotin.kosh.ui.chat.components

import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun MathFormulaCard(formula: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131316)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        // Make WebView background transparent
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                        }
                        
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                            }
                        }

                        val escapedFormula = formula
                            .replace("\\", "\\\\")
                            .replace("'", "\\'")
                            .replace("\n", " ")

                        // Local HTML containing KaTeX resources from jsdelivr CDN
                        // Fallback script displays raw LaTeX if stylesheet or JS fails to load (e.g. offline)
                        val html = """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
                                <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/katex.min.css" integrity="sha384-G5sc4EBW1569o63kR9wS8B4S4V+bA9W+uEaI3L+8Gq/tI1AdfQpZ8sQ2e4O6y1j" crossorigin="anonymous">
                                <script src="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/katex.min.js" integrity="sha384-tZg133P15j37M91t+h/H9WdC/n1A9E3m4W1O+I/2N+d5V6WvF9C6M3L2uD5u" crossorigin="anonymous"></script>
                                <style>
                                    body {
                                        margin: 0;
                                        padding: 8px;
                                        background-color: transparent !important;
                                        color: #E4E4E7;
                                        font-family: 'Courier New', Courier, monospace;
                                        display: flex;
                                        justify-content: center;
                                        align-items: center;
                                        overflow-x: auto;
                                        overflow-y: hidden;
                                        -webkit-user-select: none;
                                        user-select: none;
                                    }
                                    #math {
                                        font-size: 1.2em;
                                        text-align: center;
                                        white-space: nowrap;
                                    }
                                </style>
                            </head>
                            <body>
                                <div id="math">$$escapedFormula</div>
                                <script>
                                    window.onload = function() {
                                        try {
                                            if (typeof katex !== 'undefined') {
                                                katex.render('$escapedFormula', document.getElementById('math'), {
                                                    throwOnError: false,
                                                    displayMode: true
                                                });
                                            } else {
                                                // Fallback display if KaTeX package is not loaded
                                                document.getElementById('math').textContent = '$$escapedFormula';
                                            }
                                        } catch (e) {
                                            document.getElementById('math').textContent = '$$escapedFormula';
                                        }
                                    };
                                </script>
                            </body>
                            </html>
                        """.trimIndent()
                        
                        loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                    }
                },
                update = { webView ->
                    val escapedFormula = formula
                        .replace("\\", "\\\\")
                        .replace("'", "\\'")
                        .replace("\n", " ")

                    val html = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
                            <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/katex.min.css">
                            <script src="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/katex.min.js"></script>
                            <style>
                                body {
                                    margin: 0;
                                    padding: 8px;
                                    background-color: transparent !important;
                                    color: #E4E4E7;
                                    font-family: 'Courier New', Courier, monospace;
                                    display: flex;
                                    justify-content: center;
                                    align-items: center;
                                    overflow-x: auto;
                                    overflow-y: hidden;
                                }
                                #math {
                                    font-size: 1.2em;
                                    text-align: center;
                                    white-space: nowrap;
                                }
                            </style>
                        </head>
                        <body>
                            <div id="math">$$escapedFormula</div>
                            <script>
                                window.onload = function() {
                                    try {
                                        if (typeof katex !== 'undefined') {
                                            katex.render('$escapedFormula', document.getElementById('math'), {
                                                throwOnError: false,
                                                displayMode: true
                                            });
                                        }
                                    } catch (e) {}
                                };
                            </script>
                        </body>
                        </html>
                    """.trimIndent()
                    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp, max = 150.dp)
            )
        }
    }
}
