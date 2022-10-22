package org.thenuts.powerplay.opmode.test

import com.acmerobotics.dashboard.config.Config
import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.hardware.DcMotor

@Autonomous
@Config
class SpoolFast : OpMode() {
    lateinit var motor: DcMotor

    override fun init() {
        motor = hardwareMap.dcMotor["slides"]
    }

    override fun loop() {
        motor.power = SPOOL_POWER * gamepad1.right_stick_x
    }

    override fun stop() {
        motor.power = 0.0
    }

    companion object {
        @JvmField var SPOOL_POWER: Double = 0.2
    }
}