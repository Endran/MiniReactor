package nl.endran.minireactor.core

import io.reactivex.Flowable
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.internal.schedulers.SingleScheduler
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.processors.PublishProcessor
import nl.endran.minireactor.core.MiniReactor.Companion.generateId
import java.util.concurrent.TimeUnit

/**
 * Concrete implementation of [MiniReactor]
 *
 * @constructor
 * @param reactorScheduler
 *          The Reactor Scheduler, will default to a [Scheduler] from [Executors.newSingleThreadExecutor()]
 */
class LocalMiniReactor(private val reactorScheduler: Scheduler = createDefaultReactorScheduler()) : MiniReactor {

    private val publishProcessor = PublishProcessor.create<Event<*>>()
    private val reactor = publishProcessor.onBackpressureBuffer();
    private val classCountMap = mutableMapOf<Class<*>, Int>()

    override fun dispatch(data: Any, id: String): String {
        dispatch(Event(data, id))
        return id;
    }

    fun dispatch(event: Event<*>): String {
        if (!isClassSupported(event.data!!.javaClass)) {
            val data = UnsupportedData(event.data)
            val event1 = Event(data, event.id)
            publishProcessor.onNext(event1)

        } else {
            publishProcessor.onNext(event)
        }
        return event.id
    }

    override fun <T, R> reaction(clazz: Class<T>, block: (Flowable<T>) -> Flowable<R>): Disposable {
        return register(clazz)
                .flatMap {
                    return@flatMap Flowable.combineLatest(
                            block.invoke(Flowable.just(it.data)),
                            Flowable.just(it.id),
                            BiFunction<R, String, Event<R>> { payload, id -> Event(payload, "$id>") })
                }
                .subscribe { dispatch(it) }
    }

    override fun <T> lurker(clazz: Class<T>): Flowable<T> {
        return register(clazz).map { it.data }
    }

    override fun <T> lurkerForSequences(clazz: Class<T>): Flowable<Pair<String, T>> {
        return register(clazz).map { Pair(it.id, it.data) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> register(clazz: Class<T>): Flowable<Event<T>> {
        return reactor
                .observeOn(reactorScheduler)
                .filter { clazz.isInstance(it.data) }
                .map { it as Event<T> }
                .doOnSubscribe {
                    classCountMap[clazz] = classCountMap.getOrDefault(clazz, 0) + 1
                }
                .doOnCancel {
                    classCountMap[clazz] = classCountMap.getOrDefault(clazz, 0) - 1
                }
    }


    override fun <T> lurkAndDispatch(clazz: Class<T>, payload: Any, id: String): Flowable<T> {
        var once = false
        return register(clazz)
                .filter { it.id.startsWith(id) }
                .map { it.data }
                .doOnSubscribe {
                    if (!once) {
                        once = true
                        Flowable.just(Event(payload, id))
                                .delay(1, TimeUnit.MICROSECONDS)
                                .subscribe { dispatch(it) }
                    }
                }
    }

    fun isClassSupported(clazz: Class<*>): Boolean {
        val isSupported = classCountMap.getOrDefault(clazz, 0) > 0
        if (!isSupported) {
            return classCountMap.keys
                    .filter { it.isInstance(clazz) }
                    .filter { !it.equals(Object::class.java) }
                    .filter { !clazz.equals(Object::class.java) }.count() > 0
        }
        return isSupported
    }

    companion object {
        private fun createDefaultReactorScheduler() = RxJavaPlugins.onSingleScheduler(RxJavaPlugins.initSingleScheduler({ SingleScheduler() }));
    }

    data class Event<out T>(val data: T, val id: String = generateId())
    data class UnsupportedData<T>(val data: T)
}
