package com.sentinel.companion.data.pairing

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * CameraX [ImageAnalysis.Analyzer] that decodes QR codes from YUV preview frames.
 * Calls [onResult] at most once — flips an internal latch so subsequent frames are ignored.
 */
class QrAnalyzer(private val onResult: (String) -> Unit) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader().apply {
        setHints(mapOf(DecodeHintType.TRY_HARDER to true))
    }
    private val handled = AtomicBoolean(false)

    override fun analyze(image: ImageProxy) {
        if (handled.get()) { image.close(); return }
        try {
            val plane = image.planes[0]
            val buffer = plane.buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            val source = PlanarYUVLuminanceSource(
                bytes,
                plane.rowStride,
                image.height,
                0, 0,
                image.width,
                image.height,
                false,
            )
            val bitmap = BinaryBitmap(HybridBinarizer(source))
            val result = reader.decodeWithState(bitmap)
            if (handled.compareAndSet(false, true)) onResult(result.text)
        } catch (_: NotFoundException) {
            // No QR in this frame — try the next one.
        } catch (_: Exception) {
            // Decoder errors are non-fatal; keep scanning.
        } finally {
            reader.reset()
            image.close()
        }
    }
}
