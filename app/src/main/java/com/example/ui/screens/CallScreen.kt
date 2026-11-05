package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun CallScreen(
    targetName: String,
    callType: String, // "audio" or "video"
    onEndCall: () -> Unit
) {
    var callDurationSeconds by remember { mutableStateOf(0L) }
    var callStatus by remember { mutableStateOf("Connecting...") }
    var earnedPaise by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        delay(2000)
        callStatus = "Connected"
        while (true) {
            delay(1000)
            callDurationSeconds++
            // Every 15 seconds qualifying interval, add earnings (e.g. ₹4 = 400 paise)
            if (callDurationSeconds % 15 == 0L) {
                earnedPaise += 400
            }
        }
    }

    val minutes = callDurationSeconds / 60
    val seconds = callDurationSeconds % 60
    val timeFormatted = "%02d:%02d".format(minutes, seconds)
    val earnedRupees = earnedPaise / 100.0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Header
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 40.dp)) {
                Text(
                    text = callType.uppercase() + " CALL",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Cyan,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = targetName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = callStatus,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (callStatus == "Connected") Color.Green else Color.Yellow
                )
                if (callStatus == "Connected") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Earned so far: ₹%.2f".format(earnedRupees),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF10B981)
                    )
                }
            }

            // Center Avatar or Video placeholder
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .background(Color(0xFF334155), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = targetName.take(1).uppercase(),
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            // Bottom Call Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 40.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { },
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0xFF334155), CircleShape)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Mute", tint = Color.White)
                }

                IconButton(
                    onClick = onEndCall,
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color.Red, CircleShape)
                ) {
                    Icon(Icons.Default.CallEnd, contentDescription = "End Call", tint = Color.White)
                }

                if (callType == "video") {
                    IconButton(
                        onClick = { },
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color(0xFF334155), CircleShape)
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = "Camera", tint = Color.White)
                    }
                }
            }
        }
    }
}
