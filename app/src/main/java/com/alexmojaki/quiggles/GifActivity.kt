package com.alexmojaki.quiggles

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import com.waynejo.androidndkgif.GifEncoder
import kotlinx.android.synthetic.main.activity_gif.*
import pl.droidsonroids.gif.GifDrawable
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import android.content.Intent
import android.net.Uri
import java.io.File


class GifActivity : CommonActivity() {

    override fun onCreate() {
        setContentView(R.layout.activity_gif)

        val fps = 24
        val delay = 1000 / fps
        clock = ControlledClock(delay)

        val scale = Math.min(0.5, 1000 / (gifDrawing!!.scenter.x * 2))
        val scenter = gifDrawing!!.scenter * scale
        val drawing = Drawing(scenter)

        val quiggles = gifDrawing!!.nonTransitioning(includeCompleting = true).second
        val duration = (quiggles.map { it.oscillationPeriod * 2 } +
                quiggles.map { it.huePeriod } +
                quiggles.map { it.rotationPeriod / it.numVertices })
            .map { it.absoluteValue }
            .filter { it.isFinite() }
            .max()!!
            .toNearest(delay / 1000.0)

        val hash = sha256(quiggles.map { jsonMapper.writeValueAsString(it) }.sorted().joinToString())
        val sharedPreferences = getPreferences(Context.MODE_PRIVATE)!!
        val cachedPath = sharedPreferences.getString(hash, null)
        if (cachedPath != null && File(cachedPath).exists()) {
            complete(cachedPath)
            return
        }

        drawing.quiggles.addAll(quiggles.map { it.copyForGif(scenter, duration, scale) })

        val (scaledWidth, scaledHeight) = (scenter * 2.0).toInt()

        val frames = (duration / delay * 1000).roundToInt()
        gifProgress.max = frames
        gifProgress.progress = 0

        prn("delay", delay)
        prn("duration", duration)
        prn("frames", frames)

        val gifEncoder = GifEncoder()
        val path = (picsDir() /
                "${gifDrawing!!.filename ?: "untitled"} ${isoFormat(currentTime())}.gif"
                    .replace(Regex("""["*/:<>?\\|]"""), "_")
                ).absolutePath

        gifEncoder.init(
            scaledWidth, scaledHeight,
            path, GifEncoder.EncodingType.ENCODING_TYPE_SIMPLE_FAST
        )

        val bitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        Thread {
            for (i in 1..frames) {
                drawing.draw(canvas)
                clock.tick()
                gifEncoder.encodeFrame(
                    bitmap,
                    delay
                )
                gifProgress.progress = i

                // Shows each frame in ImageView preview
//                    val copy = Bitmap.createBitmap(bitmap)
//                    runOnUiThread { gifPreview.setImageBitmap(copy) }
            }
            gifEncoder.close()
            with(sharedPreferences.edit()) {
                putString(hash, path)
                apply()
            }

            complete(path)

        }.start()
    }

    private fun complete(path: String) {
        val gifDrawable = GifDrawable(path)
        runOnUiThread {
            gifPreview.setImageDrawable(gifDrawable)
            gifPreview.visibility = VISIBLE
            gifProgress.visibility = INVISIBLE
            buttons.visibility = VISIBLE

            addButton(
                "Share",
                R.drawable.share_variant,
                buttonsLayout
            ) {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "image/gif"
                val uri = Uri.fromFile(File(path))
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                startActivity(Intent.createChooser(shareIntent, "Share GIF"))
            }
        }
    }

    override fun finish() {
        super.finish()
        clock = SystemClock()
    }

}

var gifDrawing: Drawing? = null
