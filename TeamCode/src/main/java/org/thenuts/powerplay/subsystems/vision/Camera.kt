package org.firstinspires.ftc.teamcode.opmode.vision

import com.acmerobotics.dashboard.FtcDashboard
import com.acmerobotics.roadrunner.geometry.Pose2d
import org.opencv.core.Mat
import org.opencv.core.Size
import org.openftc.easyopencv.OpenCvCamera
import org.openftc.easyopencv.OpenCvCameraRotation
import org.openftc.easyopencv.OpenCvPipeline

class Camera(private val cam: OpenCvCamera, val botPose: Pose2d, val physicalHeight: Double, val verticalAngle: Double = 0.0, val trigger: () -> Boolean, val orientation: OpenCvCameraRotation, width: Int, height: Int) {
    private val pipelines: MutableList<Pipeline> = mutableListOf()
    private val _views: MutableList<View> = mutableListOf()
    val views: String get() = _views.map { it.name }.toString()
    val index: Int get() = i
    val size = Size(width.toDouble(), height.toDouble())
    val width get() = size.width
    val height get() = size.height
    private var i = -1
    private var input: Mat? = null

    private val pipelineSplitter = object : OpenCvPipeline() {
        override fun init(mat: Mat?) {
            input = mat!!
            pipelines.forEach { mat.copyTo(it.input); it.initialize(this@Camera) }
        }

        override fun processFrame(mat: Mat?): Mat {
            if (mat == null) return Mat()
            pipelines.forEach { mat.copyTo(it.input); it.processFrame(this@Camera) }
            checkButton()
            return if (i >= 0 && i < _views.size) _views[i].labeledMat else mat
        }

        override fun onViewportTapped() {
            if (i >= _views.size - 1)
                i = -1
            else
                i++
        }

        private var last = false
        fun checkButton() {
            val s = trigger()
            if (s && !last)
                onViewportTapped()
            last = s
        }
    }

    init {
        cam.setPipeline(pipelineSplitter)
    }

    fun addPipeline(p: Pipeline) {
        if (pipelines.isEmpty()) {
            startStreaming()
        }
        if (p !in pipelines) {
            pipelines.add(p)
            input?.let { it.copyTo(p.input); p.initialize(this) }
        }
    }

    fun removePipeline(p: Pipeline) {
        if (p in pipelines) {
            pipelines.remove(p)
            if (pipelines.isEmpty()) {
                stopStreaming()
            }
        }
    }

    fun addView(v: View) {
        if (v !in _views)
            _views.add(v)
    }

    fun removeView(v: View) {
        _views.remove(v)
    }

    private fun startStreaming() {
        cam.openCameraDeviceAsync(object : OpenCvCamera.AsyncCameraOpenListener {
            override fun onOpened() {
                cam.startStreaming(Vision.FRAME_WIDTH, Vision.FRAME_HEIGHT, orientation)
//                if (Logger.DEBUG) {
//                    startDebug()
//                }
            }

            override fun onError(errorCode: Int) {
//                cam.stopStreaming() // not allowed
            }
        })
    }

    private fun stopStreaming() {
//        if (Logger.DEBUG) {
//            stopDebug()
//        }
        cam.stopStreaming()
        cam.closeCameraDeviceAsync { }
    }

    fun startDebug() {
        FtcDashboard.getInstance().startCameraStream(cam, 10.0);
    }

    fun stopDebug() {
        FtcDashboard.getInstance().stopCameraStream()
    }
}