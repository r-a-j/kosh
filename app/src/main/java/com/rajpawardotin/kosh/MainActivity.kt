package com.rajpawardotin.kosh

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rajpawardotin.kosh.data.LiteRTModelProvider
import com.rajpawardotin.kosh.data.SearchProviderImpl
import com.rajpawardotin.kosh.ui.chat.ChatScreen
import com.rajpawardotin.kosh.ui.chat.ChatViewModel
import com.rajpawardotin.kosh.ui.theme.KoshTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val aiProvider = LiteRTModelProvider(applicationContext)
        val searchProvider = SearchProviderImpl(applicationContext)
        val viewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ChatViewModel(aiProvider, searchProvider, applicationContext) as T
            }
        }

        setContent {
            KoshTheme(darkTheme = true) {
                val chatViewModel: ChatViewModel = viewModel(factory = viewModelFactory)
                ChatScreen(
                    viewModel = chatViewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

