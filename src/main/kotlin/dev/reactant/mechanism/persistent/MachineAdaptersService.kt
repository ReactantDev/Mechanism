package dev.reactant.mechanism.persistent

import dev.reactant.mechanism.Machine
import dev.reactant.mechanism.MachinePersistentData
import dev.reactant.reactant.core.component.Component
import dev.reactant.reactant.core.component.lifecycle.LifeCycleHook
import dev.reactant.reactant.core.dependency.injection.components.Components
import kotlin.reflect.KClass

@Component
class MachineAdaptersService(
        private val adaptersInfo: Components<MachineAdapter<*, *>>
) : LifeCycleHook {

    private val machineTypeAdaptersMap: Map<String, MachineAdapter<*, *>> = adaptersInfo.map { it.machineTypeIdentifier to it }.toMap()

    fun getAdapter(typeIdentifier: String) = machineTypeAdaptersMap[typeIdentifier]
    fun <T : Machine> getAdapter(machine: T) = getAdapter(machine.typeIdentifier) as MachineAdapter<T, *>?
    fun <T : MachinePersistentData> getAdapter(machineData: T) = getAdapter(machineData.machineTypeIdentifier) as MachineAdapter<*, T>?

    override fun onEnable() {
    }

    override fun onSave() {
    }

    class MachineDataInfo<T : Machine, S : MachinePersistentData>(
            val typeIdentifier: String,
            val machineAdapter: MachineAdapter<T, S>,
            val dataClass: KClass<S>
    )
}
