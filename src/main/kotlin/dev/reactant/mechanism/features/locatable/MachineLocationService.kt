package dev.reactant.mechanism.features.locatable

import dev.reactant.mechanism.MachineService
import dev.reactant.mechanism.event.MachineLoadEvent
import dev.reactant.reactant.core.component.Component
import dev.reactant.reactant.core.component.lifecycle.LifeCycleHook
import dev.reactant.reactant.service.spec.server.EventService
import org.bukkit.Location

@Component
class MachineLocationService(
        private val machineService: MachineService,
        private val eventService: EventService
) : LifeCycleHook {

    private val _machineLocation = HashMap<MachineLocatable, Location>();
    private val _locationMachines = HashMap<Location, HashSet<MachineLocatable>>();

    val locationMachines: Map<Location, Set<MachineLocatable>> get() = _locationMachines.mapValues { it.value.toSet() }

    override fun onEnable() {
        machineService.machines.mapNotNull { it as? MachineLocatable }
                .forEach { monitorMachineLocations(it) }

        eventService {
            MachineLoadEvent::class.observable()
                    .map { it.machine }
                    .ofType(MachineLocatable::class.java)
                    .subscribe { monitorMachineLocations(it) }
        }
    }

    private fun removeMachineLocation(machine: MachineLocatable) {
        _machineLocation[machine]?.let { lastLocation ->
            _locationMachines[lastLocation]?.let {
                it.remove(machine)
                if (it.isEmpty()) _locationMachines.remove(lastLocation)
            }
        }
        _machineLocation.remove(machine)
    }

    private fun monitorMachineLocations(machine: MachineLocatable) {
        machine.location.doOnComplete {
            removeMachineLocation(machine)
        }.subscribe { location ->
            removeMachineLocation(machine)
            _machineLocation[machine] = location
            _locationMachines.getOrPut(location, { HashSet() }).add(machine)
        }
    }

    /**
     * Get machines by location
     */
    fun getMachine(location: Location): Set<MachineLocatable> {
        return _locationMachines.get(location)?.toSet() ?: setOf()
    }


}
