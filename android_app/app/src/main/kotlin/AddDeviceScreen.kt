package com.example.carremote.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.carremote.data.CarDevice
import java.util.UUID
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeviceScreen(
    onBack: () -> Unit,
    onSave: (CarDevice) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var ip by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }

    val isValid = name.isNotBlank() && ip.isNotBlank() && secret.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إضافة سيارة / ريموت") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("اسم السيارة") },
                placeholder = { Text("مثال: سيارتي") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = ip,
                onValueChange = { ip = it },
                label = { Text("عنوان IP الخاص بـ ESP32") },
                placeholder = { Text("مثال: 192.168.1.50") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = secret,
                onValueChange = { secret = it },
                label = { Text("المفتاح السري المشترك") },
                placeholder = { Text("نفس القيمة المضبوطة في الفيرموير") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            Text(
                "ملاحظة: المفتاح السري يجب أن يطابق تمامًا القيمة الموجودة داخل كود الـ ESP32 (SHARED_SECRET).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(32.dp))
            Button(
                onClick = {
                    onSave(
                        CarDevice(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            ipAddress = ip,
                            sharedSecret = secret,
                            colorSeed = Random.nextLong(),
                            isLocked = true
                        )
                    )
                },
                enabled = isValid,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("حفظ السيارة")
            }
        }
    }
}
