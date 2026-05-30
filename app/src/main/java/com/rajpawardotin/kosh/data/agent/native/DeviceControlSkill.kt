package com.rajpawardotin.kosh.data.agent.native

import android.content.Context
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import com.rajpawardotin.kosh.domain.agent.Tool
import com.rajpawardotin.kosh.domain.agent.ToolParam

class DeviceControlSkill(private val context: Context) {

    @Tool(name = "toggle_flashlight", description = "Turns the device flashlight on or off")
    fun toggleFlashlight(
        @ToolParam(name = "enable", description = "Set to true to turn flashlight on, false to turn it off") enable: Boolean
    ): String {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                ?: return "Error: Camera Service not available"
            val cameraId = cameraManager.cameraIdList.firstOrNull()
                ?: return "Error: No camera/flash detected on this device"
            cameraManager.setTorchMode(cameraId, enable)
            "Flashlight successfully toggled to ${if (enable) "ON" else "OFF"}"
        } catch (e: Exception) {
            "Error toggling flashlight: ${e.message}"
        }
    }

    @Tool(name = "set_volume", description = "Adjusts the system media/music volume percentage")
    fun setVolume(
        @ToolParam(name = "percentage", description = "The volume percentage, integer from 0 to 100") percentage: Int
    ): String {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                ?: return "Error: Audio Service not available"
            
            val streamType = AudioManager.STREAM_MUSIC
            val maxVolume = audioManager.getStreamMaxVolume(streamType)
            val targetVolume = ((percentage.coerceIn(0, 100) / 100.0) * maxVolume).toInt()
            
            audioManager.setStreamVolume(streamType, targetVolume, AudioManager.FLAG_SHOW_UI)
            "Media volume set to $percentage% ($targetVolume/$maxVolume)"
        } catch (e: Exception) {
            "Error setting volume: ${e.message}"
        }
    }
}
