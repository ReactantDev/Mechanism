package dev.reactant.mechanism.event

import dev.reactant.mechanism.Machine
import org.bukkit.event.HandlerList

/**
 * This event will be sent after a machine was loaded in the machine service
 * Create new machine will also trigger this event
 */
class MachineLoadEvent(machine: Machine) : MachineEvent(machine) {

    companion object {
        private val HANDLER_LIST = HandlerList()
        @JvmStatic
        fun getHandlerList() = HANDLER_LIST
    }

    override fun getHandlers(): HandlerList = HANDLER_LIST


}
