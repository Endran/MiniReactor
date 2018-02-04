package nl.endran.minireactor.playground

import nl.endran.minireactor.plant.MiniReactorSiteNode

open class MainClientB {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {

            val miniReactor = MiniReactorSiteNode("theClientB")

            miniReactor.lurker(Ping::class.java)
                    .subscribe {
                        try {
                            System.out.println("Received Ping $it")
                        }catch (e:Exception) {
                            println(e.toString())
                        }
                    }

            LoggingReaction(miniReactor).start()

            miniReactor.start("127.0.0.1", 5000)

            miniReactor.dispatch(Pong("Here we go"))

            while (true) {
                // Do some otherstuff it you want
                Thread.sleep(1000)
            }
        }
    }
}
