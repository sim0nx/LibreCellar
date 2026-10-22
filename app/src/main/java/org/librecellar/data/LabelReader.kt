package org.librecellar.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.librecellar.data.model.LabelLine
import org.librecellar.data.model.LabelSuggestions
import org.librecellar.data.model.LabelTextParser
import org.librecellar.ui.components.WineImageHelper
import kotlin.coroutines.resume

/**
 * Reads the text on wine label pictures with ML Kit's bundled text recognition model, entirely on
 * the device (no picture leaves the phone for this), and turns it into suggestions.
 */
object LabelReader {

    private const val TAG = "LabelReader"

    /** [front] and [back] are picture filenames as kept by the app (see [WineImageHelper]). */
    suspend fun read(context: Context, front: String?, back: String?): LabelSuggestions =
        withContext(Dispatchers.Default) {
            val lines = recognise(context, front, isBack = false) + recognise(context, back, isBack = true)
            LabelTextParser.parse(lines)
        }

    private suspend fun recognise(context: Context, picture: String?, isBack: Boolean): List<LabelLine> {
        val file = WineImageHelper.getCachedImageFile(context, picture) ?: return emptyList()
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            // Reads the EXIF orientation, so upright text is found in portrait photos too
            val image = InputImage.fromFilePath(context, Uri.fromFile(file))
            suspendCancellableCoroutine { cont ->
                recognizer.process(image)
                    .addOnSuccessListener { text ->
                        val lines = text.textBlocks.flatMap { it.lines }.map { line ->
                            LabelLine(
                                text = line.text,
                                height = line.boundingBox?.height()?.toFloat() ?: 0f,
                                isBackLabel = isBack,
                                top = line.boundingBox?.top?.toFloat() ?: 0f
                            )
                        }
                        cont.resume(lines)
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Text recognition failed", e)
                        cont.resume(emptyList())
                    }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not read $picture", e)
            emptyList()
        } finally {
            recognizer.close()
        }
    }
}
