package dev.reactant.mechanism.persistent.repository

import dev.reactant.mechanism.ChunkInfo
import dev.reactant.mechanism.MachinePersistentData
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import org.bukkit.Chunk
import java.util.*

interface MachinePersistentDataRepository {
    fun get(uuid: UUID): Maybe<MachinePersistentData>

    fun getAll(chunk: ChunkInfo): Single<List<MachinePersistentData>>
    fun getAll(chunk: Chunk): Single<List<MachinePersistentData>> = getAll(ChunkInfo.fromChunk(chunk))

    fun insertOrUpdate(machineData: MachinePersistentData): Completable

    fun delete(uuid: UUID): Completable
    fun delete(machineData: MachinePersistentData) = delete(machineData.uuid)

    fun getAll(): Single<Map<ChunkInfo, List<MachinePersistentData>>>
}

