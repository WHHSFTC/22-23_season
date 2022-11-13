package org.firstinspires.ftc.teamcode.opmode.vision

import org.opencv.core.Mat

interface Pipeline {
    val input: Mat
    fun initialize(cam: Camera) { }
    fun processFrame(cam: Camera)
}