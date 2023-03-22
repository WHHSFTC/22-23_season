package org.thenuts.powerplay.subsystems

import com.acmerobotics.roadrunner.geometry.Pose2d
import com.acmerobotics.roadrunner.trajectory.constraints.TrajectoryAccelerationConstraint

data class AccelDecelConstraint(
    val maxAccel: Double,
    val maxDecel: Double
): TrajectoryAccelerationConstraint {
    override fun get(s: Double, pose: Pose2d, deriv: Pose2d, baseRobotVel: Pose2d): Double {
        return (deriv.headingVec().dot(baseRobotVel.headingVec()) + 1) / 2.0 * (maxAccel - maxDecel) + maxDecel
    }
}