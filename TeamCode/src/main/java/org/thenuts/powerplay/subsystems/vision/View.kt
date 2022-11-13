package org.firstinspires.ftc.teamcode.opmode.vision

import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

class View (
    val name: String,
    val color: Scalar = VisionUtil.Color.RED
) : Mat() {
    var labeledMat: Mat = Mat()
        get() {
            this.copyTo(field)
            Imgproc.putText(field, name, Point(0.0, 20.0), Imgproc.FONT_HERSHEY_PLAIN, 1.0, color, 2)
            return field
        }
}
