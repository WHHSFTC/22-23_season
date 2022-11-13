package org.firstinspires.ftc.teamcode.opmode.vision

import com.acmerobotics.roadrunner.geometry.Pose2d
import com.qualcomm.robotcore.hardware.Gamepad
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName
import org.openftc.easyopencv.OpenCvCameraFactory
import org.openftc.easyopencv.OpenCvCameraRotation
import org.thenuts.powerplay.game.Alliance
import org.thenuts.switchboard.core.Logger
import org.thenuts.switchboard.hardware.Configuration

class Vision(val logger: Logger, val config: Configuration, val alliance: Alliance) {
    val vpId: Int = config.hwMap.appContext.resources.getIdentifier("cameraMonitorViewId", "id", config.hwMap.appContext.packageName)
//    val subIds = OpenCvCameraFactory.getInstance().splitLayoutForMultipleViewports(vpId, 2, OpenCvCameraFactory.ViewportSplitMethod.HORIZONTALLY)
//    val subSubIds = OpenCvCameraFactory.getInstance().splitLayoutForMultipleViewports(subIds[1], 2, OpenCvCameraFactory.ViewportSplitMethod.VERTICALLY)
    var gamepad: Gamepad? = null

    val front: Camera = Camera(
        cam = OpenCvCameraFactory.getInstance().createWebcam(config.hwMap.get(WebcamName::class.java, "Front Camera"), vpId),
        botPose = Pose2d(5.0, 4.0, 0.0),
        physicalHeight = 6.0,
        verticalAngle = 0.0,
        trigger = { gamepad?.left_bumper ?: false },
        orientation = OpenCvCameraRotation.UPRIGHT,
        width = FRAME_WIDTH,
        height = FRAME_HEIGHT
    )
//    val red: Camera = Camera(
//        cam = OpenCvCameraFactory.getInstance().createWebcam(config.hwMap.get(WebcamName::class.java, "Red Camera"), subIds[1]),
//        botPose = Pose2d(0.0, -8.0, -PI/2.0),
//        physicalHeight = 15.0,
//        verticalAngle = -PI/6.0,
//        trigger = { gamepad?.right_bumper ?: false },
//        orientation = OpenCvCameraRotation.UPSIDE_DOWN,
//        width = FRAME_WIDTH,
//        height = FRAME_HEIGHT
//    )

    val signal = SignalProcessor(front, alliance)

    companion object {
        @JvmField var FRAME_WIDTH: Int = 640
        @JvmField var FRAME_HEIGHT: Int = 480
    }
}