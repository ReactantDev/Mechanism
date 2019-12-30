package dev.reactant.mechanism.features.locatable

import io.reactivex.subjects.BehaviorSubject
import org.bukkit.Location

interface MachineLocatable {
    val location: BehaviorSubject<Location>
}
