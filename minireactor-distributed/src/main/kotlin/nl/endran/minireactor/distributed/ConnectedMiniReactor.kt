package nl.endran.minireactor.distributed

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.google.gson.Gson
import io.reactivex.disposables.Disposable
import nl.endran.minireactor.core.LocalMiniReactor
import nl.endran.minireactor.core.MiniReactor
import org.craftsmenlabs.socketoutlet.client.SocketOutletClient
import org.craftsmenlabs.socketoutlet.core.*
import org.craftsmenlabs.socketoutlet.core.log.CustomLogger

class ConnectedMiniReactor(private val plantId: String,
                           private val miniReactor: LocalMiniReactor = LocalMiniReactor(),
                           private val outletRegistry: OutletRegistry = OutletRegistry(),
                           private val objectMapper: ObjectMapper = ObjectMapper().initForSocketOutlet().registerKotlinModule(),
                           private val customLogger: CustomLogger = CustomLogger(CustomLogger.Level.INFO),
                           private val outletClient: OutletClient = OutletClient(plantId, miniReactor, outletRegistry, objectMapper, customLogger)
) : MiniReactor by miniReactor {

    private var initialized = false
    private val disposables = mutableListOf<Disposable>()
    private val clientMap = mutableMapOf<String, SocketOutletClient>()
    private val gson = Gson() // TODO: Use GSON everywhere

    private fun lazyInit() {
        if (!initialized) {

            outletRegistry.register(object : Outlet<ErrorMessage>(ErrorMessage::class.java) {
                override fun onMessage(sender: String, message: ErrorMessage, egress: Egress) {
                    customLogger.e { message.toString() }
                }
            }) // TODO: unregister

            outletRegistry.register(object : Outlet<NetworkMessage>(NetworkMessage::class.java) {
                override fun onMessage(sender: String, message: NetworkMessage, egress: Egress) {
                    val clazz = Class.forName(message.type)
                    val event = gson.fromJson(message.payload, clazz)
                    if (miniReactor.isClassSupported(clazz)) {
                        miniReactor.dispatch(event, message.id)
                    }
                }
            }) // TODO: unregister

            miniReactor.lurker(ClientStarted::class.java)
                    .subscribe {
                        val client = it.client
                        clientMap.put(it.ipAddress + it.port, client)
                    }.let { disposables.add(it) }

            miniReactor.lurker(ClientStopped::class.java)
                    .subscribe {
                        clientMap.remove(it.ipAddress + it.port)
                    }.let { disposables.add(it) }

            miniReactor.lurker(ClientToServerConnectionEvent::class.java)
                    .subscribe {
                        if (it.connectionState == ConnectionState.CONNECTED) {
                            val client = it.client
                            clientMap.put(it.ipAddress + it.port, client)
                        } else {
                            clientMap.remove(it.ipAddress + it.port)
                        }
                    }.let { disposables.add(it) }

            miniReactor.lurkerForSequences(LocalMiniReactor.UnsupportedData::class.java)
                    .subscribe {
                        val payload = ObjectMapper().writeValueAsString(it.second.data!!)
                        val networkMessage = NetworkMessage(it.second.data!!::class.java.name, payload, it.first)
                        clientMap.values.forEach { it.send(networkMessage) }
                    }.let { disposables.add(it) }

            initialized = true
        }
    }

    fun start(ipAddress: String, port: Int) {
        lazyInit()
        outletClient.start(ipAddress, port)
    }

    fun stop(ipAddress: String, port: Int) {
        disposables.forEach { it.dispose() }
        clientMap.values.forEach { it.stop() }
        clientMap.clear()
    }
}
