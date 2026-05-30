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
import com.rajpawardotin.kosh.data.KoshDatabaseHelper
import com.rajpawardotin.kosh.data.SQLiteMessageRepository
import com.rajpawardotin.kosh.data.SQLiteSessionRepository
import com.rajpawardotin.kosh.data.SQLiteDocumentRepository
import com.rajpawardotin.kosh.data.SharedPrefsSettingsProvider
import com.rajpawardotin.kosh.data.TtsProvider
import com.rajpawardotin.kosh.data.TtsProviderImpl
import com.rajpawardotin.kosh.ui.chat.ChatScreen
import com.rajpawardotin.kosh.ui.chat.ChatViewModel
import com.rajpawardotin.kosh.ui.theme.KoshTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private lateinit var chatViewModel: ChatViewModel
    private lateinit var ttsProvider: TtsProvider
    private var currentPermissionRequest: com.rajpawardotin.kosh.domain.agent.PermissionRequest? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(applicationContext)
        
        val requestPermissionLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            currentPermissionRequest?.deferred?.complete(isGranted)
            currentPermissionRequest = null
        }
        
        val aiProvider = LiteRTModelProvider(applicationContext)
        val modelLibraryManager = com.rajpawardotin.kosh.data.ModelLibraryManager(applicationContext)
        val modelRouter = com.rajpawardotin.kosh.domain.usecase.ModelRouter()

        val searchProvider = SearchProviderImpl(applicationContext)
        val dbHelper = KoshDatabaseHelper(applicationContext)
        val sessionRepository = SQLiteSessionRepository(dbHelper)
        val messageRepository = SQLiteMessageRepository(dbHelper)
        val documentRepository = SQLiteDocumentRepository(dbHelper)
        val settingsProvider = SharedPrefsSettingsProvider(applicationContext)
        ttsProvider = TtsProviderImpl(applicationContext)
        val viewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ChatViewModel(applicationContext, aiProvider, searchProvider, sessionRepository, messageRepository, documentRepository, settingsProvider, ttsProvider, modelLibraryManager, modelRouter) as T
            }
        }

        chatViewModel = ViewModelProvider(this, viewModelFactory)[ChatViewModel::class.java]

        lifecycleScope.launch {
            chatViewModel.permissionRequestFlow.collect { request ->
                currentPermissionRequest = request
                requestPermissionLauncher.launch(request.permission)
            }
        }

        setContent {
            KoshTheme(darkTheme = true) {
                val isScreenshotEnabled = chatViewModel.isScreenshotEnabled
                LaunchedEffect(isScreenshotEnabled) {
                    if (isScreenshotEnabled) {
                        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }

                ChatScreen(
                    viewModel = chatViewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (::chatViewModel.isInitialized) {
            chatViewModel.startTrackingMetrics()
        }
    }

    override fun onStop() {
        super.onStop()
        if (::chatViewModel.isInitialized) {
            chatViewModel.stopTrackingMetrics()
            chatViewModel.lockAppOnBackground()
        }
        ttsProvider.stop()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        ttsProvider.shutdown()
    }
}
