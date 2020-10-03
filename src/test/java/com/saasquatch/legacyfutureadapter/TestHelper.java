package com.saasquatch.legacyfutureadapter;

import java.time.Duration;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

public class TestHelper {

  public static <T> ListenableFuture<T> delayedFuture(@Nullable T elem, @Nonnull Duration delay) {
    final SettableFuture<T> f = SettableFuture.create();
    final Thread t = new Thread(() -> {
      try {
        Thread.sleep(delay.toMillis());
      } catch (InterruptedException e) {
      }
      f.set(elem);
    });
    t.setDaemon(true);
    t.start();
    return f;
  }

}
