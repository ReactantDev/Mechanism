package dev.reactant.mechanism.persistent.repository

import com.comphenix.protocol.utility.MinecraftReflection
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapterFactory
import dev.reactant.mechanism.ChunkInfo
import dev.reactant.mechanism.MachinePersistentData
import dev.reactant.mechanism.Mechanism
import dev.reactant.mechanism.persistent.MachineAdaptersService
import dev.reactant.mechanism.serialize.ItemStackTypeAdapter
import dev.reactant.mechanism.serialize.LocationTypeAdapter
import dev.reactant.reactant.core.component.Component
import dev.reactant.reactant.core.component.lifecycle.LifeCycleHook
import dev.reactant.reactant.core.dependency.injection.components.Components
import dev.reactant.reactant.extra.parser.gsonadapters.TypeAdapterPair
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import org.bukkit.Location
import org.bukkit.inventory.ItemStack
import java.io.File
import java.sql.*
import java.util.*

@Component
class SQLiteMachinePersistentDataRepository(
        private val machineAdaptersService: MachineAdaptersService,
        private val typeAdapterFactories: Components<TypeAdapterFactory>,
        private val typeAdapters: Components<TypeAdapterPair>
) : MachinePersistentDataRepository, LifeCycleHook {
    private val tableName = "machine"
    private lateinit var connection: Connection
    private lateinit var preparedStatements: PreparedStatements

    private val gson = GsonBuilder()
            .also { builder -> typeAdapterFactories.forEach { builder.registerTypeAdapterFactory(it) } }
            .also { builder -> typeAdapters.forEach { builder.registerTypeAdapter(it.type, it.typeAdapter) } }
            .registerTypeAdapter(ItemStack::class.java, ItemStackTypeAdapter())
            .registerTypeAdapter(MinecraftReflection.getCraftItemStackClass(), ItemStackTypeAdapter())
            .registerTypeAdapter(Location::class.java, LocationTypeAdapter())
            .serializeNulls()
            .create()


    class PreparedStatements(connection: Connection) {
        val insertOrUpdate: PreparedStatement =
                connection.prepareStatement("REPLACE INTO machine(uuid, type_identifier, chunk_worldUUID, chunk_x, chunk_z, data) VALUES (?,?,?,?,?,?);")
        val get: PreparedStatement =
                connection.prepareStatement("SELECT * FROM machine WHERE uuid = ?;")
        val getAllByChunk: PreparedStatement =
                connection.prepareStatement("SELECT * FROM machine WHERE chunk_worldUUID = ? AND chunk_x = ? AND chunk_z = ?;")
        val getAll: PreparedStatement =
                connection.prepareStatement("SELECT * FROM  machine;")
        val delete: PreparedStatement =
                connection.prepareStatement("DELETE FROM machine WHERE uuid = ?;")
    }


    override fun onEnable() {
        try {
            val file = File("plugins/Reactant/Mechanism/persistent-data.db")
            file.parentFile.mkdirs()
            val address = "jdbc:sqlite:${file.absolutePath}"
            connection = DriverManager.getConnection(address)
            initializeTableIfNotExist()
            preparedStatements = PreparedStatements(connection)

        } catch (e: SQLException) {
            throw IllegalStateException("SQLite machine persistent data repository cannot be initialized", e)
        }
    }

    private fun initializeTableIfNotExist() {
        val tableExist = connection.createStatement().executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='$tableName';").next()

        if (!tableExist) {
            connection.createStatement().let { statement ->
                statement.executeUpdate(
                        """
                            CREATE TABLE machine(
                                uuid TEXT PRIMARY KEY,
                                type_identifier TEXT,
                                chunk_worldUUID TEXT,
                                chunk_x INTEGER ,
                                chunk_z INTEGER ,
                                data TEXT
                            );
                         """
                )

                statement.executeUpdate("CREATE INDEX machine_chunk_worldUUID ON machine(chunk_worldUUID);")
                statement.executeUpdate("CREATE INDEX machine_chunk_x ON machine(chunk_x);")
                statement.executeUpdate("CREATE INDEX machine_chunk_z ON machine(chunk_z);")
            }
        }
    }

    private fun readResultSet(resultSet: ResultSet): Sequence<MachinePersistentData> = generateSequence {
        while (resultSet.next()) {
            val type = resultSet.getString("type_identifier")
            val dataClass = machineAdaptersService.getAdapter(type)?.dataClass
            if (dataClass == null) {
                Mechanism.logger.warn("Adapter for machine(type_identifier=${type}) not found!")
                continue; // ignore and go next result
            } else {
                return@generateSequence resultSet.getString("data")
                        .let { gson.fromJson(it, dataClass.java) }
            }
        }
        return@generateSequence null
    }

    override fun get(uuid: UUID): Maybe<MachinePersistentData> = Maybe.create { emitter ->
        preparedStatements.get.runCatching {
            setString(1, uuid.toString())
            val results = readResultSet(executeQuery())

            results.firstOrNull()?.let { emitter.onSuccess(it) } ?: emitter.onComplete()
        }.onFailure { emitter.onError(it) }
    }

    override fun getAll(chunk: ChunkInfo): Single<List<MachinePersistentData>> {
        return Single.create { emitter ->
            preparedStatements.getAllByChunk.runCatching {
                setString(1, chunk.worldUUID.toString())
                setInt(2, chunk.x)
                setInt(3, chunk.z)
                val results = readResultSet(executeQuery())

                emitter.onSuccess(results.toList())
            }.onFailure { emitter.onError(it) }
        }
    }

    override fun getAll(): Single<Map<ChunkInfo, List<MachinePersistentData>>> = Single.create { emitter ->
        preparedStatements.getAll.runCatching {
            val results = readResultSet(executeQuery())
            emitter.onSuccess(results.groupBy { it.chunk })
        }.onFailure { emitter.onError(it) }
    }

    override fun insertOrUpdate(machineData: MachinePersistentData): Completable = Completable.create { emitter ->
        preparedStatements.insertOrUpdate.runCatching {
            setString(1, machineData.uuid.toString())
            setString(2, machineData.machineTypeIdentifier)
            setString(3, machineData.chunk.worldUUID.toString())
            setString(4, machineData.chunk.x.toString())
            setString(5, machineData.chunk.z.toString())
            setString(6, gson.toJson(machineData))
            executeUpdate()
        }.onFailure {
            emitter.onError(it)
        }
    }

    override fun delete(uuid: UUID): Completable = Completable.create { emitter ->
        preparedStatements.delete.runCatching {
            setString(1, uuid.toString())
            executeUpdate()
        }.onFailure { emitter.onError(it) }
    }

}
