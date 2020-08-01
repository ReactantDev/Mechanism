package dev.reactant.mechanism.persistent

import dev.reactant.mechanism.Machine
import dev.reactant.mechanism.MachineService
import dev.reactant.mechanism.Mechanism
import dev.reactant.mechanism.event.MachineUnloadEvent
import dev.reactant.mechanism.persistent.repository.MachinePersistentDataRepository
import dev.reactant.reactant.core.component.Component
import dev.reactant.reactant.core.component.lifecycle.LifeCycleHook
import dev.reactant.reactant.service.spec.server.EventService
import dev.reactant.reactant.service.spec.server.SchedulerService
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.CompletableSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.event.EventPriority
import org.bukkit.event.world.ChunkLoadEvent

@Component
class MachinePersistentService(
        private val eventService: EventService,
        private val schedulerService: SchedulerService,
        private val machineService: MachineService,
        private val machinePersistentDataRepository: MachinePersistentDataRepository,
        private val machineAdaptersService: MachineAdaptersService
) : LifeCycleHook {
    private lateinit var queries: PublishSubject<Completable>
    private lateinit var queriesCompleted: CompletableSubject

    private fun createQueriesSubject() {
        queries = PublishSubject.create<Completable>()
        queriesCompleted = CompletableSubject.create()
    }

    override fun onEnable() {
        startHandleQueries()

        eventService {
            MachineUnloadEvent::class.observable(EventPriority.LOWEST).subscribe { storeMachineState(it.machine) }
            ChunkLoadEvent::class.observable().subscribe { loadChunkMachines(it.chunk) }
        }

        Bukkit.getWorlds().flatMap { it.loadedChunks.toList() }.forEach { loadChunkMachines(it) }
    }

    private fun startHandleQueries() {
        createQueriesSubject()
        queries.subscribeOn(Schedulers.single())
                .doOnComplete {
                    Mechanism.logger.info("All machine persistent data has been saved")
                    queriesCompleted.onComplete()
                }
                .flatMapCompletable { it }
                .retry(5)
                .doOnError { it.printStackTrace() }
                .subscribe()

    }

    override fun onSave() {
        machineService.chunkMachines.flatMap { it.value }.union(machineService.fixedMachines)
                .forEach { machineService.unloadMachine(it) }
        queries.onComplete()
        queriesCompleted.blockingAwait()

        // start a new queries subject
        startHandleQueries()
    }

    override fun onDisable() {}

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
                .observeOn(schedulerService.mainThreadScheduler)
                .doOnSuccess { machines -> machines.forEach { machineService.loadMachine(it) } }
                .ignoreElement()
        queries.onNext(task)
    }

}
