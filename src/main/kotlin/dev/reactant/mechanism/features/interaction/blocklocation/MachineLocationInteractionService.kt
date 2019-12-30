package dev.reactant.mechanism.features.interaction.blocklocation

import dev.reactant.mechanism.MachineService
import dev.reactant.mechanism.event.MachineLoadEvent
import dev.reactant.reactant.core.component.Component
import dev.reactant.reactant.core.component.lifecycle.LifeCycleHook
import dev.reactant.reactant.service.spec.dsl.register
import dev.reactant.reactant.service.spec.server.EventService
import org.bukkit.Location
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent

@Component
class MachineLocationInteractionService(val machineService: MachineService, val eventService: EventService) : LifeCycleHook {
    private val monitoringLocation = HashMap<Pair<Location, Action>, HashSet<InteractiveLocation>>()
    private val machineInteractiveLocationMap = HashMap<MachineLocationBasedInteractive, HashSet<InteractiveLocation>>()
    override fun onEnable() {
        machineService.machines.mapNotNull { it as? MachineLocationBasedInteractive }
                .forEach { monitorMachineInteractiveLocations(it) }

        register(eventService) {
            MachineLoadEvent::class.observable().map { it.machine }.ofType(MachineLocationBasedInteractive::class.java)
                    .subscribe { monitorMachineInteractiveLocations(it) }

            PlayerInteractEvent::class.observable(EventPriority.LOW)
                    .filter {
                        it.useInteractedBlock() != Event.Result.DENY && it.useItemInHand() != Event.Result.DENY
                                && monitoringLocation.contains(it.clickedBlock?.location to it.action)
                    }
                    .map { it.player to monitoringLocation[it.clickedBlock?.location to it.action] }
                    .subscribe { (player, interactiveLocations) ->
                        interactiveLocations?.forEach { it.callback(player) }
                    }
        }
    }

    private fun removeMonitoringLocation(location: InteractiveLocation) {
        location.interactionTypes.forEach { action ->
            val key = location.location to action
            monitoringLocation[key]?.let {
                it.remove(location);
                if (it.isEmpty()) monitoringLocation.remove(key)
            }
        }
    }

    private fun removeMachineMonitoring(machine: MachineLocationBasedInteractive) {
        machineInteractiveLocationMap.remove(machine)?.let { it.forEach { removeMonitoringLocation(it) } }
    }

    private fun addMachineMonitoring(machine: MachineLocationBasedInteractive) {
        machine.interactiveLocations.value!!.let { locations ->
            machineInteractiveLocationMap[machine] = HashSet(locations)
            locations.forEach { addMonitoringLocation(it) }
        }
    }

    private fun addMonitoringLocation(location: InteractiveLocation) {
        location.interactionTypes.forEach { action ->
            val key = location.location to action
            monitoringLocation.getOrPut(key, { hashSetOf() }).add(location)
        }
    }


    private fun monitorMachineInteractiveLocations(machine: MachineLocationBasedInteractive) {
        machine.interactiveLocations
                .doOnComplete { removeMachineMonitoring(machine) }
                .subscribe {
                    removeMachineMonitoring(machine)
                    addMachineMonitoring(machine)
                }
    }

}
