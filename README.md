# MiniReactor

A very small reactor based on RxJava2

## Usage

```
val reactor = MiniReactor()

reactor.register(SomeRequestToTheDatabase::class.java)
    .map{ goToTheDatabaseAndGetSomeData }
    .react()


reactor.register(SomeResponseFromTheDatabase::class.java)
    .take(1)
    .subscribe {
        DoSomethingWithTheResponse(it)
    }
    
reactor.dispatch(SomeRequestToTheDatabase('* from users'))
```