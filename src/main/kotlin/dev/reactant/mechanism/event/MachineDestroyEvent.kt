package dev.reactant.mechanism.event

import dev.reactant.mechanism.Machine
import org.bukkit.event.HandlerList

/**
 * This event will be sent before a machine was be destroyed in the machine service
 * It is not some kind of block destroy event so it cannot be cancelled
 */
class MachineDestroyEvent(machine: Machine) : MachineEvent(machine) {

    companion object {
        private val HANDLER_LIST = HandlerList()
        @JvmStatic
        fun getHandlerList() = HANDLER_LIST
    }

    override fun getHandlers(): HandlerList = HANDLER_LIST


}
