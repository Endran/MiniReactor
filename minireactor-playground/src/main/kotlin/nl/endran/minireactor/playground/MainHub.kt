package nl.endran.minireactor.playground

import nl.endran.minireactor.plant.MiniReactorSiteHub

open class MainHub {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {

            val miniReactor = MiniReactorSiteHub("theHub")

            LoggingReaction(miniReactor).start()

            miniReactor.open(5000)
            while (true) {
                Thread.sleep(1000)
            }
            miniReactor.close()
        }
    }
}
