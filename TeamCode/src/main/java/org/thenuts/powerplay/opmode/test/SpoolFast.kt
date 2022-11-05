package org.thenuts.powerplay.opmode.test

import com.acmerobotics.dashboard.config.Config
import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.hardware.DcMotor

@Autonomous
@Config
class SpoolFast : OpMode() {
    lateinit var motor1: DcMotor
    lateinit var motor2: DcMotor

    override fun init() {
        motor1 = hardwareMap.dcMotor["slides1"]
        motor2 = hardwareMap.dcMotor["slides2"]
    }

    override fun loop() {
        motor1.power = SPOOL_POWER * gamepad1.right_stick_x
        motor2.power = SPOOL_POWER * gamepad1.right_stick_x
    }

    override fun stop() {
        motor1.power = 0.0
        motor2.power = 0.0
    }

    companion object {
        @JvmField var SPOOL_POWER: Double = 1.0
    }
}