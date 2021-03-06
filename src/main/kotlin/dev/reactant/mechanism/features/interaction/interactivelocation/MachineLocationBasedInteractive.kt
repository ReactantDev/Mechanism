package dev.reactant.mechanism.features.interaction.interactivelocation

import io.reactivex.rxjava3.subjects.BehaviorSubject

/**
 * A machine that can be interact by clicking locations
 */
interface MachineLocationBasedInteractive {
    val interactiveLocations: BehaviorSubject<Set<InteractiveLocation>>

}

