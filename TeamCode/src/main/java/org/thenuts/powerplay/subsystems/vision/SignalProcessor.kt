package org.firstinspires.ftc.teamcode.opmode.vision

import com.acmerobotics.dashboard.config.Config
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.thenuts.powerplay.game.Signal
import org.thenuts.powerplay.game.Alliance


@Config
class SignalProcessor(val cam: Camera, alliance: Alliance) {
    private val pipeline = SignalPipeline(alliance)
    val finalTarget get() = pipeline.finalSignal

    fun enable() {
        cam.addPipeline(pipeline)
    }

    fun disable() {
        cam.removePipeline(pipeline)
    }

    class SignalPipeline(val alliance: Alliance) : Pipeline {
        override val input: View = View("Signal - input", VisionUtil.Color.RED)
        val leftMask: View = View("Signal - Left Mask", VisionUtil.Color.RED)
        val rightMask: View = View("Signal - Right Mask", VisionUtil.Color.RED)
        private val signalWindow: MutableList<Signal> = MutableList(10) { Signal.MID }

        val REGION_X get() = if (alliance == Alliance.RED) RED_REGION_X else BLUE_REGION_X
        val REGION_Y get() = if (alliance == Alliance.RED) RED_REGION_Y else BLUE_REGION_Y

        val topLeft get() = Point(
            REGION_X.toDouble(),
            REGION_Y.toDouble()
        )

        val bottomRight get() = Point(
            (REGION_X + REGION_WIDTH).toDouble(),
            (REGION_Y + REGION_HEIGHT).toDouble()
        )

        val rect: Rect get() = Rect(topLeft, bottomRight)

        var finalSignal: Signal = Signal.RIGHT

        override fun initialize(cam: Camera) {
            cam.addView(input)
            cam.addView(leftMask)
            cam.addView(rightMask)
        }

        override fun processFrame(cam: Camera) {
            fun findColor(low: Scalar, high: Scalar, mask: Mat): Double {
                Imgproc.cvtColor(input, mask, Imgproc.COLOR_RGB2YCrCb)

                Core.inRange(mask, low, high, mask)
                // because of the thresholding, the value of each pixel is either 0 or 255

                val rectMat = mask.submat(rect)

                val proportion: Double = Core.mean(rectMat).`val`[0] / 255.0

                rectMat.release()

                return proportion
            }

            val leftProportion = findColor(LEFT_LOWER, LEFT_UPPER, leftMask)
            val rightProportion = findColor(RIGHT_LOWER, RIGHT_UPPER, rightMask)

            Imgproc.rectangle(input, topLeft, bottomRight, Scalar(10.0,255.0,10.0), 5)
            Imgproc.rectangle(leftMask, topLeft, bottomRight, Scalar(10.0,255.0,10.0), 5)
            Imgproc.rectangle(rightMask, topLeft, bottomRight, Scalar(10.0,255.0,10.0), 5)

            val signal = when {
                leftProportion > THRESHOLD && leftProportion > rightProportion -> {
                    Signal.LEFT
                }
                rightProportion > THRESHOLD -> {
                    Signal.RIGHT
                }
                else -> {
                    Signal.MID
                }
            }

            signalWindow.add(signal)
            signalWindow.removeAt(0)

            var l = 0
            var m = 0
            var r = 0
            for (t: Signal in signalWindow){
                when (t) {
                    Signal.LEFT -> l++
                    Signal.MID -> m++
                    Signal.RIGHT -> r++
                }
            }

            finalSignal =  if (l>m && l>r)
                Signal.LEFT
            else if (m>r)
                Signal.MID
            else
                Signal.RIGHT

            Imgproc.putText(rightMask, finalSignal.name, Point(0.0, 20.0), Imgproc.FONT_HERSHEY_SIMPLEX, 20.0, Scalar(255.0, 255.0, 255.0), 4)
        }
    }

    companion object {
        @JvmField var RED_REGION_X = 150
        @JvmField var RED_REGION_Y = 140

        @JvmField var BLUE_REGION_X = 95
        @JvmField var BLUE_REGION_Y = 350

        @JvmField var REGION_HEIGHT = 125
        @JvmField var REGION_WIDTH = 95

        @JvmField var LEFT_LOWER_Y = 0.0
        @JvmField var LEFT_LOWER_Cr = 130.0
        @JvmField var LEFT_LOWER_Cb = 130.0
        @JvmField var LEFT_UPPER_Y = 255.0
        @JvmField var LEFT_UPPER_Cr = 190.0
        @JvmField var LEFT_UPPER_Cb = 200.0
        val LEFT_LOWER: Scalar get() = Scalar(LEFT_LOWER_Y, LEFT_LOWER_Cr, LEFT_LOWER_Cb)
        val LEFT_UPPER: Scalar get() = Scalar(LEFT_UPPER_Y, LEFT_UPPER_Cr, LEFT_UPPER_Cb)

        @JvmField var RIGHT_LOWER_Y = 0.0
        @JvmField var RIGHT_LOWER_Cr = 150.0
        @JvmField var RIGHT_LOWER_Cb = 0.0
        @JvmField var RIGHT_UPPER_Y = 255.0
        @JvmField var RIGHT_UPPER_Cr = 190.0
        @JvmField var RIGHT_UPPER_Cb = 115.0
        val RIGHT_LOWER: Scalar get() = Scalar(RIGHT_LOWER_Y, RIGHT_LOWER_Cr, RIGHT_LOWER_Cb)
        val RIGHT_UPPER: Scalar get() = Scalar(RIGHT_UPPER_Y, RIGHT_UPPER_Cr, RIGHT_UPPER_Cb)

        @JvmField var THRESHOLD: Double = .25
    }
}

