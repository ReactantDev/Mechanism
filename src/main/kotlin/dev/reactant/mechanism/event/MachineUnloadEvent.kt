package dev.reactant.mechanism.event

import dev.reactant.mechanism.Machine
import org.bukkit.event.HandlerList

/**
 * This event will be sent before the machine unload from the machine service
 * Chunk unload or destroy a machine will also trigger this event
 */
class MachineUnloadEvent(machine: Machine) : MachineEvent(machine) {

    companion object {
        private val HANDLER_LIST = HandlerList()
        @JvmStatic
        fun getHandlerList() = HANDLER_LIST
    }

    override fun getHandlers(): HandlerList = HANDLER_LIST

}
