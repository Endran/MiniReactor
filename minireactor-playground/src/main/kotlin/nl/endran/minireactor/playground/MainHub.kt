package nl.endran.minireactor.playground

import nl.endran.minireactor.plant.MiniReactorSiteHub
import org.craftsmenlabs.socketoutlet.core.log.CustomLogger

open class MainHub {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {

            val miniReactor = MiniReactorSiteHub("theHub")

            val logger = CustomLogger(CustomLogger.Level.DEBUG)
            LoggingReaction(miniReactor, logger).start()

            miniReactor.open(5000)
            while (true) {
                Thread.sleep(1000)
            }
            miniReactor.close()
        }
    }
}
