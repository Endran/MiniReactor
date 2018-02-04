package nl.endran.minireactor.util

import nl.endran.minireactor.core.MiniReactor

class LoggingReaction(val miniReactor: MiniReactor, val miniLogger: MiniLogger = MiniLogger(MiniLogger.Level.DEBUG)) {

    fun start() {
        miniReactor.lurkerForSequences(Object::class.java)
                .subscribe {
                    miniLogger.d { "Reactor: ${getId(it.first)} ${it.second}" }
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
