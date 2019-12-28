package dev.reactant.mechanism.event

import dev.reactant.mechanism.Machine
import org.bukkit.event.HandlerList

/**
 * This event will be sent after a machine was be created in the machine service
 */
class MachineCreateEvent(machine: Machine) : MachineEvent(machine) {

    companion object {
        private val HANDLER_LIST = HandlerList()
        @JvmStatic
        fun getHandlerList() = HANDLER_LIST
    }

    override fun getHandlers(): HandlerList = HANDLER_LIST


}
