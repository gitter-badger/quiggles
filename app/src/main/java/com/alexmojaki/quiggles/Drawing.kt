package com.alexmojaki.quiggles

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.util.DisplayMetrics
import kotlin.math.PI
import kotlin.math.min

class Drawing {

    val quiggles = ArrayList<Quiggle>()
    lateinit var metrics: DisplayMetrics


    fun draw(canvas: Canvas) {
        canvas.drawColor(DEFAULT_BG_COLOR)

        val centers = ArrayList<Point>()
        val o = Point(0, 0)
        centers.add(o)
        for (i in 0 until quiggles.size - 1) {
            centers.add(o.pointInDirection(PI / 2 + i * PI / 3, 2.0))
        }
        val minx = centers.asSequence().map { it.x }.min()!! - 1
        val maxx = centers.asSequence().map { it.x }.max()!! + 1
        val miny = centers.asSequence().map { it.y }.min()!! - 1
        val maxy = centers.asSequence().map { it.y }.max()!! + 1

        val boxCenter = Point((minx + maxx) / 2, (miny + maxy) / 2)
        val width = maxx - minx
        val height = maxy - miny

        val swidth = metrics.widthPixels
        val sheight = metrics.heightPixels
        val scenter = Point(swidth / 2f, sheight / 2f)
        val scale = min(
            swidth / width,
            sheight / height
        ).toFloat()

        for ((c, quiggle) in centers.zip(quiggles)) {
            val matrix = Matrix()
            val tc = (scenter - boxCenter).toFloat()
            matrix.postTranslate(tc.x, tc.y)
            matrix.postScale(scale, scale, scenter.xf, scenter.yf)

            canvas.save()
            quiggle.draw(canvas, scale, matrix.transform(c.toFloat()).toDouble())
            canvas.restore()
        }

    }

    fun touchStart(x: Float, y: Float) {
        val quiggle = Quiggle()
        quiggle.start(x, y)
        quiggles.add(quiggle)
    }

    fun touchMove(x: Float, y: Float) {
        quiggles.last().addPoint(x, y)
    }

    fun touchUp() {
        val quiggle = quiggles.last()
        if (quiggle.points.size < 5) {
            quiggles.remove(quiggle)
        } else {
            quiggle.finishDrawing()
        }
    }

    fun update() {
        for (quiggle in quiggles) {
            quiggle.update()
        }
    }

    companion object {
        const val DEFAULT_BG_COLOR = Color.BLACK
        const val TOUCH_TOLERANCE = 8f
    }
}