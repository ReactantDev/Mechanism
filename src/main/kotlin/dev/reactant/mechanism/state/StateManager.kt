package dev.reactant.mechanism.state

import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.Subject
import java.util.*

class StateManager : StateHolder {

    /**
     * Should all state be complete when holder complete
     */
    private val states: HashSet<Subject<*>> = hashSetOf()

    /**
     * Automatically depose behavior subject
     */
    fun <T> defaultState(state: T): BehaviorSubject<T> = BehaviorSubject.createDefault(state)
            .also { states.add(it) }
            .also { it.doOnComplete { states.remove(it) } }

    override fun completeAllStates() = states.forEach { it.onComplete() }
}

interface StateHolder {
    /**
     * Complete all holding states
     */
    fun completeAllStates()
}
