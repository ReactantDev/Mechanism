package dev.reactant.mechanism.persistent

import dev.reactant.mechanism.Machine
import dev.reactant.mechanism.MachineService
import dev.reactant.mechanism.Mechanism
import dev.reactant.mechanism.event.MachineUnloadEvent
import dev.reactant.mechanism.persistent.repository.MachinePersistentDataRepository
import dev.reactant.reactant.core.ReactantCore
import dev.reactant.reactant.core.component.Component
import dev.reactant.reactant.core.component.lifecycle.LifeCycleHook
import dev.reactant.reactant.service.spec.dsl.register
import dev.reactant.reactant.service.spec.server.EventService
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.CompletableSubject
import io.reactivex.subjects.PublishSubject
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.event.EventPriority
import org.bukkit.event.world.ChunkLoadEvent

@Component
class MachinePersistentService(
        private val eventService: EventService,
        private val machineService: MachineService,
        private val machinePersistentDataRepository: MachinePersistentDataRepository,
        private val machineAdaptersService: MachineAdaptersService
) : LifeCycleHook {
    private val queries = PublishSubject.create<Completable>()
    private val queriesCompleted = CompletableSubject.create()
    override fun onEnable() {
        startHandleQueries()

        register(eventService) {
            MachineUnloadEvent::class.observable(EventPriority.LOWEST).subscribe { storeMachineState(it.machine) }
            ChunkLoadEvent::class.observable().subscribe { loadChunkMachines(it.chunk) }
        }

        Bukkit.getWorlds().flatMap { it.loadedChunks.toList() }.forEach { loadChunkMachines(it) }
    }

    private fun startHandleQueries() {
        queries.subscribeOn(Schedulers.io())
                .doOnComplete {
                    Mechanism.logger.info("All machine persistent data has been saved")
                    queriesCompleted.onComplete()
                }
                .flatMapCompletable { it }
                .doOnError { it.printStackTrace() }
                .subscribe()

    }

    override fun onDisable() {
        machineService.chunkMachines.flatMap { it.value }.union(machineService.fixedMachines).forEach { machineService.unloadMachine(it) }
        queries.onComplete()
        queriesCompleted.blockingAwait()
    }

    fun storeMachineState(machine: Machine) {
        val completable: Completable = machinePersistentDataRepository
                .insertOrUpdate(machineAdaptersService.getAdapter(machine)!!.store(machine))
                .doOnError {
                    throw IllegalStateException("Machine state cannot be saved", it)
                }
        queries.onNext(completable)
    }

    fun loadChunkMachines(chunk: Chunk) {
        val task: Completable = machinePersistentDataRepository
                .getAll(chunk)
                .map { machinesData -> machinesData.map { machineAdaptersService.getAdapter(it)!!.restore(it) } }
                .observeOn(ReactantCore.mainThreadScheduler)
                .doOnSuccess { machines -> machines.forEach { machineService.loadMachine(it) } }
                .ignoreElement()
        queries.onNext(task)
    }

}
