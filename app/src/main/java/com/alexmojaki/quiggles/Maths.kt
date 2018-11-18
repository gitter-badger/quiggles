package com.alexmojaki.quiggles

import java.util.*
import kotlin.math.PI

val angleToPoints = TreeMap<Double, Int>().apply {
    for (points in 3..9) {
        for (revolutions in 1 until points) {
            if (gcd(points, revolutions) == 1) {
                val angle = 2 * PI * revolutions / points
                this[angle] = points
            }
        }
    }
}

fun star(angle: Double): Pair<Double, Int> {
    val bestAngle = if (angle in angleToPoints) {
        angle
    } else {
        val lo = angleToPoints.lowerKey(angle)
        val hi = angleToPoints.higherKey(angle)
        listOf(lo, hi).minBy { Math.abs(angle - (it ?: 1000.0)) }!!
    }
    return Pair(bestAngle, angleToPoints[bestAngle]!!)
}

tailrec fun gcd(a: Int, b: Int): Int =
    if (b == 0) a else gcd(b, a % b)

val rand = Random()

fun randRange(min: Float, max: Float) = min + rand.nextFloat() * (max - min)
