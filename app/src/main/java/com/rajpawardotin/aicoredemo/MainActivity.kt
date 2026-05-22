package com.rajpawardotin.aicoredemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rajpawardotin.aicoredemo.data.LiteRTModelProvider
import com.rajpawardotin.aicoredemo.data.SearchProviderImpl
import com.rajpawardotin.aicoredemo.ui.chat.ChatScreen
import com.rajpawardotin.aicoredemo.ui.chat.ChatViewModel
import com.rajpawardotin.aicoredemo.ui.theme.AICoreDemoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val aiProvider = LiteRTModelProvider(applicationContext)
        val searchProvider = SearchProviderImpl()
        val viewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ChatViewModel(aiProvider, searchProvider, applicationContext) as T
            }
        }

        setContent {
            AICoreDemoTheme(darkTheme = true) {
                val chatViewModel: ChatViewModel = viewModel(factory = viewModelFactory)
                ChatScreen(
                    viewModel = chatViewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
