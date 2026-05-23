package com.rajpawardotin.kosh

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rajpawardotin.kosh.data.LiteRTModelProvider
import com.rajpawardotin.kosh.data.SearchProviderImpl
import com.rajpawardotin.kosh.data.SQLiteChatRepository
import com.rajpawardotin.kosh.data.SharedPrefsSettingsProvider
import com.rajpawardotin.kosh.data.TtsProvider
import com.rajpawardotin.kosh.data.TtsProviderImpl
import com.rajpawardotin.kosh.ui.chat.ChatScreen
import com.rajpawardotin.kosh.ui.chat.ChatViewModel
import com.rajpawardotin.kosh.ui.theme.KoshTheme

class MainActivity : FragmentActivity() {
    private lateinit var chatViewModel: ChatViewModel
    private lateinit var ttsProvider: TtsProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_SECURE,
            android.view.WindowManager.LayoutParams.FLAG_SECURE
        )

        enableEdgeToEdge()
        
        com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(applicationContext)
        
        val aiProvider = LiteRTModelProvider(applicationContext)

        val searchProvider = SearchProviderImpl(applicationContext)
        val chatRepository = SQLiteChatRepository(applicationContext)
        val settingsProvider = SharedPrefsSettingsProvider(applicationContext)
        ttsProvider = TtsProviderImpl(applicationContext)
        val viewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ChatViewModel(aiProvider, searchProvider, chatRepository, settingsProvider, ttsProvider) as T
            }
        }

        chatViewModel = ViewModelProvider(this, viewModelFactory)[ChatViewModel::class.java]

        setContent {
            KoshTheme(darkTheme = true) {
                ChatScreen(
                    viewModel = chatViewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (::chatViewModel.isInitialized) {
            chatViewModel.lockAppOnBackground()
        }
        ttsProvider.stop()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        ttsProvider.shutdown()
    }
}
