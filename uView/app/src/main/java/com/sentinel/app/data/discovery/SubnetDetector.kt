package com.sentinel.app.data.discovery

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SubnetDetector
 *
 * Derives the local WiFi subnet from [WifiManager] and [ConnectivityManager].
 *
 * On Android 10+ WifiManager.connectionInfo is deprecated for location
 * purposes, but reading the IP address for LAN scanning (not location) is
 * still permitted without location permission.
 *
 * Returns the subnet prefix as a String like "192.168.1" — suitable for
 * constructing host addresses by appending ".1" through ".254".
 */
@Singleton
class SubnetDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Returns the /24 subnet prefix of the current WiFi connection,
     * e.g. "192.168.1" or "10.0.0".
     * Returns null if not connected to WiFi or if IP cannot be resolved.
     */
    fun detectSubnet(): String? {
        return try {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                ?: return null

            @Suppress("DEPRECATION")
            val dhcpInfo = wifiManager.dhcpInfo ?: return null
            val ipInt = dhcpInfo.ipAddress

            if (ipInt == 0) return null

            // WifiManager returns IP in little-endian order on little-endian CPU
            val ip = String.format(
                "%d.%d.%d.%d",
                ipInt and 0xFF,
                (ipInt shr 8) and 0xFF,
                (ipInt shr 16) and 0xFF,
                (ipInt shr 24) and 0xFF
            )

            Timber.d("SubnetDetector: local IP = $ip")

            // Return /24 prefix (first three octets)
            ip.substringBeforeLast(".")
        } catch (e: Exception) {
            Timber.e(e, "SubnetDetector: failed to detect subnet")
            null
        }
    }

    /**
     * Returns the local device IP address (all four octets).
     */
    fun detectLocalIp(): String? {
        return try {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                ?: return null
            @Suppress("DEPRECATION")
            val ipInt = wifiManager.dhcpInfo?.ipAddress ?: return null
            if (ipInt == 0) return null
            String.format(
                "%d.%d.%d.%d",
                ipInt and 0xFF,
                (ipInt shr 8) and 0xFF,
                (ipInt shr 16) and 0xFF,
                (ipInt shr 24) and 0xFF
            )
        } catch (e: Exception) {
            Timber.e(e, "SubnetDetector: failed to detect local IP")
            null
        }
    }

    /**
     * Returns true if the device is currently connected to a WiFi network.
     */
    fun isWifiConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    /**
     * Returns the gateway IP address (router) on the current network.
     */
    fun detectGateway(): String? {
        return try {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                ?: return null
            @Suppress("DEPRECATION")
            val gatewayInt = wifiManager.dhcpInfo?.gateway ?: return null
            if (gatewayInt == 0) return null
            String.format(
                "%d.%d.%d.%d",
                gatewayInt and 0xFF,
                (gatewayInt shr 8) and 0xFF,
                (gatewayInt shr 16) and 0xFF,
                (gatewayInt shr 24) and 0xFF
            )
        } catch (e: Exception) {
            null
        }
    }
}
