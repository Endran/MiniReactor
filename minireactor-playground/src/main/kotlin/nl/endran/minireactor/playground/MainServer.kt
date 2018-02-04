package nl.endran.minireactor.playground

import nl.endran.minireactor.plant.MiniReactorSiteManager

open class MainServer {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {

            val miniReactor = MiniReactorSiteManager("theServer")

            miniReactor.reaction(Ping::class.java) {
                it.map { Pong("Replying to ${it.message}") }
            }
            
            LoggingReaction(miniReactor).start()

            miniReactor.open(5001)
            while (true) {
                Thread.sleep(1000)
            }
            miniReactor.close()
        }
    }
}
