package org.firstinspires.ftc.teamcode.opmode.vision

import com.acmerobotics.roadrunner.geometry.Vector2d
import org.opencv.core.MatOfPoint
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import kotlin.math.*

object VisionUtil {
    object Color {
        val RED = Scalar(255.0, 0.0, 0.0)
        val BLUE = Scalar(0.0, 255.0, 0.0)
        val GREEN = Scalar(0.0, 0.0, 255.0)
    }

    data class Extremes(val left: Point, val top: Point, val right: Point, val bottom: Point)
    fun findExtremes(contour: MatOfPoint) : Extremes {
        val list = contour.toList()
        assert(list.isNotEmpty())
        return Extremes(
            left = list.minByOrNull { point -> point.x }!!,
            top = list.minByOrNull { point -> point.y }!!,
            right = list.maxByOrNull { point -> point.x }!!,
            bottom = list.maxByOrNull { point -> point.y }!!,
        )
    }

    data class DualAngle(val alpha: Double, val beta: Double) {
        operator fun plus(that: DualAngle) = DualAngle(this.alpha + that.alpha, this.beta + that.beta)
    }
    fun getDualAngle(point: Point, focalLength: Double, width: Int, height: Int): DualAngle {
        val x =  point.x - width / 2.0
        val y = -point.y + height / 2.0

        val alpha = atan(x / focalLength)
        val beta = atan(y / focalLength)

        return DualAngle(alpha, beta)
    }

    fun pinToHorizontalPlane(point: Point, size: Size, focalLength: Double, camHeight: Double, camAngle: Double): Vector2d {
        // transformation so that origin is center of image and right handed, then flipped for pinhole
        val col = -point.x + size.width / 2.0
        val row =  point.y - size.height / 2.0

        val beta = atan(row / focalLength) - camAngle
        val x = camHeight / tan(beta)

        val rowFocalDist = sqrt(row*row + focalLength*focalLength)
        val zAxisToRow = rowFocalDist * cos(beta)
        // col/zAxisToRow = y/x
        val y = x * col/zAxisToRow

        return Vector2d(x, y)
    }

    fun pinToParallelPlane(point: Point, size: Size, focalLength: Double, distance: Double): Vector2d {
        // transformation so that origin is center of image and right handed, then flipped for pinhole
        val col = -point.x + size.width / 2.0
        val row =  point.y - size.height / 2.0

        val scalar = distance / focalLength
        val x = -col * scalar
        val y = -row * scalar

        return Vector2d(x, y)
    }

    fun alpha(point: Point, size: Size, focalLength: Double): Double {
        val col = -point.x + size.width / 2.0
        val alpha = atan(col / focalLength)
        return alpha
    }

    operator fun Point.plus(that: Point) = Point(this.x + that.x, this.y + that.y)
}