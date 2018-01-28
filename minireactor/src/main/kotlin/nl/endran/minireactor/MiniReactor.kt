package nl.endran.minireactor

import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * A very lightweight and thread-safe implementation of the Reactor Pattern, with RxJava2. MiniReactor takes in events
 * of any type, from any thread, and demultiplexes them into a single thread. Interested classes can register to
 * MiniReactor to obtain those events, on the Reactor thread. Classes should never block the Reactor thread.
 *
 * @constructor
 * @param reactorScheduler
 *          The Reactor Scheduler, will default to a [Scheduler] from [Executors.newSingleThreadExecutor()]
 */
class MiniReactor(private val reactorScheduler: Scheduler = MiniReactor.reactorScheduler) {

    /**
     * Dispatches an event to the reactor. The reactor will output any event on the reactorScheduler thread.
     *
     * @param event
     *            The event to dispatch onto the reactor.
     */
    fun dispatch(event: Any) {
        reactor.onNext(event);
    }

    /**
     * Register for specific events from the reactor. Events will be emitted on the reactorScheduler thread.
     *
     * **Warning:** Do not block this thread. Blocking calls must be lifted from this thread to another tread,
     * use for example [Observable.observeOn(Schedulers.io())].
     *
     * @param clazz
     *            The Class of the event to receive from the reactor.
     * @return The [Observable] for the specific events, already casted to the correct type.
     */
    fun <T> register(clazz: Class<T>): Observable<T> {
        return reactor
                .observeOn(reactorScheduler)
                .filter { clazz.isInstance(it) }
                .map { it as T }
    }

    /**
     * Similiar to [register], will register for specific events from the reactor. Events will be emitted on the
     * reactorScheduler thread. On the first subscribe the event will be dispatch, similar to [dispatch].
     *
     * **Warning:** Do not block this thread. Blocking calls must be lifted from this thread to another tread,
     * use for example [Observable.observeOn(Schedulers.io())].
     *
     * @param event
     *            The event to dispatch onto the reactor.
     * @param clazz
     *            The Class of the event to receive from the reactor.
     * @return The [Observable] for the specific events, already casted to the correct type.
     */
    fun <T> registerAndDispatch(event: Any, clazz: Class<T>): Observable<T> {
        var once = false
        return register(clazz).doOnSubscribe {
            if (!once) {
                once = true
                Observable.just(event).delay(1, TimeUnit.MICROSECONDS).subscribe { dispatch(it) }
            }
        }
    }

    companion object {
        internal val reactor = PublishSubject.create<Any>();
        private val reactorScheduler = Schedulers.from { Executors.newSingleThreadExecutor() }
    }
}

/**
 * Subscribes to the source [Observable] and will [dispatch] any incoming events onto the reactor.
 * @receiver [Observable]
 *              The source observable.
 * @return The [Disposable] of the [Observable.subscribe] call.
 */
fun <T> Observable<T>.react(): Disposable {
    return this.subscribe {
        MiniReactor.reactor.onNext(it as Any);
    }
}

/**
 * Subscribes to the source [Observable] and will [dispatch] the first incoming event onto the reactor, automatically
 * unsubscribing after the first event.
 * @receiver [Observable]
 *              The source observable.
 * @return The [Disposable] of the [Observable.subscribe] call.
 */
fun <T> Observable<T>.reactOnce(): Disposable {
    return this.take(1).react()
}
