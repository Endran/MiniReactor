package nl.endran.minireactor.plant

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.google.gson.Gson
import nl.endran.minireactor.core.ConcreteMiniReactor
import nl.endran.minireactor.core.MiniReactor
import org.craftsmenlabs.socketoutlet.core.*
import org.craftsmenlabs.socketoutlet.core.log.CustomLogger

class MiniReactorSiteNode(private val plantId: String,
                          private val miniReactor: ConcreteMiniReactor = ConcreteMiniReactor(),
                          private val outletRegistry: OutletRegistry = OutletRegistry(),
                          private val objectMapper: ObjectMapper = ObjectMapper().initForSocketOutlet().registerKotlinModule(),
                          private val customLogger: CustomLogger = CustomLogger(CustomLogger.Level.VERBOSE),
                          private val miniReactorServer: MiniReactorServer = MiniReactorServer(plantId, miniReactor, outletRegistry, objectMapper, customLogger),
                          private val miniReactorClient: MiniReactorClient = MiniReactorClient(plantId, miniReactor, outletRegistry, objectMapper, customLogger)
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
                    customLogger.e { message.toString() }
                }
            })

            outletRegistry.register(object : Outlet<Slug>(Slug::class.java) {
                override fun onMessage(sender: String, message: Slug, egress: Egress) {
                    val clazz = Class.forName(message.type)
                    val event = gson.fromJson(message.payload, clazz)
                    if (miniReactor.isClassSupported(clazz)) {
                        miniReactor.dispatch(event, message.id)
                    }
                }
            })

            val clientMap = mutableMapOf<String, ((Slug) -> Unit)>()
            miniReactor.lurker(ClientStarted::class.java)
                    .subscribe {
                        val client = it.client
                        clientMap.put(it.ipAddress + it.port, { client.send(it) })
                    }

            miniReactor.lurker(ClientStopped::class.java)
                    .subscribe {
                        clientMap.remove(it.ipAddress + it.port)
                    }

            miniReactor.lurker(ClientToServerConnectionEvent::class.java)
                    .subscribe {
                        if (it.connectionState == ConnectionState.CONNECTED) {
                            val client = it.client
                            clientMap.put(it.ipAddress + it.port, { client.send(it) })
                        } else {
                            clientMap.remove(it.ipAddress + it.port)
                        }
                    }

            miniReactor.lurkerForSequences(ConcreteMiniReactor.UnsupportedData::class.java)
                    .subscribe {
                        val payload = ObjectMapper().writeValueAsString(it.second.data!!)
                        val slug = Slug(it.second.data!!::class.java.name, payload, it.first)
                        clientMap.values.forEach { it.invoke(slug) }
                    }

            initialized = true
        }
    }

    fun start(ipAddress: String, port: Int) {
        lazyInit()
        miniReactorClient.start(ipAddress, port)
    }

    fun close(ipAddress: String, port: Int) {
        miniReactorClient.close(ipAddress, port)
    }
}
