package com.saasquatch.legacyfutureadapter;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.common.util.concurrent.SettableFuture;

public class TestHelper {

  public static Executor threadPerTaskExecutor() {
    return r -> new Thread(r).start();
  }

  public static <T> CompletableFuture<T> delayedFuture(@Nullable T elem, @Nonnull Duration delay) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        Thread.sleep(delay.toMillis());
      } catch (InterruptedException e) {
      }
      return elem;
    }, threadPerTaskExecutor());
  }

  public static <T> SettableFuture<T> delayedSettableFuture(@Nullable T elem,
      @Nonnull Duration delay) {
    final SettableFuture<T> f = SettableFuture.create();
    delayedFuture(null, delay).thenRun(() -> f.set(elem));
    return f;
  }

}
