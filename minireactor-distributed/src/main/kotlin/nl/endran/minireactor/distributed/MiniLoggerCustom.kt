package nl.endran.minireactor.distributed

import nl.endran.minireactor.util.MiniLogger
import org.craftsmenlabs.socketoutlet.core.log.SLogger

internal class MiniLoggerCustom(val miniLogger: MiniLogger = MiniLogger(MiniLogger.Level.INFO)) : SLogger {
    override fun d(message: () -> String) = miniLogger.d(message)

    override fun e(message: () -> String) = miniLogger.e(message)

    override fun i(message: () -> String) = miniLogger.i(message)

    override fun v(message: () -> String) = miniLogger.v(message)

    override fun w(message: () -> String) = miniLogger.w(message)
}
