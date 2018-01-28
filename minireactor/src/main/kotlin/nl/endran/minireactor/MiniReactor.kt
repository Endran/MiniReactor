package nl.endran.minireactor

import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

class MiniReactor(private val scheduler: Scheduler = MiniReactor.reactorScheduler) {

    fun dispatch(any: Any) {
        reactor.onNext(any);
    }

    fun <T> register(clazz: Class<T>): Observable<T> {
        return reactor
                .observeOn(scheduler)
                .filter { it.javaClass.equals(clazz) }
                .map { it as T }
    }

    companion object {
        internal val reactor = PublishSubject.create<Any>();
        private val reactorScheduler = Schedulers.from { it.run() }
    }
}

fun <T> Observable<T>.react(): Disposable {
    return this.subscribe {
        MiniReactor.reactor.onNext(it as Any);
    }
}

fun <T> Observable<T>.reactOnce(): Disposable {
    return this.take(1).react()
}
