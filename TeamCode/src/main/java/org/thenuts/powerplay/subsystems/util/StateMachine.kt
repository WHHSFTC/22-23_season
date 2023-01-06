package org.thenuts.powerplay.subsystems.util

import org.thenuts.switchboard.util.Frame

/**
 * Boilerplate for implementing state machines for mechanisms
 * [State] should be implemented with CRTP to return its own type for transitions
 * eg. class ClawState : StateMachine.State<Claw, ClawState>
 *
 * Pending rewrite when context receivers are stable
 */
open class StateMachine<C, S: StateMachine.State<C, S>>(initial: S, val mech: C) {
    interface State<in C, out S> {
        fun transition(mech: C, frame: Frame): S? { return null }
        fun init(mech: C) { }
        fun update(mech: C, frame: Frame)
        fun cleanup(mech: C) { }
    }

    var state: S = initial
        private set

    fun switch(newState: S) {
        state.cleanup(mech)
        newState.init(mech)
        state = newState
    }

    fun update(frame: Frame) {
        val newState = state.transition(mech, frame)
        if (newState != null && newState != state) {
            switch(newState)
        }
        state.update(mech, frame)
    }
}