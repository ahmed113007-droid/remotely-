package com.example.carremote.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "car_devices_store")

class DeviceStore(private val context: Context) {

    private val DEVICES_KEY = stringPreferencesKey("devices_json")
    private val json = Json { ignoreUnknownKeys = true }

    val devicesFlow: Flow<List<CarDevice>> = context.dataStore.data.map { prefs ->
        val raw = prefs[DEVICES_KEY] ?: "[]"
        runCatching { json.decodeFromString<List<CarDevice>>(raw) }.getOrDefault(emptyList())
    }

    suspend fun saveDevices(devices: List<CarDevice>) {
        context.dataStore.edit { prefs ->
            prefs[DEVICES_KEY] = json.encodeToString(devices)
        }
    }

    suspend fun addDevice(device: CarDevice) {
        context.dataStore.edit { prefs ->
            val raw = prefs[DEVICES_KEY] ?: "[]"
            val current = runCatching { json.decodeFromString<List<CarDevice>>(raw) }.getOrDefault(emptyList())
            prefs[DEVICES_KEY] = json.encodeToString(current + device)
        }
    }

    suspend fun removeDevice(id: String) {
        context.dataStore.edit { prefs ->
            val raw = prefs[DEVICES_KEY] ?: "[]"
            val current = runCatching { json.decodeFromString<List<CarDevice>>(raw) }.getOrDefault(emptyList())
            prefs[DEVICES_KEY] = json.encodeToString(current.filterNot { it.id == id })
        }
    }

    suspend fun updateDevice(updated: CarDevice) {
        context.dataStore.edit { prefs ->
            val raw = prefs[DEVICES_KEY] ?: "[]"
            val current = runCatching { json.decodeFromString<List<CarDevice>>(raw) }.getOrDefault(emptyList())
            prefs[DEVICES_KEY] = json.encodeToString(current.map { if (it.id == updated.id) updated else it })
        }
    }
}
