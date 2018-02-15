package nl.endran.minireactor.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

class MiniLogger(val level: Level, val logTag: String = "") {

    enum class Level {
        VERBOSE, DEBUG, INFO, WARNING, ERROR, NONE
    }

    val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")

    fun v(message: () -> String) {
        v(null, message)
    }

    fun v(ex: Throwable?, message: () -> String) {
        if (Level.VERBOSE.ordinal >= level.ordinal) {
            log("VERBOSE", message, ex)
        }
    }

    fun d(message: () -> String) {
        d(null, message)
    }

    fun d(ex: Throwable?, message: () -> String) {
        if (Level.DEBUG.ordinal >= level.ordinal) {
            log("DEBUG  ", message, ex)
        }
    }

    fun i(message: () -> String) {
        i(null, message)
    }

    fun i(ex: Throwable?, message: () -> String) {
        if (Level.INFO.ordinal >= level.ordinal) {
            log("INFO   ", message, ex)
        }
    }

    fun w(message: () -> String) {
        w(null, message)
    }

    fun w(ex: Throwable?, message: () -> String) {
        if (Level.WARNING.ordinal >= level.ordinal) {
            log("WARNING", message, ex)
        }
    }

    fun e(message: () -> String) {
        e(null, message)
    }

    fun e(ex: Throwable?, message: () -> String) {
        if (Level.ERROR.ordinal >= level.ordinal) {
            log("ERROR  ", message, ex)
        }
    }

    private fun log(level: String, message: () -> String, ex: Throwable?) {
        println("${getTimeStamp()} $level (${getTag()}) ${logTag}: ${message.invoke()}${ex?.let { "\n$it" } ?: ""}")
    }

    private val ANONYMOUS_CLASS = Pattern.compile("(\\$\\d+)+$")
    private val CALL_STACK_INDEX = 2
    private fun createStackElementTag(element: StackTraceElement): String {
        var tag = element.className
        val m = ANONYMOUS_CLASS.matcher(tag)
        if (m.find()) {
            tag = m.replaceAll("")
        }
        return tag.substring(tag.lastIndexOf('.') + 1)
    }

    private fun getTag(): String {
        val stackTrace = Throwable().stackTrace
        if (stackTrace.size <= CALL_STACK_INDEX) {
            println("LOG ERROR: Synthetic stacktrace didn't have enough elements :(")
            return "GENERIC"
        }
        return createStackElementTag(stackTrace[CALL_STACK_INDEX])
    }

    private fun getTimeStamp(): String {
        return LocalDateTime.now().format(formatter)
    }
}
