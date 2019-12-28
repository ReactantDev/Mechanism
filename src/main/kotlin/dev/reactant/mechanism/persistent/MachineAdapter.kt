package dev.reactant.mechanism.persistent

import dev.reactant.mechanism.Machine
import dev.reactant.mechanism.MachineAdapterOf
import dev.reactant.mechanism.MachinePersistentData
import dev.reactant.mechanism.MachineType
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

/**
 * The adapter that use to convert between machine instance and data instance
 * @param T The machine
 * @param S The data
 */
interface MachineAdapter<T : Machine, S : MachinePersistentData> {
    /**
     * Convert as Data
     */
    fun store(machine: T): S

    /**
     * Convert as Machine
     */
    fun restore(data: S): T

    private val machineAdapterOfAnnotation
        get() = this::class.findAnnotation<MachineAdapterOf>()
                ?: throw IllegalStateException(this::class.qualifiedName + " has no @MachineDataOf")

    val machineTypeIdentifier
        get() = machineAdapterOfAnnotation.run {
            machineClass.findAnnotation<MachineType>()?.typeIdentifier
                    ?: throw IllegalStateException(machineClass.qualifiedName + " has no @MachineIdentifier")
        }

    val dataClass: KClass<out MachinePersistentData> get() = machineAdapterOfAnnotation.dataClass

}
