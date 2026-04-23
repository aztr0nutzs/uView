package com.sentinel.app.data.discovery.arp

import com.sentinel.app.domain.model.KNOWN_CAMERA_OUI_PREFIXES
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ArpTableScanner
 *
 * Reads the Linux ARP table at /proc/net/arp to instantly enumerate all
 * hosts that have recently communicated on the local network.
 *
 * This is dramatically faster than TCP port probing (milliseconds vs seconds)
 * because the kernel already knows which hosts are alive from normal network
 * traffic. No packets are sent.
 *
 * ARP table format (/proc/net/arp):
 *
 *   IP address       HW type  Flags    HW address          Mask  Device
 *   192.168.1.101    0x1      0x2      b8:a4:4f:12:34:56   *     wlan0
 *   192.168.1.1      0x1      0x2      a0:b1:c2:d3:e4:f5   *     wlan0
 *
 * Flags:
 *   0x2 = ATF_COM — completed entry (host responded to ARP)
 *   0x4 = ATF_PUBL — proxy ARP entry (skip these)
 *   0x6 = ATF_COM | ATF_PUBL
 *
 * Notes:
 *   - Only hosts that have recently communicated appear here.
 *   - New hosts will only appear after they send or receive a packet.
 *   - The ARP table is per-interface; we read all entries regardless of interface.
 *   - No permission is required to read /proc/net/arp on Android.
 */
@Singleton
class ArpTableScanner @Inject constructor() {

    data class ArpEntry(
        val ipAddress: String,
        val macAddress: String,
        val flags: Int,
        val device: String,
        val vendor: String?
    ) {
        val isComplete: Boolean get() = (flags and 0x2) != 0
        val isProxy: Boolean   get() = (flags and 0x4) != 0
    }

    /**
     * Read all valid (completed, non-proxy) ARP entries.
     * Returns an empty list if /proc/net/arp is not readable.
     */
    fun readArpTable(): List<ArpEntry> {
        return try {
            val file = File("/proc/net/arp")
            if (!file.exists() || !file.canRead()) {
                Timber.w("ArpTableScanner: /proc/net/arp not readable")
                return emptyList()
            }

            val lines = file.readLines()
            // Skip header line
            lines.drop(1)
                .mapNotNull { parseLine(it) }
                .filter { it.isComplete && !it.isProxy }
                .also { Timber.d("ArpTableScanner: found ${it.size} ARP entries") }
        } catch (e: Exception) {
            Timber.e(e, "ArpTableScanner: failed to read ARP table")
            emptyList()
        }
    }

    /**
     * Parse a single ARP table line into an [ArpEntry].
     * Returns null if the line is malformed.
     */
    private fun parseLine(line: String): ArpEntry? {
        return try {
            // Split on whitespace, collapse multiple spaces
            val parts = line.trim().split(Regex("\\s+"))
            if (parts.size < 6) return null

            val ip      = parts[0]
            val flagHex = parts[2]
            val mac     = parts[3].uppercase()
            val device  = parts[5]

            // Validate IP address format
            if (!ip.matches(Regex("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}"))) return null

            // Skip zero MACs (00:00:00:00:00:00) — incomplete entries
            if (mac == "00:00:00:00:00:00") return null

            val flags = flagHex.removePrefix("0x").toIntOrNull(16) ?: 0
            val vendor = lookupVendor(mac)

            ArpEntry(
                ipAddress  = ip,
                macAddress = mac,
                flags      = flags,
                device     = device,
                vendor     = vendor
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Look up MAC address vendor using the curated OUI prefix table.
     * Checks 3-byte prefix (AA:BB:CC) against known camera vendors.
     */
    fun lookupVendor(mac: String): String? {
        if (mac.length < 8) return null
        val prefix = mac.take(8).uppercase()
        return KNOWN_CAMERA_OUI_PREFIXES[prefix]
    }

    /**
     * Returns true if the MAC address belongs to a known camera manufacturer.
     */
    fun isKnownCameraMac(mac: String): Boolean =
        lookupVendor(mac) != null

    /**
     * Read only the IP addresses that appear in the ARP table.
     * Faster than [readArpTable] when vendor data is not needed.
     */
    fun readLiveHosts(): List<String> =
        readArpTable().map { it.ipAddress }
}
