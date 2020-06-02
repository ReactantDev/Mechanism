package dev.reactant.mechanism.features.tickable

import io.reactivex.rxjava3.subjects.BehaviorSubject

interface MachineTickable {
    val isTicking: BehaviorSubject<Boolean>
    fun onTick()
}
