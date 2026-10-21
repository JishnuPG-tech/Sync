package com.ghostmesh.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostmesh.app.data.model.ChatMessage
import com.ghostmesh.app.ui.components.AuraInitialsAvatar
import com.ghostmesh.app.ui.theme.AuraTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    peerName: String,
    peerInstagram: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                id = "1",
                senderId = "peer",
                text = "Hey! We just crossed paths on Plex 👋",
                timestamp = "10:24 AM",
                isFromMe = false
            ),
            ChatMessage(
                id = "2",
                senderId = "me",
                text = "Yes! Love your profile intro. Were you at the campus cafe?",
                timestamp = "10:25 AM",
                isFromMe = true
            ),
            ChatMessage(
                id = "3",
                senderId = "peer",
                text = "Haha yes, working on some design prototypes! Let's connect on IG too: @$peerInstagram",
                timestamp = "10:26 AM",
                isFromMe = false
            )
        )
    }

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        listState.animateScrollToItem(messages.size)
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = AuraTheme.colors.canvas
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Chat Header
            Surface(
                color = AuraTheme.colors.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AuraTheme.colors.border)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = AuraTheme.colors.ink
                        )
                    }

                    AuraInitialsAvatar(name = peerName, size = 42)

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = peerName,
                            style = AuraTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AuraTheme.colors.ink
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(AuraTheme.colors.success)
                            )
                            Text(
                                text = "Active now • @$peerInstagram",
                                style = AuraTheme.typography.labelSmall,
                                color = AuraTheme.colors.inkSecondary
                            )
                        }
                    }
                }
            }

            // Messages List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    val isMe = msg.isFromMe
                    val bubbleBg = if (isMe) AuraTheme.colors.accent else AuraTheme.colors.surface
                    val textColor = if (isMe) {
                        if (AuraTheme.colors.isDark) AuraTheme.colors.canvas else Color.White
                    } else {
                        AuraTheme.colors.ink
                    }
                    val timestampColor = if (isMe) {
                        if (AuraTheme.colors.isDark) AuraTheme.colors.canvas.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.7f)
                    } else {
                        AuraTheme.colors.inkMuted
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                    ) {
                        Column(
                            modifier = Modifier
                                .widthIn(max = 280.dp)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isMe) 16.dp else 4.dp,
                                        bottomEnd = if (isMe) 4.dp else 16.dp
                                    )
                                )
                                .background(bubbleBg)
                                .border(
                                    width = if (isMe) 0.dp else 1.dp,
                                    color = AuraTheme.colors.border,
                                    shape = RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isMe) 16.dp else 4.dp,
                                        bottomEnd = if (isMe) 4.dp else 16.dp
                                    )
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = msg.text,
                                style = AuraTheme.typography.bodyMedium,
                                color = textColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = msg.timestamp,
                                style = AuraTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = timestampColor,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }
            }

            // Input bar
            Surface(
                color = AuraTheme.colors.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AuraTheme.colors.border)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = "Type a message...",
                                style = AuraTheme.typography.bodyMedium,
                                color = AuraTheme.colors.inkMuted
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = AuraTheme.colors.surfaceSubtle,
                            unfocusedContainerColor = AuraTheme.colors.surfaceSubtle,
                            focusedBorderColor = AuraTheme.colors.accent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = AuraTheme.colors.ink,
                            unfocusedTextColor = AuraTheme.colors.ink
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                val userMsg = inputText.trim()
                                messages.add(
                                    ChatMessage(
                                        id = System.currentTimeMillis().toString(),
                                        senderId = "me",
                                        text = userMsg,
                                        timestamp = "Just now",
                                        isFromMe = true
                                    )
                                )
                                inputText = ""

                                // Simulated friendly peer response
                                coroutineScope.launch {
                                    delay(1200)
                                    messages.add(
                                        ChatMessage(
                                            id = (System.currentTimeMillis() + 1).toString(),
                                            senderId = "peer",
                                            text = "Awesome! Really glad we connected on Plex!",
                                            timestamp = "Just now",
                                            isFromMe = false
                                        )
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(AuraTheme.colors.accent)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Send,
                            contentDescription = "Send",
                            tint = if (AuraTheme.colors.isDark) AuraTheme.colors.canvas else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
