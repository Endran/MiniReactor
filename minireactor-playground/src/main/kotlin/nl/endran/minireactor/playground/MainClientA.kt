package nl.endran.minireactor.playground

import nl.endran.minireactor.plant.ConnectedMiniReactor
import org.craftsmenlabs.socketoutlet.core.log.CustomLogger
import java.util.concurrent.TimeUnit

open class MainClientA {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {

            val logger = CustomLogger(CustomLogger.Level.DEBUG)
            val miniReactor = ConnectedMiniReactor("theClientA")

            miniReactor.reaction(Pong::class.java) {
                it.map { logger.i { "Received Pong $it" } }
                        .delay(1, TimeUnit.SECONDS)
                        .map { Ping("Ping from A") }
            }

            LoggingReaction(miniReactor, logger).start()

            miniReactor.start("127.0.0.1", 5000)

            miniReactor.dispatch(Ping("Here we go"))

            while (true) {
                // Do some otherstuff it you want
                Thread.sleep(1000)
            }
        }
    }
}
