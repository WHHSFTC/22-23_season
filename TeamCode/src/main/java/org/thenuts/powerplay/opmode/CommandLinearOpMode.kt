package org.thenuts.powerplay.opmode

import com.acmerobotics.dashboard.FtcDashboard
import com.acmerobotics.dashboard.config.Config
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry
import com.acmerobotics.dashboard.telemetry.TelemetryPacket
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.hardware.VoltageSensor
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit
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

@Config
abstract class CommandLinearOpMode<T: Robot>(val robotSupplier: (Logger, Configuration, Alliance, Mode) -> T, val alliance: Alliance, val mode: Mode) : LinearOpMode() {
    lateinit var log: Logger
    lateinit var config: Configuration
    lateinit var batteryVoltageSensor: VoltageSensor
    lateinit var bot: T

    val sched = CommandScheduler()
    var frame = Frame(0, Duration.ZERO, Duration.ZERO)
    var initTime = Duration.ZERO
    var startTime = Duration.ZERO
    var recentSteps = MutableList(10) { 0.0 }

    open fun preInitHook() { }
    open fun initHook() { }
    open fun postInitHook() { }
    open fun initLoopHook() { }
    open fun startHook() { }
    open fun postStartHook() { }
    open fun loopHook() { }
    open fun stopHook() { }

    companion object DashboardReceiver : Logger.LogReceiver {
        var ENABLED = false

        val dash: FtcDashboard by lazy { FtcDashboard.getInstance() }
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

        @JvmField var initKeys = ""
        @JvmField var coachKeys = ""
        @JvmField var filter = false
    }

    override fun runOpMode() {
        ENABLED = true

        preInitHook()

        log = Logger(telemetry)
        log.addReceiver(DashboardReceiver)
        config = Configuration(hardwareMap, log)
        batteryVoltageSensor = config.hwMap.voltageSensor.iterator().next()
        bot = robotSupplier(log, config, alliance, mode)

        initKeys.split(",").forEach {
            log.out[it] = 0.0
        }

        initTime = Duration.sinceJvmTime()
        sched.clear()
        initHook()
        bot.initCommands.forEach(sched::addCommand)
        postInitHook()
        log.out["transmission interval"] = telemetry.msTransmissionInterval
        log.update()

        do {
            initLoopHook()
            updateFrom(initTime)
            if (mode != Mode.TELE)
                bot.hardwareScheduler.output(all = true)
        } while (!isStarted)

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

        for (i in config.revHubs.indices) {
            log.out["current $i"] = config.revHubs[i].getCurrent(CurrentUnit.AMPS)
        }
        log.out["voltage"] = batteryVoltageSensor.voltage

        sched.update(frame)
        if (filter) {
            val keys = coachKeys.split(",")
            keys.forEach {
                log.out.mutableMap.remove(it)
                log.out.suppliers.remove(it)
            }
        }
        log.update()
        idle()
    }
}