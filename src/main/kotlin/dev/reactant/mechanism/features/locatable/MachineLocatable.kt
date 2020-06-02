package dev.reactant.mechanism.features.locatable

import io.reactivex.rxjava3.subjects.BehaviorSubject
import org.bukkit.Location

interface MachineLocatable {
    val location: BehaviorSubject<Location>
}
