package org.thenuts.powerplay.subsystems

import com.acmerobotics.dashboard.config.Config
import com.acmerobotics.roadrunner.geometry.Pose2d
import com.acmerobotics.roadrunner.localization.Localizer
import com.qualcomm.hardware.bosch.BNO055IMU
import org.thenuts.powerplay.acme.drive.ThreeOdo
import org.thenuts.powerplay.opmode.auto.angleWrap
import org.thenuts.switchboard.util.sinceJvmTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Config
class KalmanLocalizer(val odo: Localizer, val imu: BNO055IMU) : Localizer {
    override var poseEstimate: Pose2d = odo.poseEstimate
    override var poseVelocity: Pose2d? = odo.poseVelocity

    private var lastImuTime = Duration.ZERO

    fun getHeading(): Double {
        return (imu.angularOrientation.firstAngle - TELE_HEADING_OFFSET).angleWrap()
    }

    override fun update() {
        val odoPose = odo.poseEstimate
        val odoVel = odo.poseVelocity ?: Pose2d()

        if (Duration.sinceJvmTime() > lastImuTime + 1.seconds / IMU_FREQ && odoVel.heading < MIN_IMU_OMEGA) {
            odo.poseEstimate = odo.poseEstimate.copy(heading = getHeading())
        }

        poseEstimate = odo.poseEstimate
        poseVelocity = odo.poseVelocity
    }

    companion object {
        @JvmField var IMU_FREQ = 10.0
        @JvmField var MIN_IMU_OMEGA = 1.0
        var TELE_HEADING_OFFSET = 0.0
    }
}