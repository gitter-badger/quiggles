package com.alexmojaki.quiggles

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import com.waynejo.androidndkgif.GifEncoder
import kotlinx.android.synthetic.main.activity_gif.*
import pl.droidsonroids.gif.GifDrawable
import kotlin.math.absoluteValue
import kotlin.math.roundToInt


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
                quiggles.map { it.rotationPeriod.absoluteValue / it.numVertices })
            .filter { it.isFinite() }
            .max()!!
            .toNearest(delay / 1000.0)

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

            val gifDrawable = GifDrawable(path)
            runOnUiThread {
                gifPreview.setImageDrawable(gifDrawable)
                gifPreview.visibility = VISIBLE
                gifProgress.visibility = INVISIBLE
            }

        }.start()
    }

    override fun finish() {
        super.finish()
        clock = SystemClock()
    }

}

var gifDrawing: Drawing? = null
