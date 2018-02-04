package nl.endran.minireactor.playground

import nl.endran.minireactor.core.MiniReactor
import org.craftsmenlabs.socketoutlet.core.log.CustomLogger

class LoggingReaction(val miniReactor: MiniReactor, val customLogger: CustomLogger = CustomLogger(CustomLogger.Level.INFO)) {

    fun start() {
        miniReactor.lurkerForSequences(Object::class.java)
                .subscribe {
                    customLogger.d { "Reactor: ${getId(it.first)} ${it.second}" }
                }
    }

    companion object {
        val sixteenDots = "................"

        private fun getId(id: String): String {
            return if (id.length < 16) {
                "$id$sixteenDots:".substring(0, 16)
            } else {
                "$id:"
            }
        }
    }
}
