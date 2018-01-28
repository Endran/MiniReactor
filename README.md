[![Hex.pm](https://img.shields.io/hexpm/l/plug.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Language](https://img.shields.io/badge/language-kotlin-yellowgreen.svg)](https://www.google.nl/search?q=kotlin)
[![Build Status](https://travis-ci.org/Endran/MiniReactor.svg?branch=develop)](https://travis-ci.org/Endran/MiniReactor)
[![Coverage Status](https://coveralls.io/repos/github/Endran/MiniReactor/badge.svg?branch=master)](https://coveralls.io/github/Endran/MiniReactor?branch=develop)
[![](https://jitpack.io/v/endran/MiniReactor.svg)](https://jitpack.io/#endran/MiniReactor)
# MiniReactor

A very small reactor based on RxJava2.

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