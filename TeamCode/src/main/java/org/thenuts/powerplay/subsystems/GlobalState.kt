package org.thenuts.powerplay.subsystems

import com.acmerobotics.roadrunner.geometry.Pose2d
import kotlin.time.Duration

object GlobalState {
    var poseEstimate: Pose2d = Pose2d()
    var autoStartTime: Duration = Duration.ZERO
    var autoStopTime: Duration = Duration.ZERO
}