package nl.endran

import io.reactivex.schedulers.TestScheduler
import io.reactivex.subscribers.TestSubscriber
import nl.endran.minireactor.ConcreteMiniReactor
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

class ConcreteMiniReactorTest {

    lateinit var reactor: ConcreteMiniReactor
    lateinit var testScheduler: TestScheduler

    val testObserver1 = TestSubscriber<ExampleEvent1>()
    val testObserver2 = TestSubscriber<ExampleEvent2>()

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

        reactor.lurker(ExampleEvent1::class.java)
                .subscribe(testObserver1)

        val event1 = ExampleEvent1("TEST_MESSAGE")
        reactor.dispatch(event1)

        testScheduler.triggerActions()

        testObserver1.assertNotComplete();
        testObserver1.assertNoErrors();
        testObserver1.assertValueCount(1);
        assertThat(testObserver1.values()).containsExactly(event1);
    }

    @Test
    fun shouldInformObservableWithDetailedInfoWhenReactorIsDispatched() {

        val testObserver = TestSubscriber<Pair<String, ExampleEvent1>>()

        reactor.lurkerForSequences(ExampleEvent1::class.java)
                .subscribe(testObserver)

        val event1 = ExampleEvent1("TEST_MESSAGE")
        reactor.dispatch(event1, "TEST_ID")

        testScheduler.triggerActions()

        testObserver.assertNotComplete();
        testObserver.assertNoErrors();
        testObserver.assertValueCount(1);
        assertThat(testObserver.values()).containsExactly(Pair("TEST_ID", event1));
    }

    @Test
    fun shouldInformObservableWhenReactionIsReReacted() {

        val reactDisposable = reactor.reaction(ExampleEvent1::class.java) {
            it.map { ExampleEvent2(it.toString()) }
        }

        reactor.lurker(ExampleEvent1::class.java)
                .subscribe(testObserver1)
        reactor.lurker(ExampleEvent2::class.java)
                .subscribe(testObserver2)

        val event1 = ExampleEvent1("TEST_MESSAGE")
        reactor.dispatch(event1)

        testScheduler.triggerActions()
        testScheduler.triggerActions()

        assertThat(reactDisposable.isDisposed).isFalse()

        testObserver1.assertNotComplete();
        testObserver1.assertNoErrors();
        testObserver1.assertValueCount(1);
        assertThat(testObserver1.values()).containsExactly(event1);

        testObserver2.assertNotComplete();
        testObserver2.assertNoErrors();
        testObserver2.assertValueCount(1);
        assertThat(testObserver2.values()).containsExactly(ExampleEvent2(event1.toString()));
    }

    @Test
    fun shouldRegisterAndDispatch() {

        reactor.reaction(ExampleEvent1::class.java) {
            it.map { ExampleEvent2(it.toString()) }
        }

        val event1 = ExampleEvent1("TEST_MESSAGE")
        reactor.lurkAndDispatch(ExampleEvent2::class.java, event1)
                .subscribe(testObserver2)

        testScheduler.triggerActions()
        testScheduler.triggerActions()

        testObserver2.assertNotComplete();
        testObserver2.assertNoErrors();
        testObserver2.assertValueCount(1);
        assertThat(testObserver2.values()).containsExactly(ExampleEvent2(event1.toString()));
    }

    data class ExampleEvent1(val message: String)
    data class ExampleEvent2(val message: String)
}
