package nl.endran.minireactor

import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.internal.schedulers.SingleScheduler
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

/**
 * A very lightweight and thread-safe implementation of the Reactor Pattern, with RxJava2. MiniReactor takes in events,
 * from any thread, and demultiplexes them into a single thread. Interested classes can register to
 * MiniReactor to obtain those events, on the Reactor thread. Classes should never block the Reactor thread.
 *
 * @constructor
 * @param reactorScheduler
 *          The Reactor Scheduler, will default to a [Scheduler] from [Executors.newSingleThreadExecutor()]
 */
class MiniReactor(private val reactorScheduler: Scheduler = createDefaultReactorScheduler()) {

    private val reactor = PublishSubject.create<Event<*>>();

    /**
     * Dispatches an event to the reactor. The reactor will output any event on the reactorScheduler thread.
     *
     * @param event
     *            The event to dispatch onto the reactor.
     */

    fun dispatch(payload: Any, id: String = generateId()) {
        dispatch(Event(payload, id))
    }

    private fun dispatch(event: Event<*>) {
        reactor.onNext(event)
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
    fun <T, R> reaction(clazz: Class<T>, block: (Observable<T>) -> Observable<R>): Disposable {
        return registerInternal(clazz)
                .flatMap {
                    return@flatMap Observable.combineLatest(
                            block.invoke(Observable.just(it.payload)),
                            Observable.just(it.id),
                            BiFunction<R, String, Event<R>> { payload, id -> Event(payload, id) })
                }
                .subscribe { dispatch(it) }
    }

    fun <T> lurker(clazz: Class<T>): Observable<T> {
        return registerInternal(clazz).map { it.payload }
    }

    private fun <T> registerInternal(clazz: Class<T>): Observable<Event<T>> {
        return reactor
                .observeOn(reactorScheduler)
                .filter { clazz.isInstance(it.payload) }
                .map { it as Event<T> }
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
    fun <T> lurkAndDispatch(clazz: Class<T>, payload: Any, id: String = generateId()): Observable<T> {
        var once = false
        return registerInternal(clazz)
                .filter { it.id == id }
                .map { it.payload }
                .doOnSubscribe {
                    if (!once) {
                        once = true
                        Observable.just(Event(payload, id))
                                .delay(1, TimeUnit.MICROSECONDS)
                                .subscribe { dispatch(it) }
                    }
                }
    }

    companion object {
        var eventId = 0
        private fun generateId(): String {
            synchronized(eventId) {
                return "GenerateId_${eventId++}"
            }
        }

        private fun createDefaultReactorScheduler() = RxJavaPlugins.onSingleScheduler(RxJavaPlugins.initSingleScheduler({ SingleScheduler() }));
    }

    private data class Event<out T>(val payload: T, val id: String = generateId())
}
