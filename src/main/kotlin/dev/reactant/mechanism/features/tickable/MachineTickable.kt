package dev.reactant.mechanism.features.tickable

import io.reactivex.subjects.BehaviorSubject

interface MachineTickable {
    val isTicking: BehaviorSubject<Boolean>
    fun onTick()
}
