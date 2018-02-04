package nl.endran.minireactor.core

import io.reactivex.schedulers.TestScheduler
import io.reactivex.subscribers.TestSubscriber
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

class LocalMiniReactorTest {

    private lateinit var reactor: LocalMiniReactor
    private lateinit var testScheduler: TestScheduler

    private val testSubscriber1 = TestSubscriber<ExampleEvent1>()
    private val testSubscriber2 = TestSubscriber<ExampleEvent2>()

    @Before
    fun setUp() {
        testScheduler = TestScheduler()
        testScheduler.start()
        reactor = LocalMiniReactor(testScheduler)
    }

    @After
    fun tearDown() {
        testScheduler.shutdown()
        testSubscriber1.dispose()
        testSubscriber2.dispose()
    }

    @Test
    fun shouldInformObservableWhenReactorIsDispatched() {

        reactor.lurker(ExampleEvent1::class.java)
                .subscribe(testSubscriber1)

        val event1 = ExampleEvent1("TEST_MESSAGE")
        reactor.dispatch(event1)

        testScheduler.triggerActions()

        assertThat(testSubscriber1.completions()).isEqualTo(0)
        assertThat(testSubscriber1.errorCount()).isEqualTo(0)
        assertThat(testSubscriber1.valueCount()).isEqualTo(1)
        assertThat(testSubscriber1.values()).containsExactly(event1);
    }

    @Test
    fun shouldInformObservableWithDetailedInfoWhenReactorIsDispatched() {

        val testObserver = TestSubscriber<Pair<String, ExampleEvent1>>()

        reactor.lurkerForSequences(ExampleEvent1::class.java)
                .subscribe(testObserver)

        val event1 = ExampleEvent1("TEST_MESSAGE")
        reactor.dispatch(event1, "TEST_ID")

        testScheduler.triggerActions()

        assertThat(testObserver.completions()).isEqualTo(0)
        assertThat(testObserver.errorCount()).isEqualTo(0)
        assertThat(testObserver.valueCount()).isEqualTo(1)
        assertThat(testObserver.values()).containsExactly(Pair("TEST_ID", event1));
    }

    @Test
    fun shouldInformObservableWhenReactionIsReReacted() {

        val reactDisposable = reactor.reaction(ExampleEvent1::class.java) {
            it.map { ExampleEvent2(it.toString()) }
        }

        reactor.lurker(ExampleEvent1::class.java)
                .subscribe(testSubscriber1)
        reactor.lurker(ExampleEvent2::class.java)
                .subscribe(testSubscriber2)

        val event1 = ExampleEvent1("TEST_MESSAGE")
        reactor.dispatch(event1)

        testScheduler.triggerActions()
        testScheduler.triggerActions()

        assertThat(reactDisposable.isDisposed).isFalse()

        assertThat(testSubscriber1.completions()).isEqualTo(0)
        assertThat(testSubscriber1.errorCount()).isEqualTo(0)
        assertThat(testSubscriber1.valueCount()).isEqualTo(1)
        assertThat(testSubscriber1.values()).containsExactly(event1);

        assertThat(testSubscriber2.completions()).isEqualTo(0)
        assertThat(testSubscriber2.errorCount()).isEqualTo(0)
        assertThat(testSubscriber2.valueCount()).isEqualTo(1)
        assertThat(testSubscriber2.values()).containsExactly(ExampleEvent2(event1.toString()));
    }

    @Test
    fun shouldRegisterAndDispatch() {

        reactor.reaction(ExampleEvent1::class.java) {
            it.map { ExampleEvent2(it.toString()) }
        }

        val event1 = ExampleEvent1("TEST_MESSAGE")
        reactor.lurkAndDispatch(ExampleEvent2::class.java, event1)
                .subscribe(testSubscriber2)

        testScheduler.triggerActions()
        testScheduler.triggerActions()

        assertThat(testSubscriber2.completions()).isEqualTo(0)
        assertThat(testSubscriber2.errorCount()).isEqualTo(0)
        assertThat(testSubscriber2.valueCount()).isEqualTo(1)
        assertThat(testSubscriber2.values()).containsExactly(ExampleEvent2(event1.toString()));
    }

    @Test
    fun shouldCountClassesCorrectly() {

        reactor.lurker(ExampleEvent1::class.java)
                .subscribe(testSubscriber1)

        assertThat(reactor.isClassSupported(ExampleEvent1::class.java)).isTrue()
        assertThat(reactor.isClassSupported(ExampleEvent2::class.java)).isFalse()
        assertThat(reactor.isClassSupported(Object::class.java)).isFalse()

        reactor.lurker(Object::class.java)
                .subscribe(TestSubscriber<Object>())

        assertThat(reactor.isClassSupported(ExampleEvent1::class.java)).isTrue()
        assertThat(reactor.isClassSupported(ExampleEvent2::class.java)).isFalse()
        assertThat(reactor.isClassSupported(Object::class.java)).isTrue()

        reactor.lurker(ExampleEvent2::class.java)
                .subscribe(testSubscriber2)
        testSubscriber1.dispose()
        testScheduler.triggerActions()

        assertThat(reactor.isClassSupported(ExampleEvent1::class.java)).isFalse()
        assertThat(reactor.isClassSupported(ExampleEvent2::class.java)).isTrue()
        assertThat(reactor.isClassSupported(Object::class.java)).isTrue()

    }

    private data class ExampleEvent1(val message: String)
    private data class ExampleEvent2(val message: String)
}
