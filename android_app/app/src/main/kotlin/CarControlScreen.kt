package com.example.carremote.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.carremote.data.CarDevice
import com.example.carremote.network.CarApiClient
import com.example.carremote.network.CarCommandResult
import com.example.carremote.ui.theme.NeonGreen
import com.example.carremote.ui.theme.NeonRed
import kotlinx.coroutines.launch

private enum class ActionState { IDLE, SENDING, SUCCESS, ERROR }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarControlScreen(
    device: CarDevice,
    onBack: () -> Unit,
    onLockedStateChanged: (Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    val api = remember { CarApiClient() }

    var isLocked by remember { mutableStateOf(device.isLocked) }
    var actionState by remember { mutableStateOf(ActionState.IDLE) }
    var isOnline by remember { mutableStateOf<Boolean?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(device.id) {
        isOnline = api.checkStatus(device.ipAddress)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val baseColor by animateColorAsState(
        targetValue = when (actionState) {
            ActionState.SUCCESS -> if (isLocked) NeonRed else NeonGreen
            ActionState.ERROR -> MaterialTheme.colorScheme.error
            else -> if (isLocked) NeonRed.copy(alpha = 0.85f) else NeonGreen.copy(alpha = 0.85f)
        },
        animationSpec = tween(500),
        label = "baseColor"
    )

    fun sendCommand(command: String, newLockedValue: Boolean) {
        actionState = ActionState.SENDING
        errorText = null
        scope.launch {
            when (val result = api.sendCommand(device.ipAddress, device.sharedSecret, command)) {
                is CarCommandResult.Success -> {
                    isLocked = newLockedValue
                    onLockedStateChanged(newLockedValue)
                    actionState = ActionState.SUCCESS
                }
                is CarCommandResult.Failure -> {
                    errorText = result.message
                    actionState = ActionState.ERROR
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(device.name, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isOnline == true) Icons.Default.Wifi else Icons.Default.WifiOff,
                    contentDescription = null,
                    tint = if (isOnline == true) NeonGreen else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = when (isOnline) {
                        true -> "الجهاز متصل"
                        false -> "الجهاز غير متصل"
                        null -> "جارٍ التحقق..."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(48.dp))

            Box(contentAlignment = Alignment.Center) {
                if (actionState == ActionState.SENDING) {
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .scale(pulseScale)
                            .background(baseColor.copy(alpha = 0.25f), CircleShape)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .background(baseColor, CircleShape)
                        .clickableNoRipple {
                            if (actionState != ActionState.SENDING) {
                                val newVal = !isLocked
                                sendCommand(if (newVal) "lock" else "unlock", newVal)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = when (actionState) {
                                ActionState.SENDING -> "جارٍ الإرسال..."
                                else -> if (isLocked) "مقفولة" else "مفتوحة"
                            },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
            Text(
                text = "اضغط الزر لتبديل حالة القفل",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )

            errorText?.let {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = androidx.compose.foundation.clickable(
    indication = null,
    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
    onClick = onClick
)
