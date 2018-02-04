package nl.endran.minireactor.playground

import nl.endran.minireactor.distributed.ConnectedMiniReactor
import nl.endran.minireactor.util.LoggingReaction
import nl.endran.minireactor.util.MiniLogger
import java.util.concurrent.TimeUnit

open class MainClientA {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {

            val logger = MiniLogger(MiniLogger.Level.DEBUG)
            val miniReactor = ConnectedMiniReactor("theClientA")

            miniReactor.reaction(Pong::class.java) {
                it.map { logger.i { "Received Pong $it" } }
                        .delay(1, TimeUnit.SECONDS)
                        .map { Ping("Ping from A") }
            }

            logger.w(RuntimeException("test")) { "Just a test" }

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
