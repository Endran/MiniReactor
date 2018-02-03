package nl.endran.minireactor.playground

import nl.endran.minireactor.plant.MiniReactorNetwork

open class MainClient {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {

            val miniReactor = MiniReactorNetwork("theClient")

            miniReactor.lurker(Pong::class.java)
                    .subscribe {
                        try {
                            System.out.println("Received Pong $it")
                        }catch (e:Exception) {
                            println(e.toString())
                        }
                    }

            LoggingReaction(miniReactor).start()

            miniReactor.start("127.0.0.1", 5001)

            miniReactor.dispatch(Ping("Here we go"))

            while (true) {
                // Do some otherstuff it you want
                Thread.sleep(1000)
            }
            miniReactor.close()
        }
    }
}
