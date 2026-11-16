package com.jarvis.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.jarvis.assistant.ai.JarvisBrain
import com.jarvis.assistant.ui.components.ArcReactorView
import com.jarvis.assistant.ui.theme.*
import kotlinx.coroutines.launch

data class ChatMessage(val sender: String, val text: String, val isUser: Boolean)

class MainActivity : ComponentActivity() {

    private val brain = JarvisBrain()
    private val speechManager get() = JarvisApplication.instance.speechManager
    private val prefs get() = JarvisApplication.instance.prefs
    private val memoryStorage get() = JarvisApplication.instance.memoryStorage

    private val requestRecordAudio =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            // Audio permission result
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request microphone permission if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            requestRecordAudio.launch(Manifest.permission.RECORD_AUDIO)
        }

        setContent {
            JarvisTheme {
                MainDashboardScreen(
                    prefs = prefs,
                    memoryStorage = memoryStorage,
                    onOpenAssistantSettings = {
                        openDefaultAssistantSettings()
                    },
                    onOpenAccessibilitySettings = {
                        openAccessibilitySettings()
                    },
                    onSendMessage = { query, onReply ->
                        lifecycleScope.launch {
                            val reply = brain.processCommand(this@MainActivity, query)
                            onReply(reply)
                            speechManager.speak(reply)
                        }
                    },
                    onStartVoice = { onResult, onError ->
                        speechManager.startListening(
                            onResult = onResult,
                            onError = onError
                        )
                    }
                )
            }
        }
    }

    private fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }
    }

    private fun openDefaultAssistantSettings() {
        try {
            // Directly opens Android's Default Apps / Voice Input Settings
            val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                startActivity(intent)
            } catch (e2: Exception) {
                val intent = Intent(Settings.ACTION_SETTINGS)
                startActivity(intent)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(
    prefs: com.jarvis.assistant.data.PreferencesManager,
    memoryStorage: com.jarvis.assistant.data.MemoryStorage,
    onOpenAssistantSettings: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onSendMessage: (String, (String) -> Unit) -> Unit,
    onStartVoice: ((String) -> Unit, (String) -> Unit) -> Unit
) {
    var apiKey by remember { mutableStateOf(prefs.geminiApiKey) }
    var userName by remember { mutableStateOf(prefs.userName) }
    var voiceRate by remember { mutableFloatStateOf(prefs.voiceRate) }
    var voicePitch by remember { mutableFloatStateOf(prefs.voicePitch) }
    var messages by remember {
        mutableStateOf(
            listOf(
                ChatMessage(
                    "JARVIS",
                    "Good day, ${prefs.userName}. All protocols active. Tap the button below to set me as your Default Android Assistant.",
                    false
                )
            )
        )
    }
    var inputText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Chat, 1: Assistant Settings, 2: Memory

    Scaffold(
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(CyanGlow, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "J.A.R.V.I.S. 4.0",
                            color = CyanGlow,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgDark)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = BgCard) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.ChatBubble, contentDescription = "HUD", tint = if (selectedTab == 0) CyanGlow else TextDim) },
                    label = { Text("HUD", color = if (selectedTab == 0) CyanGlow else TextDim, fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Config", tint = if (selectedTab == 1) CyanGlow else TextDim) },
                    label = { Text("Config", color = if (selectedTab == 1) CyanGlow else TextDim, fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Psychology, contentDescription = "Memory", tint = if (selectedTab == 2) CyanGlow else TextDim) },
                    label = { Text("Memory", color = if (selectedTab == 2) CyanGlow else TextDim, fontSize = 11.sp) }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                0 -> HudChatTab(
                    messages = messages,
                    inputText = inputText,
                    isListening = isListening,
                    onInputChanged = { inputText = it },
                    onSend = {
                        if (inputText.isNotBlank()) {
                            val text = inputText.trim()
                            inputText = ""
                            messages = messages + ChatMessage(prefs.userName, text, true)
                            onSendMessage(text) { reply ->
                                messages = messages + ChatMessage("JARVIS", reply, false)
                            }
                        }
                    },
                    onToggleMic = {
                        if (!isListening) {
                            isListening = true
                            onStartVoice(
                                { result ->
                                    isListening = false
                                    messages = messages + ChatMessage(prefs.userName, result, true)
                                    onSendMessage(result) { reply ->
                                        messages = messages + ChatMessage("JARVIS", reply, false)
                                    }
                                },
                                {
                                    isListening = false
                                }
                            )
                        } else {
                            isListening = false
                        }
                    },
                    onSetDefaultAssistant = onOpenAssistantSettings,
                    onSetAccessibility = onOpenAccessibilitySettings
                )
                1 -> ConfigTab(
                    apiKey = apiKey,
                    onApiKeyChanged = {
                        apiKey = it
                        prefs.geminiApiKey = it
                    },
                    userName = userName,
                    onUserNameChanged = {
                        userName = it
                        prefs.userName = it
                    },
                    voiceRate = voiceRate,
                    onVoiceRateChanged = {
                        voiceRate = it
                    },
                    voicePitch = voicePitch,
                    onVoicePitchChanged = {
                        voicePitch = it
                    },
                    onOpenAssistantSettings = onOpenAssistantSettings,
                    onOpenAccessibilitySettings = onOpenAccessibilitySettings
                )
                2 -> MemoryTab(memoryStorage = memoryStorage)
            }
        }
    }
}

@Composable
fun HudChatTab(
    messages: List<ChatMessage>,
    inputText: String,
    isListening: Boolean,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onToggleMic: () -> Unit,
    onSetDefaultAssistant: () -> Unit,
    onSetAccessibility: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Banner to replace Gemini / Google Assistant
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp)
                .clickable { onSetDefaultAssistant() },
            colors = CardDefaults.cardColors(containerColor = CyanDim),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = CyanGlow, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Replace Gemini / Google Assistant", color = CyanGlow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Set JARVIS as default digital assistant", color = TextDim, fontSize = 10.sp)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = CyanGlow)
            }
        }

        // Second Banner: God-Mode Autopilot (Accessibility)
        val isGodModeActive = com.jarvis.assistant.service.JarvisAccessibilityService.isServiceRunning
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp)
                .clickable { onSetAccessibility() },
            colors = CardDefaults.cardColors(containerColor = if (isGodModeActive) Color(0xFF064E3B) else Color(0xFF3B1E06)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (isGodModeActive) Icons.Default.CheckCircle else Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = if (isGodModeActive) Color(0xFF34D399) else Color(0xFFFBBF24),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (isGodModeActive) "God-Mode Autopilot: ACTIVE" else "God-Mode Autopilot: OFFLINE",
                        color = if (isGodModeActive) Color(0xFF34D399) else Color(0xFFFBBF24),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (isGodModeActive) "Reels scroll, auto-click & screen control ready" else "Tap to enable Screen Automation in Accessibility Settings",
                        color = TextDim,
                        fontSize = 10.sp
                    )
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = CyanGlow)
            }
        }

        // Center Arc Reactor
        ArcReactorView(
            size = 110.dp,
            isActive = isListening,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // Chat Message Feed
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                Card(
                    modifier = Modifier.fillMaxWidth(if (msg.isUser) 0.85f else 0.95f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (msg.isUser) Color(0xFF1E293B) else BgCard
                    ),
                    border = if (!msg.isUser) androidx.compose.foundation.BorderStroke(1.dp, CyanBorder) else null
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            msg.sender,
                            color = if (msg.isUser) TextDim else CyanGlow,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(msg.text, color = TextWhite, fontSize = 13.5.sp, lineHeight = 19.sp)
                    }
                }
            }
        }

        // Bottom Input Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = inputText,
                onValueChange = onInputChanged,
                placeholder = { Text("Command JARVIS...", color = TextDim, fontSize = 13.sp) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = BgCard,
                    unfocusedContainerColor = BgCard,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, CyanBorder, RoundedCornerShape(20.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onSend,
                colors = IconButtonDefaults.iconButtonColors(containerColor = CyanGlow)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = BgDark)
            }
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(
                onClick = onToggleMic,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (isListening) RedGlow else CyanGlow
                )
            ) {
                Icon(
                    if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Voice",
                    tint = if (isListening) TextWhite else BgDark
                )
            }
        }
    }
}

@Composable
fun ConfigTab(
    apiKey: String,
    onApiKeyChanged: (String) -> Unit,
    userName: String,
    onUserNameChanged: (String) -> Unit,
    voiceRate: Float,
    onVoiceRateChanged: (Float) -> Unit,
    voicePitch: Float,
    onVoicePitchChanged: (Float) -> Unit,
    onOpenAssistantSettings: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("SYSTEM CONFIGURATION", color = CyanGlow, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)

        // God-Mode Autopilot Card
        val isGodModeActive = com.jarvis.assistant.service.JarvisAccessibilityService.isServiceRunning
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BgCard)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    if (isGodModeActive) "⚡ God-Mode Autopilot (ONLINE)" else "⚠️ God-Mode Autopilot (OFFLINE)",
                    color = if (isGodModeActive) Color(0xFF34D399) else Color(0xFFFBBF24),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Enables autonomous reel scrolling, screen tapping, button clicking, and typing across all apps.",
                    color = TextDim,
                    fontSize = 11.5.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onOpenAccessibilitySettings,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isGodModeActive) Color(0xFF059669) else CyanGlow
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        if (isGodModeActive) "Accessibility Enabled ✓" else "Enable God-Mode Accessibility",
                        color = BgDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BgCard)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Default Assistant Integration", color = CyanGlow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("To replace Gemini or Siri on your phone, set JARVIS as your phone's Digital Assistant.", color = TextDim, fontSize = 11.5.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onOpenAssistantSettings,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanGlow),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Open Android Assistant Settings", color = BgDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BgCard)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Google Gemini Neural API Key", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = onApiKeyChanged,
                    placeholder = { Text("Paste your API key...", color = TextDim, fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanGlow, unfocusedBorderColor = CyanBorder),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Addressing Name / Title", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = userName,
                    onValueChange = onUserNameChanged,
                    placeholder = { Text("Sir", color = TextDim, fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanGlow, unfocusedBorderColor = CyanBorder),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Voice Speech Rate: ${String.format("%.2f", voiceRate)}x", color = TextWhite, fontSize = 12.sp)
                Slider(
                    value = voiceRate,
                    onValueChange = onVoiceRateChanged,
                    valueRange = 0.8f..1.3f,
                    colors = SliderDefaults.colors(thumbColor = CyanGlow, activeTrackColor = CyanGlow)
                )
            }
        }
    }
}

@Composable
fun MemoryTab(memoryStorage: com.jarvis.assistant.data.MemoryStorage) {
    var memories by remember { mutableStateOf(memoryStorage.getAllMemories()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("LONG-TERM MEMORY", color = CyanGlow, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            if (memories.isNotEmpty()) {
                TextButton(onClick = {
                    memoryStorage.clearAll()
                    memories = emptyList()
                }) {
                    Text("Clear All", color = RedGlow, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (memories.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No retained facts yet.\nSay 'Remember that...' to log facts.",
                    color = TextDim,
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(memories) { mem ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = BgCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(mem.fact, color = TextWhite, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                memoryStorage.removeMemory(mem.id)
                                memories = memoryStorage.getAllMemories()
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextDim)
                            }
                        }
                    }
                }
            }
        }
    }
}
