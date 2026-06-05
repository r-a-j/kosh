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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun MathFormulaCard(formula: String, modifier: Modifier = Modifier) {
    val textColor = MaterialTheme.colorScheme.onSurface
    val textColorHex = String.format(java.util.Locale.US, "#%06X", textColor.toArgb() and 0xFFFFFF)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            allowFileAccess = true
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

                        // Local HTML loading KaTeX from file:///android_asset/
                        val html = """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
                                <link rel="stylesheet" href="file:///android_asset/katex/katex.min.css">
                                <script src="file:///android_asset/katex/katex.min.js"></script>
                                <style>
                                    body {
                                        margin: 0;
                                        padding: 2px 4px;
                                        background-color: transparent !important;
                                        color: $textColorHex;
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
                                        font-size: 1.15em;
                                        text-align: center;
                                        white-space: nowrap;
                                    }
                                    .katex-display {
                                        margin: 4px 0 !important;
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
                        
                        loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null)
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
                            <link rel="stylesheet" href="file:///android_asset/katex/katex.min.css">
                            <script src="file:///android_asset/katex/katex.min.js"></script>
                            <style>
                                body {
                                    margin: 0;
                                    padding: 2px 4px;
                                    background-color: transparent !important;
                                    color: $textColorHex;
                                    font-family: 'Courier New', Courier, monospace;
                                    display: flex;
                                    justify-content: center;
                                    align-items: center;
                                    overflow-x: auto;
                                    overflow-y: hidden;
                                }
                                #math {
                                    font-size: 1.15em;
                                    text-align: center;
                                    white-space: nowrap;
                                }
                                .katex-display {
                                    margin: 4px 0 !important;
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
                    webView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 36.dp, max = 120.dp)
            )
        }
    }
}
