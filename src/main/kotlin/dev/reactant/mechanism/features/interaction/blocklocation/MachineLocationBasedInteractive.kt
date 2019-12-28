package dev.reactant.mechanism.features.interaction.blocklocation

import io.reactivex.subjects.BehaviorSubject

/**
 * A machine that can be interact by clicking locations
 */
interface MachineLocationBasedInteractive {
    val interactiveLocations: BehaviorSubject<Set<InteractiveLocation>>

}

