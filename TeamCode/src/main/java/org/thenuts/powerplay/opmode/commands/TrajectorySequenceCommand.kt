package org.thenuts.powerplay.opmode.commands

import com.acmerobotics.roadrunner.geometry.Pose2d
import org.thenuts.powerplay.acme.drive.SampleMecanumDrive
import org.thenuts.powerplay.acme.trajectorysequence.TrajectorySequence
import org.thenuts.powerplay.acme.trajectorysequence.TrajectorySequenceBuilder
import org.thenuts.switchboard.command.Command
import org.thenuts.switchboard.dsl.CommandListContext
import org.thenuts.switchboard.util.Frame

class TrajectorySequenceCommand(val drive: SampleMecanumDrive, val trajectorySequence: TrajectorySequence) : Command {
    override var done: Boolean = false

    override val postreqs: List<Pair<Command, Int>> = listOf(drive to 10)

    override fun start(frame: Frame) {
        drive.followTrajectorySequenceAsync(trajectorySequence)
    }

    override fun update(frame: Frame) {
        done = !drive.isBusy
    }
}

fun CommandListContext.go(drive: SampleMecanumDrive, startPose: Pose2d, b: TrajectorySequenceBuilder.() -> Unit) {
    add(TrajectorySequenceCommand(drive, SampleMecanumDrive.trajectorySequenceBuilder(startPose).also(b).build()))
}