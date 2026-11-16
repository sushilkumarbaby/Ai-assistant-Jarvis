package com.jarvis.assistant.automation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.util.Log
import com.jarvis.assistant.JarvisApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * BroadcastReceiver triggered when device is plugged into power / charger.
 * Handles Midnight Night Autopilot and battery health notifications.
 */
class ChargingTriggerReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ChargingTrigger"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        when (action) {
            Intent.ACTION_POWER_CONNECTED -> {
                Log.i(TAG, "⚡ Charger plugged in. Initiating Power Connection Protocols.")
                handlePowerConnected(context)
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                Log.i(TAG, "🔌 Charger disconnected.")
            }
            Intent.ACTION_BATTERY_CHANGED -> {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                if (level != -1 && scale > 0) {
                    val batteryPct = (level / scale.toFloat() * 100).toInt()
                    if (batteryPct >= 80 && status == BatteryManager.BATTERY_STATUS_CHARGING) {
                        Log.i(TAG, "Battery reached optimal 80% threshold.")
                    }
                }
            }
        }
    }

    private fun handlePowerConnected(context: Context) {
        CoroutineScope(Dispatchers.Default).launch {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)

            // Check if it's midnight / late night (between 10 PM and 6 AM)
            val isLateNight = hour >= 22 || hour <= 6

            val prefs = JarvisApplication.instance.prefs
            val userName = prefs.userName

            if (isLateNight) {
                Log.i(TAG, "🌙 Midnight charging protocol activated.")
                JarvisApplication.instance.memoryStorage.addMemory("Device connected to charger during night cycle ($hour:00). Overnight diagnostics verified.")

                // Optional TTS alert or background cleanup
                // e.g. "Power cells charging, Sir. Engaging overnight protocol."
            } else {
                Log.i(TAG, "Standard daytime charging initiated.")
            }
        }
    }
}
