package nl.endran.minireactor.core

import io.reactivex.Flowable
import io.reactivex.disposables.Disposable


/**
 * A very lightweight and thread-safe implementation of the Reactor Pattern, with RxJava2. ConcreteMiniReactor takes in data,
 * from any thread, and demultiplexes them into a single thread. Interested classes can register to
 * ConcreteMiniReactor to obtain this data, on the Reactor thread. Classes should never block the Reactor thread.
 */
interface MiniReactor {

    /**
     * Injects data in the reactor. The reactor will output any event on the reactorScheduler thread.
     *
     * @param data
     *          The data to inject in the reactor.
     * @param id
     *          The id of the data injection.
     * @return
     *          The id of the data injection, for convenience.
     */
    fun dispatch(data: Any, id: String): String

    /**
     * Injects data in the reactor. The reactor will output any event on the reactorScheduler thread.
     * A unique id will automatically be generate via [generateId].
     *
     * @param data
     *          The data to inject in the reactor.
     * @return
     *          The id of the data injection, for convenience.
     */
    fun dispatch(data: Any) = dispatch(data, generateId())

    /**
     * Register a handler for specific data types on the reactor, results automatically flow back in the reactor.
     * Data will be emitted on the reactorScheduler thread.
     *
     * **Warning:** Do not block this thread. Blocking calls must be lifted from this thread to another tread,
     * use for example [Flowable.observeOn(Schedulers.io())].
     *
     * @param clazz
     *          The Class of the event to receive from the reactor.
     * @param block
     *          The specific steps for the reaction pipeline.
     * @return The [Disposable] of the [Flowable.subscribe] call.
     */
    fun <T, R> reaction(clazz: Class<T>, block: (Flowable<T>) -> Flowable<R>): Disposable

    /**
     * Register a handler for specific data types on the reactor. Data will be emitted on the reactorScheduler thread.
     *
     * **Warning:** Do not block this thread. Blocking calls must be lifted from this thread to another tread,
     * use for example [Flowable.observeOn(Schedulers.io())].
     *
     * @param clazz
     *            The Class of the event to receive from the reactor.
     * @return The [Flowable] for the specific data.
     */
    fun <T> lurker(clazz: Class<T>): Flowable<T>

    /**
     * Register a handler for specific internal events in reactor. Data will be emitted on the reactorScheduler thread.
     *
     * **Warning:** Do not block this thread. Blocking calls must be lifted from this thread to another tread,
     * use for example [Flowable.observeOn(Schedulers.io())].
     *
     * @param clazz
     *            The Class of the event to receive from the reactor.
     * @return The [Flowable] for the specific data, with the id.
     */
    fun <T> lurkerForSequences(clazz: Class<T>): Flowable<Pair<String, T>>

    /**
     * A combination of [lurker] and [dispatch].
     * Will register a handler for specific data types on the reactor **and** and only related to this data injection id.
     * Only on the first subscribe the event will be dispatched.
     * Data will be emitted on the reactorScheduler thread.
     *
     * **Warning:** Do not block this thread. Blocking calls must be lifted from this thread to another tread,
     * use for example [Flowable.observeOn(Schedulers.io())].
     *
     * @param clazz
     *            The Class of the data to receive from the reactor.
     * @param data
     *            The data to inject in the reactor.
     * @param id
     *          The id of the data injection.
     * @return The [Flowable] for the specific data and id.
     */
    fun <T> lurkAndDispatch(clazz: Class<T>, payload: Any, id: String): Flowable<T>


    /**
     * A combination of [lurker] and [dispatch].
     * Will register a handler for specific data types on the reactor **and** and only related to this data injection id.
     * Only on the first subscribe the event will be dispatched.
     * Data will be emitted on the reactorScheduler thread.
     * A unique id will automatically be generate via [generateId].
     *
     * **Warning:** Do not block this thread. Blocking calls must be lifted from this thread to another tread,
     * use for example [Flowable.observeOn(Schedulers.io())].
     *
     * @param clazz
     *            The Class of the data to receive from the reactor.
     * @param data
     *          The id of the data injection. Will generate a unique Id by default.
     * @return The [Flowable] for the specific data and id.
     */
    fun <T> lurkAndDispatch(clazz: Class<T>, payload: Any) = lurkAndDispatch(clazz, payload, generateId())

    companion object {
        var eventId = 0
        @JvmOverloads
        fun generateId(postFix: String = "GEN_ID"): String {
            synchronized(eventId) {
                return "${eventId++}_$postFix"
            }
        }
    }
}
