package nl.endran.minireactor.playground

import nl.endran.minireactor.distributed.HubMiniReactor
import nl.endran.minireactor.util.LoggingReaction
import nl.endran.minireactor.util.MiniLogger

open class MainHub {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {

            val miniReactor = HubMiniReactor("theHub")

            val logger = MiniLogger(MiniLogger.Level.DEBUG)
            LoggingReaction(miniReactor, logger).start()

            miniReactor.open(5000)
            while (true) {
                Thread.sleep(1000)
            }
            miniReactor.close()
        }
    }
}
