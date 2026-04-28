package com.sentinel.app.core.pairing

import java.security.KeyPair

data class PairingSession(
    val keyPair: KeyPair,
    val token: ByteArray,
    val host: String,
    val port: Int,
    val expiryMs: Long,
    val deviceName: String,
) {
    val qrPayload: String by lazy {
        PairingProtocol.encodeQrPayload(
            host = host,
            port = port,
            serverPubKey = PairingProtocol.encodePublicKey(keyPair.public),
            token = token,
            expiryMs = expiryMs,
        )
    }

    /** 6-character fallback code derived from the token — for environments where the QR cannot be scanned. */
    val fallbackCode: String by lazy {
        val n = ((token[0].toInt() and 0xff) shl 16) or
                ((token[1].toInt() and 0xff) shl 8) or
                (token[2].toInt() and 0xff)
        "%06d".format(n % 1_000_000)
    }
}
