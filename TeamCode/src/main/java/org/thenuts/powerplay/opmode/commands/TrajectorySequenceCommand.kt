package org.thenuts.powerplay.opmode.commands

import com.acmerobotics.roadrunner.geometry.Pose2d
import org.thenuts.powerplay.acme.drive.SampleMecanumDrive
import org.thenuts.powerplay.acme.trajectorysequence.TrajectorySequenceBuilder
import org.thenuts.switchboard.command.Command
import org.thenuts.switchboard.dsl.CommandListContext
import org.thenuts.switchboard.util.Frame

class TrajectorySequenceCommand(val drive: SampleMecanumDrive, val startPose: Pose2d, val quickExit: Boolean = false, builder: TrajectorySequenceBuilder.() -> Unit) : Command {
    override var done: Boolean = false

    val trajectorySequence = SampleMecanumDrive.trajectorySequenceBuilder(startPose).apply(builder).apply {
        if (quickExit) {
//            addSpatialMarker(lastPose.vec()) { done = true }
            addTemporalMarker { done = true }
        }
    }.build()

    override val postreqs: List<Pair<Command, Int>> = listOf(drive to 10)

    override fun start(frame: Frame) {
        drive.followTrajectorySequenceAsync(trajectorySequence)
        done = false
    }

    override fun update(frame: Frame) {
        if (!done && !drive.isBusy)
            done = true
    }
}

fun CommandListContext.go(drive: SampleMecanumDrive, startPose: Pose2d, quickExit: Boolean = false, b: TrajectorySequenceBuilder.() -> Unit) {
    add(TrajectorySequenceCommand(drive, startPose, quickExit, b))
}
