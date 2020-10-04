# legacy-future-adapter

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Event loop based adapter for legacy `Future`s that do not support callbacks

## Getting started

### Introduction

Before the introduction of `CompletableFuture`, Java didn't have a standard implementation of `Future` that supports callbacks. As a result, legacy libraries, including some of the Java's built-in classes, still return basic `Future`s that don't support callbacks. The common solution of adding callbacks to a legacy `Future` is to wait for it with a thread (one such example is [Guava's `JdkFutureAdapters`](https://github.com/google/guava/blob/c414be307af45d8197f3d8b2db256cb369f948af/guava/src/com/google/common/util/concurrent/JdkFutureAdapters.java)). The issue of this approach is that a new thread is needed for every `Future` instance. This library, however, uses a shared single event loop thread to wait for all the instances of `Future`s.

### When not to use this library

If the legacy libraries you are using already support callbacks, then you should convert those into `CompletableFuture`s yourself with a callback. Also, if the legacy `Future`s you are dealing with are few and far between, using this library will provide little to no benefit compared to waiting for those `Future`s with separate threads.

### Sample usage

```java
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import com.saasquatch.legacyfutureadapter.LegacyFutureAdapter;

public class Example {

  public static void main(String[] args) {
    try (LegacyFutureAdapter futureAdapter = LegacyFutureAdapter.create()) {
      // This starts the event loop and has to be called before proceeding
      futureAdapter.start();
      // Say we have a Future returned by some legacy library that does not support callbacks
      final Future<Integer> f1 = delayedFuture(1, Duration.ofSeconds(1));
      // This "converts" the legacy Future into a CompletableFuture
      final CompletableFuture<Integer> cf1 = futureAdapter.toCompletableFuture(f1);
      /*
       * Do whatever you want with it. But keep in mind that for subsequent long running tasks, you
       * should provide your own Executor so it doesn't block the event loop thread.
       */
      cf1.thenAccept(System.out::println);
      // Put another fake Future in it
      final Future<Integer> f2 = delayedFuture(2, Duration.ofSeconds(2));
      /*
       * An optional timeout can be provided. In this particular case, cf2 will complete with a
       * TimeoutException.
       */
      final CompletableFuture<Integer> cf2 =
          futureAdapter.toCompletableFuture(f2, Duration.ofSeconds(1));
      cf2.thenAccept(System.out::println);
      // Another example
      final Future<Integer> f3 = delayedFuture(3, Duration.ofSeconds(3));
      final CompletableFuture<Integer> cf3 = futureAdapter.toCompletableFuture(f3);
      /*
       * NEVER DO THIS!!! If you need to run long blocking tasks, please provide your own executor.
       * Otherwise, this will block the event loop thread and in turn block other CompletableFutures
       * from completing.
       */
      cf3.thenRun(() -> {
        try {
          Thread.sleep(1000); // BAD!!!!!!
        } catch (InterruptedException e) {
        }
      });
      /*
       * stop() can be optionally called before close(). Calling stop will block the
       * LegacyFutureAdapter from accepting new Futures, but the event loop will continue running
       * and all the CompletableFutures that haven't completed yet will eventually complete.
       */
      futureAdapter.stop();
      // This give you all the Futures the LegacyFutureAdapter is still waiting for.
      final List<Future<Object>> queuedFutures = futureAdapter.getQueuedFutures();
      System.out.println(queuedFutures.size());
    }
    /*
     * Once close() is called, the event loop will stop, and all the CompletableFutures that haven't
     * completed yet, including ones created with an explicit timeout, will never complete.
     */
  }

  /**
   * Create a fake delayed {@link Future} with the given element
   */
  private static <T> Future<T> delayedFuture(T elem, Duration timeout) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        Thread.sleep(timeout.toMillis());
      } catch (InterruptedException e) {
      }
      return elem;
    }, r -> new Thread(r).start());
  }

}
```

The example source code is available under package [`com.saasquatch.legacyfutureadapter.examples`](https://github.com/saasquatch/legacy-future-adapter/tree/master/src/test/java/com/saasquatch/legacyfutureadapter/examples).

### State diagram

![](https://i.imgur.com/7Cooiup.jpg)

## License

Unless explicitly stated otherwise all files in this repository are licensed under the Apache License 2.0.

License boilerplate:

```
Copyright 2020 ReferralSaaSquatch.com Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
