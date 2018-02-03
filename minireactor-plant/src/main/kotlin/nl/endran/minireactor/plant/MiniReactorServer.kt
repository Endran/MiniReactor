package nl.endran.minireactor.plant

import com.fasterxml.jackson.databind.ObjectMapper
import nl.endran.minireactor.core.ConcreteMiniReactor
import org.craftsmenlabs.socketoutlet.core.OutletRegistry
import org.craftsmenlabs.socketoutlet.core.initForSocketOutlet
import org.craftsmenlabs.socketoutlet.core.log.CustomLogger
import org.craftsmenlabs.socketoutlet.server.SocketOutletServer

class MiniReactorServer(private val plantId: String,
                        private val miniReactor: ConcreteMiniReactor = ConcreteMiniReactor(),
                        private val outletRegistry: OutletRegistry = OutletRegistry(),
                        private val objectMapper: ObjectMapper = ObjectMapper().initForSocketOutlet(),
                        private val customLogger: CustomLogger = CustomLogger(CustomLogger.Level.DEBUG)) {

    var initialized = false
    var socketOutletServer: SocketOutletServer? = null

    private fun lazyInit() {
        if (!initialized) {

            val server = SocketOutletServer(outletRegistry, objectMapper, customLogger)
            socketOutletServer = server

            miniReactor.reaction(OpenServer::class.java) {
                it
                        .filter { it.plantId == plantId }
                        .map {
                            if (!server.running) {
                                server.open(it.port)
                            }
                            return@map ServerOpened(it.plantId, it.port)
                        }
            }

            miniReactor.reaction(CloseServer::class.java) {
                it
                        .filter { it.plantId == plantId }
                        .map {
                            if (server.running) {
                                server.close()
                            }
                            return@map ServerClosed(it.plantId)
                        }
            }

            miniReactor.lurkerForSequences(ConcreteMiniReactor.UnsupportedData::class.java)
                    .subscribe {
                        val payload = ObjectMapper().writeValueAsString(it.second.data!!)
                        server.sendToAll(Slug(it.second.data!!::class.java.name, payload, it.first))
                    }

            server.clientConnectedCallback = {
                if (miniReactor.isClassSupported(ClientConnectionState::class.java)) {
                    miniReactor.dispatch(ClientConnectionState(it, ConnectionState.CONNECTED))
                }
            }

            server.clientDisconnectedCallback = {
                if (miniReactor.isClassSupported(ClientConnectionState::class.java)) {
                    miniReactor.dispatch(ClientConnectionState(it, ConnectionState.DISCONNECTED))
                }
            }
        }
    }

    fun open(port: Int) {
        lazyInit()
        miniReactor.dispatch(OpenServer(plantId, port))
    }

    fun close() {
        miniReactor.dispatch(CloseServer(plantId))
    }
}
