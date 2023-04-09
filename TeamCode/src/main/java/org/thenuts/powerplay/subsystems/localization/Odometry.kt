package org.thenuts.powerplay.subsystems.localization

import com.acmerobotics.roadrunner.geometry.Pose2d
import org.apache.commons.math3.linear.Array2DRowRealMatrix
import org.apache.commons.math3.linear.DecompositionSolver
import org.apache.commons.math3.linear.LUDecomposition
import org.apache.commons.math3.linear.MatrixUtils
import org.thenuts.powerplay.acme.util.Encoder

abstract class Odometry(val wheels: List<DeadWheel>) {
    var poseDelta = Pose2d()
    var poseVelocity = Pose2d()

    private var lastWheelPositions: List<Double>? = null

    private val forwardSolver: DecompositionSolver

    init {
        require(wheels.size >= 3) { "3 wheels must be provided" }

        val inverseMatrix = Array2DRowRealMatrix(3, 3)
        for (i in wheels.indices) {
            val orientationVector = wheels[i].pose.headingVec()
            val positionVector = wheels[i].pose.vec()
            inverseMatrix.setEntry(i, 0, orientationVector.x)
            inverseMatrix.setEntry(i, 1, orientationVector.y)
            inverseMatrix.setEntry(
                i,
                2,
                positionVector.x * orientationVector.y - positionVector.y * orientationVector.x
            )
        }

        forwardSolver = LUDecomposition(inverseMatrix).solver

        require(forwardSolver.isNonSingular) { "The specified configuration cannot support full localization" }
    }

    private fun calculatePoseDelta(wheelDeltas: List<Double>): Pose2d {
        val rawPoseDelta = forwardSolver.solve(
            MatrixUtils.createRealMatrix(
                arrayOf(wheelDeltas.toDoubleArray())
            ).transpose()
        )
        return Pose2d(
            rawPoseDelta.getEntry(0, 0),
            rawPoseDelta.getEntry(1, 0),
            rawPoseDelta.getEntry(2, 0)
        )
    }

    fun update() {
        val wheelPositions = wheels.map { it.encoder.currentPosition.toDouble() * it.tickDistance }
        poseDelta = lastWheelPositions?.let { last ->
            val wheelDeltas = wheelPositions
                .zip(last)
                .map { it.first - it.second }
            calculatePoseDelta(wheelDeltas)
        } ?: Pose2d()

        lastWheelPositions = wheelPositions

        val wheelVelocities = wheels.map { it.encoder.correctedVelocity * it.tickDistance }
        poseVelocity = calculatePoseDelta(wheelVelocities)
    }

    data class DeadWheel(
        val encoder: Encoder,
        val pose: Pose2d,
        val tickDistance: Double,
    )
}