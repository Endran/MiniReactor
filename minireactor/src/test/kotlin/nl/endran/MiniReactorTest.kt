package nl.endran

import io.reactivex.disposables.Disposable
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.TestScheduler
import nl.endran.minireactor.MiniReactor
import nl.endran.minireactor.react
import nl.endran.minireactor.reactOnce
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

class MiniReactorTest {

    lateinit var miniReactor: MiniReactor
    lateinit var testScheduler: TestScheduler

    val disposables = mutableListOf<Disposable>()

    val testObserver1 = TestObserver<ExampleEvent1>()
    val testObserver2 = TestObserver<ExampleEvent2>()

    @Before
    fun setUp() {
        testScheduler = TestScheduler()
        testScheduler.start()
        miniReactor = MiniReactor(testScheduler)
    }

    @After
    fun tearDown() {
        testScheduler.shutdown()
        testObserver1.dispose()
        testObserver2.dispose()
    }

    @Test
    fun shouldInformObservableWhenReactorIsDispatched() {

        miniReactor.register(ExampleEvent1::class.java)
                .subscribe(testObserver1)

        val event1 = ExampleEvent1("TEST_MESSAGE")
        miniReactor.dispatch(event1)

        testScheduler.triggerActions()

        testObserver1.assertNotComplete();
        testObserver1.assertNoErrors();
        testObserver1.assertValueCount(1);
        assertThat(testObserver1.values()).containsExactly(event1);
    }

    @Test
    fun shouldInformObservableWhenReactionIsReReacted() {

        val reactDisposable = miniReactor.register(ExampleEvent1::class.java)
                .map { ExampleEvent2(it.toString()) }
                .react()

        miniReactor.register(ExampleEvent1::class.java)
                .subscribe(testObserver1)
        miniReactor.register(ExampleEvent2::class.java)
                .subscribe(testObserver2)

        val event1 = ExampleEvent1("TEST_MESSAGE")
        miniReactor.dispatch(event1)

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
    fun shouldRereactOnceIfAsked() {

        val reactOnceDisposable = miniReactor.register(ExampleEvent1::class.java)
                .map { ExampleEvent2(it.toString()) }
                .reactOnce()

        val event1 = ExampleEvent1("TEST_MESSAGE")
        miniReactor.dispatch(event1)

        testScheduler.triggerActions()

        assertThat(reactOnceDisposable.isDisposed).isTrue()
    }

    @Test
    fun shouldRegisterAndDispatch() {

        miniReactor.register(ExampleEvent1::class.java)
                .map { ExampleEvent2(it.toString()) }
                .reactOnce()

        val event1 = ExampleEvent1("TEST_MESSAGE")
        miniReactor.registerAndDispatch(event1, ExampleEvent2::class.java)
                .subscribe(testObserver2)

        testScheduler.triggerActions()

        testObserver2.assertNotComplete();
        testObserver2.assertNoErrors();
        testObserver2.assertValueCount(1);
        assertThat(testObserver2.values()).containsExactly(ExampleEvent2(event1.toString()));
    }

    data class ExampleEvent1(val message: String)
    data class ExampleEvent2(val message: String)
}