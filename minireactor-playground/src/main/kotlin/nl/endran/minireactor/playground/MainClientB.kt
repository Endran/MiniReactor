package nl.endran.minireactor.playground

import nl.endran.minireactor.distributed.ClientToServerConnectionEvent
import nl.endran.minireactor.distributed.ConnectedMiniReactor
import nl.endran.minireactor.distributed.ConnectionState
import nl.endran.minireactor.util.LoggingReaction
import nl.endran.minireactor.util.MiniLogger
import java.util.concurrent.TimeUnit

open class MainClientB {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {

            val logger = MiniLogger(MiniLogger.Level.DEBUG)
            val miniReactor = ConnectedMiniReactor("theClientB")

            miniReactor.reaction(Ping::class.java) {
                it.map { logger.i { "Received Ping $it" } }
                        .delay(1, TimeUnit.SECONDS)
                        .map { Pong("Pong from B") }
            }

            LoggingReaction(miniReactor, logger).start()

            miniReactor.start("127.0.0.1", 5000)

            miniReactor.lurker(ClientToServerConnectionEvent::class.java)
                    .filter { it.connectionState == ConnectionState.CONNECTED }
                    .take(1)
                    .subscribe { miniReactor.dispatch(Pong("Here we go")) }

            while (true) {
                // Do some otherstuff it you want
                Thread.sleep(1000)
            }
        }
    }
}
