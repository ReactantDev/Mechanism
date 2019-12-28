package dev.reactant.mechanism

import com.comphenix.protocol.utility.MinecraftReflection
import com.google.gson.GsonBuilder
import dev.reactant.mechanism.serialize.ItemStackTypeAdapter
import dev.reactant.mechanism.serialize.LocationTypeAdapter
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.inventory.ItemStack
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

abstract class Machine(val uuid: UUID, val chunk: Chunk) {
    constructor(chunk: Chunk) : this(UUID.randomUUID(), chunk)

    val typeIdentifier = this::class.findAnnotation<MachineType>().let {
        it?.typeIdentifier ?: throw IllegalStateException(this::class.qualifiedName + " has no @MachineType")
    }
    /**
     * Should all state be complete when machine unload
     */
    private val states: HashSet<Subject<*>> = hashSetOf()

    /**
     * Automatically depose behavior subject
     */
    protected fun <T> defaultState(state: T): BehaviorSubject<T> = BehaviorSubject.createDefault(state).also { states.add(it) }

    /**
     * Will be called when unload
     */
    fun onUnload() = this.states.forEach { it.onComplete() }
}

interface MachinePersistentData {
    val uuid: UUID
    val chunk: ChunkInfo

    val machineClass
        get() = this::class.findAnnotation<MachineDataOf>()?.machineClass
                ?: throw IllegalStateException(this::class.qualifiedName + " has no @MachineDataOf")

    val machineTypeIdentifier
        get() = machineClass.findAnnotation<MachineType>()?.typeIdentifier
                ?: throw IllegalStateException(machineClass.qualifiedName + " has no @MachineType")

    fun toJson() = GSON_SERIALIZER.toJson(this, this::class.java)

    companion object {
        val GSON_SERIALIZER = GsonBuilder()
                .serializeNulls()
                .registerTypeAdapter(ItemStack::class.java, ItemStackTypeAdapter())
                .registerTypeAdapter(MinecraftReflection.getCraftItemStackClass(), ItemStackTypeAdapter())
                .registerTypeAdapter(Location::class.java, LocationTypeAdapter())
                .create()

        fun fromJson(json: String, dataClass: KClass<out MachinePersistentData>) =
                GSON_SERIALIZER.fromJson(json, dataClass.java)
    }

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
