package com.triophore.weaver

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * An interface defining methods for controlling a local hotspot on Android devices.
 *
 * Provides methods to start, stop, and check the state of a local hotspot.
 *
 * @see WeaverImpl
 */
interface Weaver {

    /**
     * Starts a local hotspot with the specified configuration.
     *
     * @param ssid The SSID (name) of the hotspot.
     * @param password The password for the hotspot.
     * @param securityType The security type (e.g., SECURITY_TYPE_WPA2_PSK).
     * @param hotspotChannel The desired channel for the hotspot (optional).
     * @param band The band for the hotspot (defaults to BAND_2GHZ).
     * @param autoShutdownEnabled Whether to enable auto-shutdown for the hotspot (defaults to false).
     * @param macRandomizationSetting The MAC randomization setting for the hotspot (defaults to RANDOMIZATION_PERSISTENT).
     */
    fun startTethering(
        ssid: String,
        password: String,
        securityType: Int = SECURITY_TYPE_WPA2_PSK,
        hotspotChannel: Int? = null,
        band: Int = BAND_2GHZ,
        autoShutdownEnabled: Boolean = false,
        macRandomizationSetting: Int = RANDOMIZATION_PERSISTENT
    )

    /**
     * Stops the currently active local hotspot.
     */
    fun stopTethering()

    /**
     * Gets the current state of the local hotspot.
     *
     * @return True if the hotspot is active, false otherwise.
     */
    fun getTetherState(): Boolean

    /**
     * A state flow indicating the current state of the local hotspot (active/inactive).
     */
    val tetherState: MutableStateFlow<Boolean>

    companion object {

        const val BAND_2GHZ = 1.shl(0)
        const val BAND_5GHZ = 1.shl(1)
        const val BAND_6GHZ = 1.shl(2)
        const val BAND_60GHZ = 1.shl(3)
        const val BAND_ANY = BAND_2GHZ.or(BAND_5GHZ).or(BAND_6GHZ).or(BAND_60GHZ)
        const val RANDOMIZATION_NONE = 0
        const val RANDOMIZATION_PERSISTENT = 1
        const val RANDOMIZATION_NON_PERSISTENT = 2
        const val SECURITY_TYPE_OPEN = 0
        const val SECURITY_TYPE_WPA2_PSK = 1
        const val SECURITY_TYPE_WPA3_SAE_TRANSITION = 2
        const val SECURITY_TYPE_WPA3_SAE = 3
        const val SECURITY_TYPE_WPA3_OWE_TRANSITION = 4
        const val SECURITY_TYPE_WPA3_OWE = 5

        /**
         * Creates a new instance of the `WeaverImpl` class for managing local hotspots.
         *
         * @param context The application context.
         * @return A new `WeaverImpl` instance.
         */
        @RequiresPermission(Manifest.permission.NEARBY_WIFI_DEVICES)
        fun create(context: Context): Weaver {
            return WeaverImpl(context)
        }
    }
}