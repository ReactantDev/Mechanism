package dev.reactant.mechanism

import dev.reactant.mechanism.state.StateHolder
import dev.reactant.mechanism.state.StateManager
import io.reactivex.rxjava3.subjects.BehaviorSubject
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.World
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

abstract class Machine private constructor(
        val uuid: UUID, val chunk: Chunk, protected val stateManager: StateManager
) : StateHolder by stateManager {

    constructor(uuid: UUID, chunk: Chunk) : this(uuid, chunk, StateManager())
    constructor(chunk: Chunk) : this(UUID.randomUUID(), chunk)

    val typeIdentifier = this::class.findAnnotation<MachineType>().let {
        it?.typeIdentifier ?: throw IllegalStateException(this::class.qualifiedName + " has no @MachineType")
    }

    protected fun <T> defaultState(state: T): BehaviorSubject<T> = stateManager.defaultState(state)

    fun beforeUnload() = Unit

    fun afterLoaded() = Unit
    fun afterCreated() = Unit
    fun afterDestroyed() = Unit
    fun afterUnloaded() = completeAllStates()
}

interface MachinePersistentData {
    val uuid: UUID

    /**
     * Used as index, the machine will be load when the chunk loaded
     */
    val chunk: ChunkInfo

    val machineClass
        get() = this::class.findAnnotation<MachineDataOf>()?.machineClass
                ?: throw IllegalStateException(this::class.qualifiedName + " has no @MachineDataOf")

    val machineTypeIdentifier
        get() = machineClass.findAnnotation<MachineType>()?.typeIdentifier
                ?: throw IllegalStateException(machineClass.qualifiedName + " has no @MachineType")

}

data class ChunkInfo(
        val x: Int,
        val z: Int,
        val worldUUID: UUID
) {
    fun getWorld(): World? = Bukkit.getWorld(worldUUID)
    fun getChunk(): Chunk? = getWorld()?.getChunkAt(x, z)

    companion object {
        fun fromChunk(chunk: Chunk) = ChunkInfo(chunk.x, chunk.z, chunk.world.uid)
    }
}

annotation class MachineType(val typeIdentifier: String)
annotation class MachineAdapterOf(val machineClass: KClass<out Machine>, val dataClass: KClass<out MachinePersistentData>)
annotation class MachineDataOf(val machineClass: KClass<out Machine>)
