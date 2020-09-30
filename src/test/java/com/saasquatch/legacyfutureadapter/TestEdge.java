package com.saasquatch.legacyfutureadapter;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

public class TestEdge {

  @Test
  public void testFinishAfterClose() {
    final ListenableFuture<Boolean> settableFuture =
        TestHelper.delayedFuture(true, Duration.ofMillis(50));
    final CompletableFuture<Boolean> adaptedFuture;
    try (LegacyFutureAdapter adapter2 = LegacyFutureAdapter.newBuilder().build()) {
      adapter2.start();
      adaptedFuture = adapter2.toCompletableFuture(settableFuture);
    }
    assertThrows(TimeoutException.class, () -> adaptedFuture.get(60, TimeUnit.MILLISECONDS));
  }

  @Test
  public void testTimeout() {
    try (LegacyFutureAdapter adapter2 = LegacyFutureAdapter.newBuilder().build()) {
      adapter2.start();
      final CompletableFuture<Object> cf =
          adapter2.toCompletableFuture(SettableFuture.create(), Duration.ofMillis(100));
      try {
        cf.get(200, TimeUnit.MILLISECONDS);
        fail();
      } catch (ExecutionException e) {
        assertTrue(e.getCause() instanceof TimeoutException);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

}
