package org.thenuts.powerplay.opmode.commands

import com.acmerobotics.roadrunner.control.PIDFController
import com.acmerobotics.roadrunner.drive.DriveSignal
import com.acmerobotics.roadrunner.geometry.Pose2d
import com.acmerobotics.roadrunner.geometry.Vector2d
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.thenuts.powerplay.acme.drive.DriveConstants
import org.thenuts.powerplay.opmode.auto.CyclingAuto
import org.thenuts.powerplay.opmode.auto.angleWrap
import org.thenuts.powerplay.subsystems.October
import org.thenuts.powerplay.subsystems.TapeDetector
import org.thenuts.switchboard.command.Command
import org.thenuts.switchboard.dsl.mkLinear
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.min
import kotlin.math.sign

class StackCommand(bot: October, targetHeading: Double): Command by mkLinear(finally = { }, {
    bot.tapeDetector.disable()
    bot.tapeDetector.enable()
    while (bot.tapeDetector.read() == TapeDetector.TapeState.MISSING) {
        if (!bot.drive.isBusy) return@mkLinear
        yield()
    }
    bot.drive.trajectorySequenceRunner.interrupt()
    val startPose = bot.drive.poseEstimate
    val headingController = PIDFController(CyclingAuto.HEADING_PID)
    val axialController = PIDFController(CyclingAuto.DIST_PID)
    val lateralController = PIDFController(CyclingAuto.DIST_PID)
    do {
        val heading = bot.drive.externalHeading
        val headingError = (targetHeading - heading).angleWrap()
        val omega = headingController.update(
            measuredPosition = -headingError,
            measuredVelocity = bot.drive.poseVelocity?.heading
        )

//                val xError = intakePose.y - bot.drive.poseEstimate.y
        val xError = CyclingAuto.DIST_TARGET - bot.intakeSensor.getDistance(DistanceUnit.INCH)
        bot.log.out["xError"] = xError

        var vx = axialController.update(-xError)

        val tapeState = bot.tapeDetector.read()

        var correction = TapeDetector.suggestedCorrection(tapeState) ?: Vector2d()
        correction /= DriveConstants.kV
        var vy = correction.y

        bot.log.out["suggested y correction"] = correction.y

        if (headingError.absoluteValue > CyclingAuto.HEADING_THRESHOLD) {
            vx = 0.0
            vy = 0.0
        }

        if (vx < 0.0)
            vx = sign(vx) * min(vx.absoluteValue, correction.x.absoluteValue)

        val vel = Pose2d(
            Vector2d(vx, vy).rotated(headingError),
            omega
        )

        val driveSignal = DriveSignal(vel, Pose2d())
        bot.drive.setDriveSignal(driveSignal)
        bot.log.out["drive power"] = vel
        yield()
    } while (xError.absoluteValue > CyclingAuto.DIST_THRESHOLD)
    bot.drive.setDriveSignal(DriveSignal())
    yield()
})