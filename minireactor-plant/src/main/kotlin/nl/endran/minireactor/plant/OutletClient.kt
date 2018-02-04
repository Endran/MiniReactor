package nl.endran.minireactor.plant

import com.fasterxml.jackson.databind.ObjectMapper
import nl.endran.minireactor.core.LocalMiniReactor
import org.craftsmenlabs.socketoutlet.client.SocketOutletClient
import org.craftsmenlabs.socketoutlet.core.OutletRegistry
import org.craftsmenlabs.socketoutlet.core.initForSocketOutlet
import org.craftsmenlabs.socketoutlet.core.log.CustomLogger

class OutletClient(private val plantId: String,
                   private val miniReactor: LocalMiniReactor,
                   private val outletRegistry: OutletRegistry,
                   private val objectMapper: ObjectMapper = ObjectMapper().initForSocketOutlet(),
                   private val customLogger: CustomLogger = CustomLogger(CustomLogger.Level.DEBUG)) {

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
            val client = SocketOutletClient(plantId, outletRegistry, objectMapper, customLogger)

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
    }

    fun stop(ipAddress: String, port: Int) {
        miniReactor.dispatch(StopClient(plantId, ipAddress, port))
    }
}
