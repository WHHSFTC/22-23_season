package org.thenuts.powerplay.subsystems

import com.acmerobotics.roadrunner.geometry.Pose2d
import com.acmerobotics.roadrunner.localization.Localizer

class Kalman : Localizer {
    override var poseEstimate: Pose2d = Pose2d()
    override val poseVelocity: Pose2d = Pose2d()

    override fun update() {

    }
}