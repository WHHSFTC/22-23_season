package org.thenuts.powerplay.opmode

import com.acmerobotics.dashboard.FtcDashboard
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry
import com.acmerobotics.dashboard.telemetry.TelemetryPacket
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import org.thenuts.powerplay.game.Alliance
import org.thenuts.powerplay.game.Mode
import org.thenuts.powerplay.subsystems.Robot
import org.thenuts.switchboard.command.Command
import org.thenuts.switchboard.command.CommandScheduler
import org.thenuts.switchboard.core.Logger
import org.thenuts.switchboard.hardware.Configuration
import org.thenuts.switchboard.util.Frame
import org.thenuts.switchboard.util.sinceJvmTime
import java.util.LinkedList
import kotlin.time.Duration
import kotlin.time.DurationUnit

abstract class CommandLinearOpMode<T: Robot>(val robotSupplier: (Logger, Configuration, Alliance, Mode) -> T, val alliance: Alliance, val mode: Mode) : LinearOpMode() {
    lateinit var log: Logger
    lateinit var config: Configuration
    lateinit var bot: T

    val sched = CommandScheduler()
    var frame = Frame(0, Duration.ZERO, Duration.ZERO)
    var initTime = Duration.ZERO
    var startTime = Duration.ZERO
    var recentSteps = MutableList(10) { 0.0 }

    open fun initHook() { }
    open fun postInitHook() { }
    open fun initLoopHook() { }
    open fun startHook() { }
    open fun postStartHook() { }
    open fun loopHook() { }
    open fun stopHook() { }

    companion object DashboardReceiver : Logger.LogReceiver {
        var ENABLED = false

        val dash = FtcDashboard.getInstance()
        var packet: TelemetryPacket = TelemetryPacket()
        init {
            packet = TelemetryPacket()
        }

        override fun print(l: Logger.LogStream) {
            packet.addLine(l.name)
            packet.addLine("---")
            l.mutableMap.forEach { (k, v) -> packet.put(k, v) }
            l.suppliers.forEach { (k, v) -> packet.put(k, v()) }
            dash.sendTelemetryPacket(packet)
            packet = TelemetryPacket()
        }
    }

    override fun runOpMode() {
        ENABLED = true
        log = Logger(telemetry)
        log.addReceiver(DashboardReceiver)
        config = Configuration(hardwareMap, log)
        bot = robotSupplier(log, config, alliance, mode)

        initTime = Duration.sinceJvmTime()
        sched.clear()
        initHook()
        bot.initCommands.forEach(sched::addCommand)
        postInitHook()
        log.out["transmission interval"] = telemetry.msTransmissionInterval
        log.update()

        while (!isStarted) {
            initLoopHook()
            updateFrom(initTime)
            bot.hardwareScheduler.output(all = true)
        }

        startTime = Duration.sinceJvmTime()
        sched.clear()
        startHook()
        bot.startCommands.forEach(sched::addCommand)
        postStartHook()
        bot.hardwareScheduler.output(all = true)
        log.update()

        while (!isStopRequested) {
            loopHook()
            updateFrom(startTime)
            bot.hardwareScheduler.output()
        }

        stopHook()
        sched.clear()
        bot.hardwareScheduler.output(all = true)
        log.update()
        ENABLED = false
    }

    private fun updateFrom(basis: Duration) {
        config.read()
        frame = Frame.from(basis, frame)
        val step = frame.step.toDouble(DurationUnit.MILLISECONDS)
        recentSteps.removeAt(0)
        recentSteps.add(step)
        log.out["STEP"] = recentSteps.average()
        log.out["RUNTIME"] = runtime
        sched.update(frame)
        log.update()
        idle()
    }
}