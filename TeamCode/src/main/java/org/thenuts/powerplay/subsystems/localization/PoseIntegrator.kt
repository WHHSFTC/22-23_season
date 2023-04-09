package org.thenuts.powerplay.subsystems.localization

import com.acmerobotics.roadrunner.geometry.Pose2d
import com.acmerobotics.roadrunner.localization.Localizer

class PoseIntegrator(val poseDeltaSensor: Odometry, val poseVelocitySensor: Odometry) : Localizer {
    override var poseEstimate: Pose2d = Pose2d()
        set(value) {
            field = value
        }
    override val poseVelocity: Pose2d?
        get() = TODO("Not yet implemented")

    override fun update() {
        TODO("Not yet implemented")
    }

}