package nl.endran.minireactor.playground

import nl.endran.minireactor.core.MiniReactor

class LoggingReaction(val miniReactor: MiniReactor) {

    fun start() {
        miniReactor.lurkerForSequences(Object::class.java)
                .subscribe {
                    System.out.println("Reactor: ${getId(it.first)} ${it.second}")
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
