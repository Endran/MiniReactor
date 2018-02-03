package nl.endran.minireactor.plant

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.google.gson.Gson
import nl.endran.minireactor.core.ConcreteMiniReactor
import nl.endran.minireactor.core.MiniReactor
import org.craftsmenlabs.socketoutlet.core.*
import org.craftsmenlabs.socketoutlet.core.log.CustomLogger

class MiniReactorNetwork(private val plantId: String,
                         private val miniReactor: ConcreteMiniReactor = ConcreteMiniReactor(),
                         private val outletRegistry: OutletRegistry = OutletRegistry(),
                         private val objectMapper: ObjectMapper = ObjectMapper().initForSocketOutlet().registerKotlinModule(),
                         private val customLogger: CustomLogger = CustomLogger(CustomLogger.Level.DEBUG),
                         private val miniReactorServer: MiniReactorServer = MiniReactorServer(plantId, miniReactor, outletRegistry, objectMapper, customLogger),
                         private val miniReactorClient: MiniReactorClient = MiniReactorClient(plantId, miniReactor, outletRegistry, objectMapper, customLogger)
) : MiniReactor by miniReactor {

    var initialized = false

    val gson = Gson() // TODO: Use GSON everywhere

    fun lazyInit() {
        if (!initialized) {

            outletRegistry.register(object : Outlet<ErrorMessage>(ErrorMessage::class.java) {
                override fun onMessage(message: ErrorMessage, egress: Egress) {
                    customLogger.e { message.toString() }
                }
            })

            outletRegistry.register(object : Outlet<Slug>(Slug::class.java) {
                override fun onMessage(message: Slug, egress: Egress) {
                    val clazz = Class.forName(message.type)
                    val event = gson.fromJson(message.payload, clazz)
                    if (miniReactor.isClassSupported(clazz)) {
                        miniReactor.dispatch(event, message.id)
                    }
                }
            })

            initialized = true
        }
    }

    fun open(port: Int) {
        lazyInit()
        miniReactorServer.open(port)
    }

    fun close() {
        miniReactorServer.close()
    }

    fun start(ipAddress: String, port: Int) {
        lazyInit()
        miniReactorClient.start(ipAddress, port)
    }

    fun close(ipAddress: String, port: Int) {
        miniReactorClient.close(ipAddress, port)
    }
}