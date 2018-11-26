package com.alexmojaki.quiggles

import android.graphics.*
import java.util.*
import kotlin.math.PI

class Quiggle {
    enum class State { Drawing, Completing, Complete }

    var state = State.Drawing
    val points: MutableList<Point> = ArrayList()
    var numPaths = 0
    var fullPath = Path()
    var partialPath = Path()
    var index = 0
    var idealAngle = 0.0
    var numVertices = -1
    var drawnTime: Long = 0
    var rotationPeriod = randRange(5f, 20f)
    val randomScaleFactor = randRange(0.85f, 1f)
    var outerRadius: Double = 0.0
    var innerRadius: Double = 0.0
    val hue = randRange(0f, 360f)

    fun start(point: Point) {
        points.add(point)
        fullPath.moveTo(point.xf, point.yf)
    }

    fun addPoint(point: Point) {
        require(state == State.Drawing)
        val p = points.last()
        if (point.distance(p) >= Drawing.TOUCH_TOLERANCE) {
            val mid = (p + point) / 2.0
            fullPath.quadTo(p.xf, p.yf, mid.xf, mid.yf)
            points.add(point)
        }
    }

    fun finishDrawing() {
        val p = points.last().toFloat()
        fullPath.lineTo(p.x, p.y)
        state = State.Completing
        numPaths--
        update()

        val angle = Math.abs(points[points.size - 2].direction(points.last()) - points[0].direction(points[1]))
        val (idealAngle, numVertices) = star(angle)
        this.idealAngle = idealAngle
        this.numVertices = numVertices

        drawnTime = System.currentTimeMillis()

        val center = center()
        val distances = points.asSequence().map { it.distance(center) }
        outerRadius = distances.max()!!
        innerRadius = distances.min()!!
    }

    fun center(): Point {
        val vertices = vertices()

        return Point(
            vertices.asSequence().map { it.x }.average(),
            vertices.asSequence().map { it.y }.average()
        )
    }

    private fun vertices(): ArrayList<Point> {
        val vertices = ArrayList(listOf(points[0]))
        val dist = points[0].distance(points.last())
        val angle = points[0].direction(points.last())
        for (i in 1 until numVertices) {
            vertices.add(vertices.last().pointInDirection((i - 1) * idealAngle + angle, dist))
        }
        return vertices
    }

    fun draw(
        canvas: Canvas,
        circleCenter: Point,
        screenHeight: Int,
        scale: Float = 1f,
        brightness: Float = 1f
    ) {
        canvas.save()
        val paint = Paint()

        with(paint) {
            isAntiAlias = true
            isDither = true
            color = Color.HSVToColor(floatArrayOf(hue, 1f, brightness))
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 4f
            xfermode = null
            alpha = 0xff
        }

        val matrix = Matrix()

        if (state != Quiggle.State.Drawing) {
            val center = center()
            val elapsed = (System.currentTimeMillis() - drawnTime) / 1000.0
            val rotations = s2Line(elapsed / rotationPeriod)
            val transition = s2(elapsed / 3.0)

            val translationPoint = (circleCenter - center) * transition
            translationPoint.translate(canvas)

            val factor: Float
            factor = if (scale == 1f) {
                (if (screenHeight / 2 < outerRadius)
                    (1 - transition * (1 - randomScaleFactor * screenHeight / 2 / outerRadius)).toFloat()
                else 1f)
            } else {
                (1 - transition * (1 - scale / outerRadius)).toFloat()
            }

//            (circleCenter - translationPoint).scale(matrix, factor)
            center.scale(matrix, factor)

            center().rotate(canvas, rotations * 2 * PI)
        }

        val p1 = points.first()
        val p2 = points.last()
        for (i in 0..numPaths) {
            canvas.drawPath(matrix * fullPath, paint)
            (p2 - p1).translate(canvas)
            p1.rotate(canvas, idealAngle)
        }
        canvas.drawPath(matrix * partialPath, paint)
        canvas.restore()
    }

    fun update() {
        if (state != State.Completing) return
        val p = points[index].toFloat()
        if (index == 0) {
            numPaths++
            partialPath.reset()
            partialPath.moveTo(p.x, p.y)
            if (numPaths == numVertices) {
                state = State.Complete
            }
        } else {
            partialPath.lineTo(p.x, p.y)
        }
        index = (index + 1) % points.size
    }
}