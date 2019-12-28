package dev.reactant.mechanism.event

import dev.reactant.mechanism.Machine
import org.bukkit.event.Event

abstract class MachineEvent(val machine: Machine) : Event()
