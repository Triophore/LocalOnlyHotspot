package com.triophore.weaver


import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.SoftApConfiguration
import android.net.wifi.WifiManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.DefaultLifecycleObserver
import com.triophore.weaver.Weaver.Companion.BAND_2GHZ
import com.triophore.weaver.Weaver.Companion.RANDOMIZATION_PERSISTENT
import com.triophore.weaver.Weaver.Companion.SECURITY_TYPE_WPA2_PSK
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executor


/**
 * Implementation class for the `Weaver` interface, providing methods to manage local hotspots
 * on Android devices running API level 33 or higher. Requires the `NEARBY_WIFI_DEVICES` permission.
 *
 * @param context The application context.
 */
@RequiresApi(33)
class WeaverImpl(private val context: Context) : Weaver, DefaultLifecycleObserver {

    companion object {
        private const val TAG = "Weaver"
        private const val WIFI_AP_STATE_ENABLED = 13
        private const val WIFI_AP_STATE_ENABLING = 12
        private const val WIFI_AP_STATE_DISABLED = 11
    }

    /**
     * A state flow indicating the current state of the local hotspot (active/inactive).
     */
    override val tetherState: MutableStateFlow<Boolean> = MutableStateFlow(false)

    /**
     * Internal state flow holding a reference to the current WifiManager.LocalOnlyHotspotReservation,
     * if any.
     */
    private val reservation: MutableStateFlow<WifiManager.LocalOnlyHotspotReservation?> =
        MutableStateFlow(null)

    /**
     * A coroutine scope used for internal asynchronous operations.
     */
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)

    init {
        scope.launch {
            reservation.collect {
                tetherState.value = it != null
            }
        }
    }

    /**
     * Internal reference to the class object  for android.net.wifi.SoftApConfiguration\$Builder
     * used for reflection.
     */
    @SuppressLint("PrivateApi")
    private val builderClass = Class.forName("android.net.wifi.SoftApConfiguration\$Builder")
    /**
     * Internal instance of the SoftApConfiguration\$Builder class created using reflection.
     */
    private val builderInstance = builderClass.newInstance()

    private fun setBand(band: Int): WeaverImpl {
        builderClass.getDeclaredMethod("setBand", Int::class.javaPrimitiveType)
            .invoke(builderInstance, band)
        return this
    }

    private fun setAutoShutdownEnabled(enabled: Boolean): WeaverImpl {
        builderClass.getMethod("setAutoShutdownEnabled", Boolean::class.javaPrimitiveType).invoke(
            builderInstance, enabled
        )
        return this
    }

    private fun setSsid(ssid: String): WeaverImpl {
        builderClass.getMethod("setSsid", String::class.java).invoke(
            builderInstance, ssid
        )
        return this
    }

    private fun setPassphrase(
        passphrase: String,
        securityType: Int
    ): WeaverImpl {
        builderClass.getMethod(
            "setPassphrase", String::class.java, Int::class.javaPrimitiveType
        ).invoke(
            builderInstance, passphrase, securityType
        )

        return this
    }

    private fun setMacRandomizationSetting(randomizationSetting: Int): WeaverImpl {
        builderClass.getMethod("setMacRandomizationSetting", Int::class.java).invoke(
            builderInstance, randomizationSetting
        )
        return this
    }

    private fun setChannel(channel: Int, band: Int = BAND_2GHZ): WeaverImpl {
        builderClass.getDeclaredMethod(
            "setChannel",
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType
        ).invoke(builderInstance, channel, band)
        return this
    }

    private fun build(): SoftApConfiguration {
        return builderClass
            .getMethod("build")
            .invoke(builderInstance) as SoftApConfiguration
    }

    /**
     * Creates a `SoftApConfiguration` object with the provided configuration options.
     * Uses reflection for setting options on the builder.
     *
     * @param ssid The SSID for the hotspot.
     * @param password The password for the hotspot.
     * @param securityType The security type (e.g., WPA2_PSK). Defaults to SECURITY_TYPE_WPA2_PSK.
     * @param hotspotChannel The desired channel for the hotspot (optional).
     * @param band The band for the hotspot (defaults to BAND_2GHZ).
     * @param autoShutdownEnabled Enables or disables auto-shutdown (defaults to false).
     * @param macRandomizationSetting Configures MAC randomization behavior (defaults to RANDOMIZATION_PERSISTENT).
     * @return A SoftApConfiguration object with the specified settings, or null if reflection fails.
     */
    private fun softApConfiguration(
        ssid: String,
        password: String,
        securityType: Int = SECURITY_TYPE_WPA2_PSK,
        hotspotChannel: Int? = null,
        band: Int = BAND_2GHZ,
        autoShutdownEnabled: Boolean = false,
        macRandomizationSetting: Int = RANDOMIZATION_PERSISTENT
    ): SoftApConfiguration? {
        try {
            setSsid(ssid)
            setPassphrase(password, securityType)
            hotspotChannel?.let { channel ->
                setChannel(
                    channel,
                    band
                )
            } ?: run {
                setBand(band)
            }
            setAutoShutdownEnabled(autoShutdownEnabled)
            setMacRandomizationSetting(macRandomizationSetting)
            return build()
        } catch (e: Exception) {
            Log.e(TAG, "Reflection failed for SoftApConfiguration\$Builder")
            return null
        }
    }

    /**
     * Starts the local hotspot using the provided SoftApConfiguration and callback.
     *
     * @param config The SoftApConfiguration object containing hotspot settings.
     * @param executor The executor used for asynchronous operations (optional).
     * @param callback The callback to receive notifications about hotspot events.
     */
    private fun startLocalOnlyHotspotWithConfig(
        config: SoftApConfiguration,
        executor: Executor?,
        callback: WifiManager.LocalOnlyHotspotCallback
    ) {
        val mWifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        WifiManager::class.java.getMethod(
            "startLocalOnlyHotspot", SoftApConfiguration::class.java, Executor::class.java,
            WifiManager.LocalOnlyHotspotCallback::class.java,
        ).invoke(mWifiManager, config, executor, callback)
    }

    /**
     * Starts a local hotspot with the specified configuration.
     *
     * If a hotspot is already active, it will be stopped first.
     *
     * @param ssid The SSID (name) for the hotspot.
     * @param password The password for the hotspot.
     * @param securityType The security type (e.g., WPA2_PSK).
     * @param hotspotChannel The desired channel for the hotspot (optional).
     * @param band The band for the hotspot (defaults to BAND_2GHZ).
     * @param autoShutdownEnabled Enables or disables auto-shutdown for the hotspot (defaults to false).
     * @param macRandomizationSetting Configures MAC randomization behavior (defaults to RANDOMIZATION_PERSISTENT).
     */
    override fun startTethering(
        ssid: String,
        password: String,
        securityType: Int,
        hotspotChannel: Int?,
        band: Int,
        autoShutdownEnabled: Boolean,
        macRandomizationSetting: Int
    ) {
        if (tetherState.value)
            stopTethering()
        startLocalOnlyHotspotWithConfig(
            softApConfiguration(ssid, password)!!,
            null,
            object : WifiManager.LocalOnlyHotspotCallback() {
                override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation?) {
                    super.onStarted(reservation)
                    this@WeaverImpl.reservation.value = reservation
                    Log.d(TAG, "LocalOnlyHotspot created with ssid: $ssid and password: $password.")
                }

                override fun onStopped() {
                    super.onStopped()
                    Log.d(TAG, "LocalOnlyHotspot stopped.")
                    reservation.value = null
                }

                override fun onFailed(reason: Int) {
                    super.onFailed(reason)
                    Log.d(TAG, "LocalOnlyHotspot failed to start.")
                }
            }
        )
    }

    /**
     * Stops the currently active local hotspot, if any.
     */
    override fun stopTethering() {
        reservation.value?.let {
            it.close()
            Log.d(TAG, "LocalOnlyHotspot stopped")
            reservation.value = null
        } ?: {
            Log.d(TAG, "LocalOnlyHotspot not yet started")
        }
    }


    override fun getTetherState(): Boolean {
        return tetherState.value
    }
}