package com.elmotamyez.gallery.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

private fun compressImage(bytes: ByteArray, maxDimension: Int = 1200, quality: Float = 0.8f): ByteArray {
    val original = ImageIO.read(ByteArrayInputStream(bytes)) ?: return bytes
    val scale = minOf(1.0, maxDimension.toDouble() / maxOf(original.width, original.height))
    val source = if (scale < 1.0) {
        val newW = (original.width * scale).toInt()
        val newH = (original.height * scale).toInt()
        val scaled = BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB)
        val g = scaled.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g.drawImage(original, 0, 0, newW, newH, null)
        g.dispose()
        scaled
    } else {
        if (original.type == BufferedImage.TYPE_INT_RGB) original
        else {
            val rgb = BufferedImage(original.width, original.height, BufferedImage.TYPE_INT_RGB)
            val g = rgb.createGraphics()
            g.drawImage(original, 0, 0, null)
            g.dispose()
            rgb
        }
    }
    val out = ByteArrayOutputStream()
    val writer = ImageIO.getImageWritersByFormatName("jpeg").next()
    val params = writer.defaultWriteParam
    params.compressionMode = ImageWriteParam.MODE_EXPLICIT
    params.compressionQuality = quality
    val ios = ImageIO.createImageOutputStream(out)
    writer.output = ios
    writer.write(null, IIOImage(source, null, null), params)
    writer.dispose()
    ios.close()
    return out.toByteArray()
}

actual fun rotateLandscapeToPortrait(bytes: ByteArray): ByteArray {
    val original = ImageIO.read(ByteArrayInputStream(bytes)) ?: return bytes
    if (original.width <= original.height) return bytes
    return rotate90CW(original, bytes)
}

actual fun rotateImage90CW(bytes: ByteArray): ByteArray {
    val original = ImageIO.read(ByteArrayInputStream(bytes)) ?: return bytes
    return rotate90CW(original, bytes)
}

private fun rotate90CW(original: BufferedImage, @Suppress("UNUSED_PARAMETER") fallback: ByteArray): ByteArray {
    val rotated = BufferedImage(original.height, original.width, BufferedImage.TYPE_INT_RGB)
    val g = rotated.createGraphics()
    g.translate(original.height.toDouble(), 0.0)
    g.rotate(Math.PI / 2)
    g.drawImage(original, 0, 0, null)
    g.dispose()
    val out = ByteArrayOutputStream()
    val writer = ImageIO.getImageWritersByFormatName("jpeg").next()
    val params = writer.defaultWriteParam
    params.compressionMode = ImageWriteParam.MODE_EXPLICIT
    params.compressionQuality = 0.85f
    val ios = ImageIO.createImageOutputStream(out)
    writer.output = ios
    writer.write(null, IIOImage(rotated, null, null), params)
    writer.dispose()
    ios.close()
    return out.toByteArray()
}

@Composable
actual fun rememberImagePickerLauncher(onImagePicked: (ByteArray) -> Unit): () -> Unit {
    val scope = rememberCoroutineScope()
    return {
        scope.launch(Dispatchers.IO) {
            val chooser = JFileChooser()
            chooser.fileFilter = FileNameExtensionFilter("Images", "jpg", "jpeg", "png", "webp")
            chooser.isMultiSelectionEnabled = true
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                chooser.selectedFiles.forEach { file -> onImagePicked(compressImage(file.readBytes())) }
            }
        }
    }
}
