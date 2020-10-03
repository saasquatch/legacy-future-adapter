# legacy-future-adapter

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Event loop based adapter for legacy `Future`s that do not support callbacks

## Getting started

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
      // Do whatever you want with it
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
     * completed yet will never complete.
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
    }, r -> new Thread().start());
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
