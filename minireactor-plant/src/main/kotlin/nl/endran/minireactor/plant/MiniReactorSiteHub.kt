package nl.endran.minireactor.plant

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.reactivex.disposables.Disposable
import nl.endran.minireactor.core.ConcreteMiniReactor
import nl.endran.minireactor.core.MiniReactor
import org.craftsmenlabs.socketoutlet.core.*
import org.craftsmenlabs.socketoutlet.core.log.CustomLogger
import org.craftsmenlabs.socketoutlet.server.SocketOutletServer

class MiniReactorSiteHub(private val plantId: String,
                         private val miniReactor: ConcreteMiniReactor = ConcreteMiniReactor(),
                         private val outletRegistry: OutletRegistry = OutletRegistry(),
                         private val objectMapper: ObjectMapper = ObjectMapper().initForSocketOutlet().registerKotlinModule(),
                         private val customLogger: CustomLogger = CustomLogger(CustomLogger.Level.INFO),
                         private val outletServer: OutletServer = OutletServer(plantId, miniReactor, outletRegistry, objectMapper, customLogger)
) : MiniReactor by miniReactor {

    var initialized = false

    val clientMap = mutableMapOf<String, ((NetworkMessage) -> Unit)>()
    var server: SocketOutletServer? = null
    val disposables = mutableListOf<Disposable>()

    fun lazyInit() {
        if (!initialized) {

            outletRegistry.register(object : Outlet<ErrorMessage>(ErrorMessage::class.java) {
                override fun onMessage(sender: String, message: ErrorMessage, egress: Egress) {
                    customLogger.e { "Received error from $sender: $message" }
                }
            }) // TODO: unregister

            outletRegistry.register(object : Outlet<NetworkMessage>(NetworkMessage::class.java) {
                override fun onMessage(sender: String, message: NetworkMessage, egress: Egress) {
                    miniReactor.dispatch(HubMessage(sender, message))
                }
            }) // TODO: unregister

            miniReactor.lurker(HubMessage::class.java)
                    .subscribe { message ->
                        clientMap.entries
                                .filter { it.key != message.sender }
                                .forEach { it.value.invoke(message.networkMessage) }
                    }.let { disposables.add(it) }

            miniReactor.lurker(ServerOpened::class.java)
                    .subscribe {
                        server = it.server
                    }.let { disposables.add(it) }

            miniReactor.lurker(ServerClosed::class.java)
                    .subscribe {
                        server = null
                    }.let { disposables.add(it) }

            miniReactor.lurker(ServerToClientConnectionEvent::class.java)
                    .subscribe {
                        val clientId = it.clientId
                        if (it.connectionState == ConnectionState.CONNECTED) {
                            clientMap.put(clientId, { server?.send(clientId, it) })
                        } else {
                            clientMap.remove(clientId)
                        }
                    }.let { disposables.add(it) }

            miniReactor.lurkerForSequences(ConcreteMiniReactor.UnsupportedData::class.java)
                    .subscribe {
                        val payload = ObjectMapper().writeValueAsString(it.second.data!!)
                        val networkMessage = NetworkMessage(it.second.data!!::class.java.name, payload, it.first)
                        server?.sendToAll(networkMessage)
                    }.let { disposables.add(it) }

            initialized = true
        }
    }

    fun open(port: Int) {
        lazyInit()
        outletServer.open(port)
    }

    fun close() {
        disposables.forEach { it.dispose() }
        clientMap.clear()
        outletServer.close()
        server = null
    }
}