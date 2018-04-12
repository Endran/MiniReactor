package nl.endran.minireactor.core

import io.reactivex.schedulers.TestScheduler
import io.reactivex.subscribers.TestSubscriber
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

class ConcreteMiniReactorTest {

    private lateinit var reactor: ConcreteMiniReactor
    private lateinit var testScheduler: TestScheduler

    private val testObserver1 = TestSubscriber<ExampleEvent1>()
    private val testObserver2 = TestSubscriber<ExampleEvent2>()

    @Before
    fun setUp() {
        testScheduler = TestScheduler()
        testScheduler.start()
        reactor = ConcreteMiniReactor(testScheduler)
    }

    @After
    fun tearDown() {
        testScheduler.shutdown()
        testObserver1.dispose()
        testObserver2.dispose()
    }

    @Test
    fun shouldInformObservableWhenReactorIsDispatched() {

        reactor.listen(ExampleEvent1::class.java)
                .subscribe(testObserver1)

        val event1 = ExampleEvent1("TEST_MESSAGE")
        reactor.dispatch(event1)

        testScheduler.triggerActions()

        assertThat(testObserver1.completions()).isEqualTo(0)
        assertThat(testObserver1.errorCount()).isEqualTo(0)
        assertThat(testObserver1.valueCount()).isEqualTo(1)
        assertThat(testObserver1.values()).containsExactly(event1);
    }

    @Test
    fun shouldInformObservableWithDetailedInfoWhenReactorIsDispatched() {

        val testObserver = TestSubscriber<Pair<String, ExampleEvent1>>()

        reactor.listenForSequences(ExampleEvent1::class.java)
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

        reactor.listen(ExampleEvent1::class.java)
                .subscribe(testObserver1)
        reactor.listen(ExampleEvent2::class.java)
                .subscribe(testObserver2)

        val event1 = ExampleEvent1("TEST_MESSAGE")
        reactor.dispatch(event1)

        testScheduler.triggerActions()
        testScheduler.triggerActions()

        assertThat(reactDisposable.isDisposed).isFalse()

        assertThat(testObserver1.completions()).isEqualTo(0)
        assertThat(testObserver1.errorCount()).isEqualTo(0)
        assertThat(testObserver1.valueCount()).isEqualTo(1)
        assertThat(testObserver1.values()).containsExactly(event1);

        assertThat(testObserver2.completions()).isEqualTo(0)
        assertThat(testObserver2.errorCount()).isEqualTo(0)
        assertThat(testObserver2.valueCount()).isEqualTo(1)
        assertThat(testObserver2.values()).containsExactly(ExampleEvent2(event1.toString()));
    }

    @Test
    fun shouldRegisterAndDispatch() {

        reactor.reaction(ExampleEvent1::class.java) {
            it.map { ExampleEvent2(it.toString()) }
        }

        val event1 = ExampleEvent1("TEST_MESSAGE")
        reactor.listenAndDispatch(ExampleEvent2::class.java, event1)
                .subscribe(testObserver2)

        testScheduler.triggerActions()
        testScheduler.triggerActions()

        assertThat(testObserver2.completions()).isEqualTo(0)
        assertThat(testObserver2.errorCount()).isEqualTo(0)
        assertThat(testObserver2.valueCount()).isEqualTo(1)
        assertThat(testObserver2.values()).containsExactly(ExampleEvent2(event1.toString()));
    }

    private data class ExampleEvent1(val message: String)
    private data class ExampleEvent2(val message: String)
}
