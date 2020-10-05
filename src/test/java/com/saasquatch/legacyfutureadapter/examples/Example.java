package com.saasquatch.legacyfutureadapter.examples;

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
       * Otherwise, this can block the event loop thread and in turn block other CompletableFutures
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
