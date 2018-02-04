package nl.endran.minireactor.distributed

import com.fasterxml.jackson.databind.ObjectMapper
import nl.endran.minireactor.core.LocalMiniReactor
import nl.endran.minireactor.util.MiniLogger
import org.craftsmenlabs.socketoutlet.client.SocketOutletClient
import org.craftsmenlabs.socketoutlet.core.OutletRegistry
import org.craftsmenlabs.socketoutlet.core.initForSocketOutlet

class OutletClient(private val plantId: String,
                   private val miniReactor: LocalMiniReactor,
                   private val outletRegistry: OutletRegistry,
                   private val objectMapper: ObjectMapper = ObjectMapper().initForSocketOutlet(),
                   private val logger: MiniLogger = MiniLogger(MiniLogger.Level.DEBUG)) {

    private var initialized = false
    private val clientMap = mutableMapOf<String, SocketOutletClient>()

    private fun lazyInit() {
        if (!initialized) {

            miniReactor.reaction(StartClient::class.java) {
                it.filter { it.plantId == plantId }
                        .map {
                            val client = getClient(it.ipAddress, it.port)
                            if (!client.isRunning()) {
                                client.start(it.ipAddress, it.port)
                            }
                            return@map ClientStarted(it.plantId, it.ipAddress, it.port, client)
                        }
            }

            miniReactor.reaction(StopClient::class.java) {
                it.filter { it.plantId == plantId }
                        .map {
                            val client = getClient(it.ipAddress, it.port)
                            if (client.isRunning()) {
                                client.stop()
                                clientMap.remove(it.ipAddress + it.port)
                            }
                            return@map ClientStopped(it.plantId, it.ipAddress, it.port)
                        }
            }

            initialized = true
        }
    }

    private fun getClient(ipAddress: String, port: Int): SocketOutletClient {
        return clientMap.getOrPut(ipAddress + port) {
            val client = SocketOutletClient(plantId, outletRegistry, objectMapper, MiniLoggerCustom(logger))

            client.serverConnectedCallback = {
                if (miniReactor.isClassSupported(ClientToServerConnectionEvent::class.java)) {
                    miniReactor.dispatch(ClientToServerConnectionEvent(ipAddress, port, client, ConnectionState.CONNECTED))
                }
            }

            client.serverDisconnectedCallback = {
                if (miniReactor.isClassSupported(ClientToServerConnectionEvent::class.java)) {
                    miniReactor.dispatch(ClientToServerConnectionEvent(ipAddress, port, client, ConnectionState.DISCONNECTED))
                }
            }
            return@getOrPut client
        }
    }

    fun start(ipAddress: String, port: Int) {
        lazyInit()

        miniReactor.dispatch(StartClient(plantId, ipAddress, port))

        // something something reconnect
    }

    fun stop(ipAddress: String, port: Int) {
        miniReactor.dispatch(StopClient(plantId, ipAddress, port))
    }
}
