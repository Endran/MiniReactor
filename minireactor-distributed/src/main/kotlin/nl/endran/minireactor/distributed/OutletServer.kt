package nl.endran.minireactor.distributed

import com.fasterxml.jackson.databind.ObjectMapper
import nl.endran.minireactor.core.LocalMiniReactor
import nl.endran.minireactor.util.MiniLogger
import org.craftsmenlabs.socketoutlet.core.OutletRegistry
import org.craftsmenlabs.socketoutlet.core.initForSocketOutlet
import org.craftsmenlabs.socketoutlet.server.SocketOutletServer

class OutletServer(private val plantId: String,
                   private val miniReactor: LocalMiniReactor = LocalMiniReactor(),
                   private val outletRegistry: OutletRegistry = OutletRegistry(),
                   private val objectMapper: ObjectMapper = ObjectMapper().initForSocketOutlet(),
                   private val logger: MiniLogger = MiniLogger(MiniLogger.Level.DEBUG)) {

    var initialized = false
        private set
    var socketOutletServer: SocketOutletServer? = null
        private set

    private fun lazyInit() {
        if (!initialized) {

            val server = SocketOutletServer(outletRegistry, objectMapper, MiniLoggerCustom(logger))
            socketOutletServer = server

            miniReactor.reaction(OpenServer::class.java) {
                it
                        .filter { it.plantId == plantId }
                        .map {
                            if (!server.running) {
                                server.open(it.port)
                            }
                            return@map ServerOpened(it.plantId, server)
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

            server.clientConnectedCallback = {
                if (miniReactor.isClassSupported(ServerToClientConnectionEvent::class.java)) {
                    miniReactor.dispatch(ServerToClientConnectionEvent(it, ConnectionState.CONNECTED))
                }
            }

            server.clientDisconnectedCallback = {
                if (miniReactor.isClassSupported(ServerToClientConnectionEvent::class.java)) {
                    miniReactor.dispatch(ServerToClientConnectionEvent(it, ConnectionState.DISCONNECTED))
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
