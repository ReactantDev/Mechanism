package dev.reactant.mechanism

import dev.reactant.mechanism.event.MachineCreateEvent
import dev.reactant.mechanism.event.MachineDestroyEvent
import dev.reactant.mechanism.event.MachineLoadEvent
import dev.reactant.mechanism.event.MachineUnloadEvent
import dev.reactant.reactant.core.component.Component
import dev.reactant.reactant.core.component.lifecycle.LifeCycleHook
import dev.reactant.reactant.service.spec.dsl.register
import dev.reactant.reactant.service.spec.server.EventService
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.event.world.ChunkUnloadEvent

@Component
class MachineService(
        private val eventService: EventService
) : LifeCycleHook {
    val chunkMachines = HashMap<Chunk, HashSet<Machine>>()
    val fixedMachines = HashSet<Machine>()
    val machines = chunkMachines.values.flatten().union(fixedMachines)

    /**
     * Load the machine and fire the MachineLoadEvent
     * Use this function instead of createMachine if this is load from some kind of persistent service
     */
    fun loadMachine(machine: Machine, isFixed: Boolean = false) {
        assert(!(chunkMachines[machine.chunk]?.contains(machine) ?: false)
                && !fixedMachines.contains(machine)) { "Machine already loaded" }

        if (!machine.chunk.isLoaded || isFixed) Mechanism.logger.warn("Chunk is not loaded")
        Bukkit.getPluginManager().callEvent(MachineLoadEvent(machine))
        machine.afterLoaded()
        (if (isFixed) fixedMachines else chunkMachines.getOrPut(machine.chunk) { hashSetOf() }).add(machine)
    }

    /**
     * Load the machine and fire the MachineCreateEvent
     * Only use this function if this is a new machine
     */
    fun createMachine(machine: Machine, isFixed: Boolean = false) {
        assert(!(chunkMachines[machine.chunk]?.contains(machine) ?: false)
                && !fixedMachines.contains(machine)) { "Machine already exist" }

        if (!machine.chunk.isLoaded) Mechanism.logger.warn("Chunk is not loaded")
        Bukkit.getPluginManager().callEvent(MachineCreateEvent(machine))
        machine.afterCreated()
        loadMachine(machine, isFixed)
    }

    private fun unloadMachineResources(machine: Machine) {
        machine.beforeUnload()
        Bukkit.getPluginManager().callEvent(MachineUnloadEvent(machine))
        machine.afterUnloaded()
        chunkMachines.get(machine.chunk)!!.remove(machine)
    }

    fun destroyMachine(machine: Machine) {
        unloadMachineResources(machine)
        Bukkit.getPluginManager().callEvent(MachineDestroyEvent(machine))
        machine.afterDestroyed()
        unloadMachineResources(machine)
    }

    fun unloadMachine(machine: Machine) {
        unloadMachineResources(machine)
    }

    private fun unloadChunkMachine(chunk: Chunk) {
        chunkMachines[chunk]?.forEach { unloadMachine(it) }
        chunkMachines.remove(chunk)
    }

    override fun onEnable() {
        register(eventService) {
            ChunkUnloadEvent::class.observable().subscribe {
                unloadChunkMachine(it.chunk)
            }
        }
    }

    override fun onDisable() {
        chunkMachines.values.flatten().forEach { unloadMachine(it) }
    }
}
