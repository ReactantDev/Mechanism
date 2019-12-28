package dev.reactant.mechanism.features.tickable

import dev.reactant.mechanism.MachineService
import dev.reactant.mechanism.event.MachineLoadEvent
import dev.reactant.reactant.core.component.Component
import dev.reactant.reactant.core.component.lifecycle.LifeCycleHook
import dev.reactant.reactant.service.spec.dsl.register
import dev.reactant.reactant.service.spec.server.EventService
import dev.reactant.reactant.service.spec.server.SchedulerService

@Component
class MachineTickingService(
        private val machineService: MachineService,
        private val schedulerService: SchedulerService,
        private val eventService: EventService
) : LifeCycleHook {
    private val _tickingMachines = hashSetOf<MachineTickable>()
    val tickingMachines get() = _tickingMachines.toSet()

    override fun onEnable() {
        machineService.machines.mapNotNull { it as? MachineTickable }
                .forEach { monitorMachineTickingState(it) }

        register(eventService) {
            MachineLoadEvent::class.observable().map { it.machine }.ofType(MachineTickable::class.java)
                    .subscribe { monitorMachineTickingState(it) }
        }

        schedulerService.interval(1, 1).subscribe { tickAllMachine() }
    }

    fun monitorMachineTickingState(machine: MachineTickable) {
        machine.isTicking
                .doOnComplete { _tickingMachines.remove(machine) }
                .subscribe { tickingState ->
                    if (tickingState) _tickingMachines.add(machine)
                    else _tickingMachines.remove(machine)
                }
    }

    fun tickAllMachine() {
        tickingMachines.forEach { it.onTick() }
    }
}
