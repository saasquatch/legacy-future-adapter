package com.saasquatch.legacyfutureadapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    try (LegacyFutureAdapter adapter = LegacyFutureAdapter.newBuilder().build()) {
      adapter.start();
      adaptedFuture = adapter.toCompletableFuture(settableFuture);
    }
    assertThrows(TimeoutException.class, () -> adaptedFuture.get(60, TimeUnit.MILLISECONDS));
  }

  @Test
  public void testTimeout() {
    try (LegacyFutureAdapter adapter = LegacyFutureAdapter.newBuilder().build()) {
      adapter.start();
      final CompletableFuture<Object> cf =
          adapter.toCompletableFuture(SettableFuture.create(), Duration.ofMillis(100));
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

  @Test
  public void testQueuedFuture() {
    final LegacyFutureAdapter adapter = LegacyFutureAdapter.newBuilder().build();
    adapter.start();
    adapter.toCompletableFuture(TestHelper.delayedFuture(true, Duration.ofMillis(100)));
    adapter.toCompletableFuture(TestHelper.delayedFuture(true, Duration.ofMillis(200)));
    adapter.toCompletableFuture(TestHelper.delayedFuture(true, Duration.ofMillis(300)));
    adapter.toCompletableFuture(TestHelper.delayedFuture(true, Duration.ofMillis(400)));
    try {
      Thread.sleep(210);
    } catch (InterruptedException e) {
    }
    adapter.close();
    assertEquals(2, adapter.getQueuedFutures().size());
  }

}
