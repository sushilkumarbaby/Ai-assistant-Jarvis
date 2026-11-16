package com.jarvis.assistant.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.jarvis.assistant.JarvisApplication
import com.jarvis.assistant.ai.JarvisBrain
import com.jarvis.assistant.ui.components.ArcReactorView
import com.jarvis.assistant.ui.theme.*
import kotlinx.coroutines.launch

class AssistOverlayActivity : ComponentActivity() {

    private val brain = JarvisBrain()
    private val speechManager get() = JarvisApplication.instance.speechManager
    private val prefs get() = JarvisApplication.instance.prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            JarvisTheme {
                AssistOverlayScreen(
                    onDismiss = { finish() },
                    onStartListening = { onResult, onError ->
                        speechManager.startListening(
                            onResult = onResult,
                            onError = onError
                        )
                    },
                    onProcessQuery = { query, onComplete ->
                        lifecycleScope.launch {
                            val reply = brain.processCommand(this@AssistOverlayActivity, query)
                            onComplete(reply)
                            speechManager.speak(reply) {
                                // Auto-finish when speech completes if desired, or keep open
                            }
                        }
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechManager.stopListening()
        speechManager.stopSpeaking()
    }
}

@Composable
fun AssistOverlayScreen(
    onDismiss: () -> Unit,
    onStartListening: ((String) -> Unit, (String) -> Unit) -> Unit,
    onProcessQuery: (String, (String) -> Unit) -> Unit
) {
    var stateText by remember { mutableStateOf("INITIALIZING...") }
    var recognizedText by remember { mutableStateOf("") }
    var jarvisReply by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(true) }
    var isThinking by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        stateText = "LISTENING, SIR..."
        onStartListening(
            { query ->
                recognizedText = query
                isListening = false
                isThinking = true
                stateText = "PROCESSING NEURAL QUERY..."
                onProcessQuery(query) { reply ->
                    jarvisReply = reply
                    isThinking = false
                    stateText = "JARVIS"
                }
            },
            { error ->
                isListening = false
                stateText = "STANDBY"
                jarvisReply = "I could not detect any speech, sir. Tap the reactor to retry."
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Floating Bottom HUD Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {} // Consume click inside card
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = BgCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Status Header
                Text(
                    text = stateText,
                    color = if (isListening) RedGlow else CyanGlow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Arc Reactor
                ArcReactorView(
                    size = 90.dp,
                    isActive = isListening || isThinking,
                    coreColor = if (isListening) RedGlow else CyanGlow,
                    modifier = Modifier.clickable {
                        if (!isListening && !isThinking) {
                            isListening = true
                            stateText = "LISTENING, SIR..."
                            jarvisReply = ""
                            recognizedText = ""
                            onStartListening(
                                { query ->
                                    recognizedText = query
                                    isListening = false
                                    isThinking = true
                                    stateText = "PROCESSING..."
                                    onProcessQuery(query) { reply ->
                                        jarvisReply = reply
                                        isThinking = false
                                        stateText = "JARVIS"
                                    }
                                },
                                {
                                    isListening = false
                                    stateText = "STANDBY"
                                    jarvisReply = "Listening timed out, sir."
                                }
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // User Query Transcription
                if (recognizedText.isNotEmpty()) {
                    Text(
                        text = "“$recognizedText”",
                        color = TextDim,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // JARVIS Response
                if (jarvisReply.isNotEmpty()) {
                    Text(
                        text = jarvisReply,
                        color = TextWhite,
                        fontSize = 14.5.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Dismiss / Close Bar
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanDim),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Dismiss", color = CyanGlow, fontSize = 12.sp)
                }
            }
        }
    }
}
