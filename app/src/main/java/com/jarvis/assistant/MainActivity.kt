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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.jarvis.assistant.ai.JarvisBrain
import com.jarvis.assistant.ui.components.ArcReactorButton
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
                    },
                    onStopVoice = {
                        speechManager.stopSpeaking()
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
            val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            } catch (e2: Exception) {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
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
    onStartVoice: ((String) -> Unit, (String) -> Unit) -> Unit,
    onStopVoice: () -> Unit
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
                    "All systems online, ${prefs.userName}. Tap my Arc Reactor to command.",
                    false
                )
            )
        )
    }
    var inputText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: HUD, 1: Settings, 2: Memory

    Scaffold(
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isListening) CyanBright else CyanGlow)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "JARVIS",
                            color = CyanGlow,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 3.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                },
                actions = {
                    // Sleek corner settings button to keep HUD pure and uncluttered
                    IconButton(
                        onClick = { selectedTab = if (selectedTab == 1) 0 else 1 },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .border(1.dp, if (selectedTab == 1) CyanGlow else CyanBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = if (selectedTab == 1) CyanGlow else TextDim,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgDark)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = BgCard,
                tonalElevation = 0.dp,
                modifier = Modifier.border(
                    width = 1.dp,
                    color = CyanBorder.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                )
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Adjust, contentDescription = "HUD", tint = if (selectedTab == 0) CyanGlow else TextDim) },
                    label = { Text("HUD", color = if (selectedTab == 0) CyanGlow else TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Tune, contentDescription = "Config", tint = if (selectedTab == 1) CyanGlow else TextDim) },
                    label = { Text("PROTOCOLS", color = if (selectedTab == 1) CyanGlow else TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Memory, contentDescription = "Memory", tint = if (selectedTab == 2) CyanGlow else TextDim) },
                    label = { Text("MEMORY", color = if (selectedTab == 2) CyanGlow else TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
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
                0 -> HudScreen(
                    messages = messages,
                    inputText = inputText,
                    isListening = isListening,
                    userName = prefs.userName,
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
                    onToggleReactor = {
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
                            onStopVoice()
                        }
                    }
                )
                1 -> ProtocolsSettingsScreen(
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
                2 -> MemoryScreen(memoryStorage = memoryStorage)
            }
        }
    }
}

/**
 * Clean, Clutter-Free Stark HUD Screen.
 * Contains the prominent center Arc Reactor Button and clean conversational log.
 */
@Composable
fun HudScreen(
    messages: List<ChatMessage>,
    inputText: String,
    isListening: Boolean,
    userName: String,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onToggleReactor: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Center Tony Stark Arc Reactor Interactive Button
        ArcReactorButton(
            isActive = isListening,
            onClick = onToggleReactor,
            size = 230.dp,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Clean conversational telemetry stream
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            reverseLayout = true,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages.reversed()) { msg ->
                Card(
                    modifier = Modifier.fillMaxWidth(if (msg.isUser) 0.85f else 0.95f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (msg.isUser) Color(0xFF131D32) else BgCard
                    ),
                    border = if (!msg.isUser) androidx.compose.foundation.BorderStroke(1.dp, CyanBorder) else null
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                msg.sender.uppercase(),
                                color = if (msg.isUser) CyanBright else CyanGlow,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            msg.text,
                            color = TextWhite,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Sleek Bottom Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChanged,
                placeholder = {
                    Text(
                        if (isListening) "Listening to your voice..." else "Command JARVIS...",
                        color = TextDim,
                        fontSize = 13.sp
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanGlow,
                    unfocusedBorderColor = CyanBorder,
                    focusedContainerColor = BgCard,
                    unfocusedContainerColor = BgCard
                ),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSend,
                modifier = Modifier
                    .size(46.dp)
                    .background(CyanGlow, CircleShape)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = BgDark, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/**
 * Dedicated System Settings & Protocols Screen.
 * Houses God-Mode Autopilot, Default Assistant, API Keys, and Voice tuning.
 */
@Composable
fun ProtocolsSettingsScreen(
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
    val isGodModeActive = com.jarvis.assistant.service.JarvisAccessibilityService.isServiceRunning

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "SYSTEM PROTOCOLS",
                color = CyanGlow,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // 1. Digital Assistant Integration (Replace Gemini / Siri)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BgCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Smartphone, contentDescription = null, tint = CyanGlow, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Default Assistant Integration", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Replace Google Gemini or Siri on your phone. Long-press Power or Home to summon JARVIS.",
                        color = TextDim,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onOpenAssistantSettings,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanGlow),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Set as Default Assistant", color = BgDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. God-Mode Autopilot (Accessibility Status & Toggle)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BgCard),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isGodModeActive) Color(0xFF10B981) else CyanBorder
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isGodModeActive) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isGodModeActive) Color(0xFF34D399) else Color(0xFFFBBF24),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            if (isGodModeActive) "God-Mode Autopilot (Active)" else "God-Mode Autopilot (Offline)",
                            color = if (isGodModeActive) Color(0xFF34D399) else Color(0xFFFBBF24),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Enables hands-free reels scrolling, clicking any named button on screen, and cross-app workflows.",
                        color = TextDim,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onOpenAccessibilitySettings,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isGodModeActive) Color(0xFF059669) else CyanGlow
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            if (isGodModeActive) "Accessibility Enabled ✓" else "Configure Accessibility",
                            color = if (isGodModeActive) TextWhite else BgDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. Neural Engine API Key
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BgCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Google Gemini Neural Core", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = onApiKeyChanged,
                        placeholder = { Text("Enter Gemini API key (AIzaSy...)", color = TextDim, fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanGlow,
                            unfocusedBorderColor = CyanBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Addressing Title", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = userName,
                        onValueChange = onUserNameChanged,
                        placeholder = { Text("Boss / Sir", color = TextDim, fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanGlow,
                            unfocusedBorderColor = CyanBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Male Voice Speech Rate: ${String.format("%.2f", voiceRate)}x", color = TextWhite, fontSize = 12.sp)
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
}

/**
 * Long-Term Persistent Memory Matrix Screen.
 */
@Composable
fun MemoryScreen(memoryStorage: com.jarvis.assistant.data.MemoryStorage) {
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
            Text(
                "LONG-TERM MEMORY",
                color = CyanGlow,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.Monospace
            )
            if (memories.isNotEmpty()) {
                TextButton(onClick = {
                    memoryStorage.clearAll()
                    memories = emptyList()
                }) {
                    Text("Clear All", color = RedGlow, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (memories.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Memory core initialized.\nSay 'Remember that...' to log facts.",
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
