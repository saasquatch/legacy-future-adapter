package com.saasquatch.legacyfutureadapter;

import java.time.Duration;
import java.util.concurrent.Executor;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class TestHelper {

  public static Executor threadPerTaskExecutor() {
    return r -> new Thread(r).start();
  }

  public static <T> ListenableFuture<T> delayedFuture(@Nullable T elem, @Nonnull Duration delay) {
    return Futures.submit(() -> {
      Thread.sleep(delay.toMillis());
      return elem;
    }, threadPerTaskExecutor());
  }

}
