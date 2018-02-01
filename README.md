[![Hex.pm](https://img.shields.io/hexpm/l/plug.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Language](https://img.shields.io/badge/language-kotlin-yellowgreen.svg)](https://www.google.nl/search?q=kotlin)
[![Build Status](https://travis-ci.org/Endran/MiniReactor.svg?branch=master)](https://travis-ci.org/Endran/MiniReactor)
[![](https://jitpack.io/v/endran/MiniReactor.svg)](https://jitpack.io/#endran/MiniReactor)
# MiniReactor

A very lightweight and thread-safe implementation of the Reactor Pattern, with RxJava2.
MiniReactor takes in data, from any thread, and demultiplexes them into a single thread.
Interested classes can register to MiniReactor to obtain this data, on the Reactor thread.
Classes should never block the Reactor thread.

## Get it

Add JitPack in your root build.gradle at the end of repositories:

```
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```

Step 2. Add MiniReactor dependency

```
	dependencies {
	        compile 'com.github.endran:MiniReactor:0.1.0'
	}
```

## Usage

To use *MiniReactor* you need to define how data is used in the system,
and how you get data in and out of the system. For this 3 concepts are used;
_Reaction_, _Lurker_, and a _Resource_.

**Resource**<br>
A Resource can inject information in the reactor as follows:
```
miniReactor.dispatch(SomeRequest("Hello!!!"))
```

**Reaction**<br>
Reactions can listen for types of information and do actions on it:
```
miniReactor.reaction(SomeRequest::class.java) {
    it.map { it.toString() }
      .map { SomeResponse(it) }
}
```

**Lurker**<br>
A Lurker can lister for specific infomation, and consume it.
```
miniReactor.lurker(Object::class.java)
    .subscribe {
        System.out.println("Dispatched to the reactor: $it")
    }
```

**Lurking Resource**<br>
Finally a Lurking Resource can listen for specific information, for your request.
The reactor will make sure that you will only receive responses to your specific request.
```
miniReactor.lurkAndDispatch(SomeResponse::class.java, SomeRequest("Hello!!!"))
    .take(1)
    .subscribe {
        System.out.println("Received response for my specific request: $it")
    }
```

## Examples

See [MiniReactorExamples](https://github.com/Endran/MiniReactorExamples) for various exampels on how to use MiniReactor. Below is an example with Spring:

```
@Configuration
@ComponentScan(basePackages = arrayOf("nl.endran.minireactorexamples.spring"))
open class MainConfig {

    @Bean
    open fun miniReactor(): MiniReactor {
        return ConcreteMiniReactor()
    }
}

@Service
class SomeReaction(private val miniReactor: MiniReactor) {

    @PostConstruct
    fun start() {
        miniReactor.reaction(SomeRequest::class.java) {
            it.map { it.toString() }.map { SomeResponse(it) }
        }
    }
}

@RestController
class ExampleController(private val miniReactor: MiniReactor) {

    var count = 0

    @RequestMapping(path = arrayOf("/hello"), method = arrayOf(RequestMethod.GET))
    fun hello(): Observable<SomeResponse> {
        return miniReactor.lurkAndDispatch(SomeResponse::class.java, SomeRequest("Hello!!! (${count++})"))
                .take(1)
    } // --> [{"message":"SomeRequest(message=Hello!!! (1))"}]

    @RequestMapping(path = arrayOf("/hi"), method = arrayOf(RequestMethod.GET))
    fun hi(): Observable<SomeResponse> {
        return miniReactor.lurkAndDispatch(SomeResponse::class.java, SomeRequest("Hi :) (${count++})"))
                .take(1)
    } // --> [{"message":"SomeRequest(message=Hi :) (0))"}]
}
```


## Interface

This is the complete interface of [MiniReactor](https://github.com/Endran/MiniReactor/blob/develop/minireactor/src/main/kotlin/nl/endran/minireactor/MiniReactor.kt).

```
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
     * @param data
     *          The id of the data injection. Will generate a unique Id by default.
     * @return
     *          The id of the data injection, for convenience.
     */
    fun dispatch(data: Any, id: String = ConcreteMiniReactor.generateId()): String

    /**
     * Register a handler for specific data types on the reactor, results automatically flow back in the reactor.
     * Data will be emitted on the reactorScheduler thread.
     *
     * **Warning:** Do not block this thread. Blocking calls must be lifted from this thread to another tread,
     * use for example [Observable.observeOn(Schedulers.io())].
     *
     * @param clazz
     *          The Class of the event to receive from the reactor.
     * @param block
     *          The specific steps for the reaction pipeline.
     * @return The [Disposable] of the [Observable.subscribe] call.
     */
    fun <T, R> reaction(clazz: Class<T>, block: (Observable<T>) -> Observable<R>): Disposable

    /**
     * Register a handler for specific data types on the reactor. Data will be emitted on the reactorScheduler thread.
     *
     * **Warning:** Do not block this thread. Blocking calls must be lifted from this thread to another tread,
     * use for example [Observable.observeOn(Schedulers.io())].
     *
     * @param clazz
     *            The Class of the event to receive from the reactor.
     * @return The [Observable] for the specific events.
     */
    fun <T> lurker(clazz: Class<T>): Observable<T>

    /**
     * A combination of [lurker] and [dispatch].
     * Will register a handler for specific data types on the reactor **and** and only related to this data injection id.
     * Only on the first subscribe the event will be dispatched.
     * Data will be emitted on the reactorScheduler thread.
     *
     * **Warning:** Do not block this thread. Blocking calls must be lifted from this thread to another tread,
     * use for example [Observable.observeOn(Schedulers.io())].
     *
     * @param clazz
     *            The Class of the data to receive from the reactor.
     * @param data
     *            The data to inject in the reactor.
     * @param data
     *          The id of the data injection. Will generate a unique Id by default.
     * @return The [Observable] for the specific data and id.
     */
    fun <T> lurkAndDispatch(clazz: Class<T>, payload: Any, id: String = ConcreteMiniReactor.generateId()): Observable<T>
}
```

## License

```
Copyright 2018 David Hardy

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
```
