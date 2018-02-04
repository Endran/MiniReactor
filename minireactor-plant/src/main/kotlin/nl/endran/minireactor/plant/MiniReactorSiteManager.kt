package nl.endran.minireactor.plant

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.google.gson.Gson
import nl.endran.minireactor.core.ConcreteMiniReactor
import nl.endran.minireactor.core.MiniReactor
import org.craftsmenlabs.socketoutlet.core.*
import org.craftsmenlabs.socketoutlet.core.log.CustomLogger
import org.craftsmenlabs.socketoutlet.server.SocketOutletServer

class MiniReactorSiteManager(private val plantId: String,
                             private val miniReactor: ConcreteMiniReactor = ConcreteMiniReactor(),
                             private val outletRegistry: OutletRegistry = OutletRegistry(),
                             private val objectMapper: ObjectMapper = ObjectMapper().initForSocketOutlet().registerKotlinModule(),
                             private val customLogger: CustomLogger = CustomLogger(CustomLogger.Level.VERBOSE),
                             private val outletServer: OutletServer = OutletServer(plantId, miniReactor, outletRegistry, objectMapper, customLogger),
                             private val outletClient: OutletClient = OutletClient(plantId, miniReactor, outletRegistry, objectMapper, customLogger)
) : MiniReactor by miniReactor {

    var initialized = false

    val gson = Gson() // TODO: Use GSON everywhere
    // TODO: Make a combined list of server and clients based on their ID (don't use duplicates)
    // Send messages to all entries on that list
    // Then extend first hello call with supported classes, and roundtrip IP+Port

    // Make a site manager responsible form bridging events between sites

    fun lazyInit() {
        if (!initialized) {

            outletRegistry.register(object : Outlet<ErrorMessage>(ErrorMessage::class.java) {
                override fun onMessage(sender: String, message: ErrorMessage, egress: Egress) {
                    customLogger.e { "Received error from $sender: $message" }
                }
            })

            outletRegistry.register(object : Outlet<NetworkMessage>(NetworkMessage::class.java) {
                override fun onMessage(sender: String, message: NetworkMessage, egress: Egress) {
                    val clazz = Class.forName(message.type)
                    val event = gson.fromJson(message.payload, clazz)
                    if (miniReactor.isClassSupported(clazz)) {
                        miniReactor.dispatch(event, message.id)
                    }
                }
            })

            var server: SocketOutletServer? = null
            miniReactor.lurker(ServerOpened::class.java)
                    .subscribe {
                        server = it.server
                    }
            miniReactor.lurker(ServerClosed::class.java)
                    .subscribe {
                        server = null
                    }
            miniReactor.lurker(ServerToClientConnectionEvent::class.java)
                    .subscribe {
                    }

            miniReactor.lurker(ClientStarted::class.java)
                    .subscribe {
                    }

            miniReactor.lurker(ClientStopped::class.java)
                    .subscribe {
                    }

            miniReactor.lurker(ClientToServerConnectionEvent::class.java)
                    .subscribe {
                    }

            miniReactor.lurkerForSequences(ConcreteMiniReactor.UnsupportedData::class.java)
                    .subscribe {
                        val payload = ObjectMapper().writeValueAsString(it.second.data!!)
                        val networkMessage = NetworkMessage(it.second.data!!::class.java.name, payload, it.first)
                        server?.sendToAll(networkMessage)
                    }

            initialized = true
        }
    }

    fun open(port: Int) {
        lazyInit()
        outletServer.open(port)
    }

    fun close() {
        outletServer.close()
    }

    fun start(ipAddress: String, port: Int) {
        lazyInit()
        outletClient.start(ipAddress, port)
    }

    fun close(ipAddress: String, port: Int) {
        outletClient.close(ipAddress, port)
    }
}
