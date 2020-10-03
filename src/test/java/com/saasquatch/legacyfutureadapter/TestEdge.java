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
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class TestEdge {

  @Test
  public void testFinishAfterClose() {
    final ListenableFuture<Boolean> settableFuture =
        TestHelper.delayedFuture(true, Duration.ofMillis(50));
    final CompletableFuture<Boolean> adaptedFuture;
    try (LegacyFutureAdapter adapter = LegacyFutureAdapter.create()) {
      adapter.start();
      adaptedFuture = adapter.toCompletableFuture(settableFuture);
    }
    assertThrows(TimeoutException.class, () -> adaptedFuture.get(60, TimeUnit.MILLISECONDS));
  }

  @Test
  public void testTimeout() {
    try (LegacyFutureAdapter adapter = LegacyFutureAdapter.create()) {
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
    adapter.toCompletableFuture(TestHelper.delayedFuture(1, Duration.ofMillis(100)));
    adapter.toCompletableFuture(TestHelper.delayedFuture(2, Duration.ofMillis(200)));
    adapter.toCompletableFuture(TestHelper.delayedFuture(3, Duration.ofMillis(300)));
    adapter.toCompletableFuture(TestHelper.delayedFuture(4, Duration.ofMillis(400)));
    try {
      Thread.sleep(210);
    } catch (InterruptedException e) {
    }
    adapter.close();
    assertEquals(2, adapter.getQueuedFutures().size());
    assertEquals(ImmutableSet.of(3, 4), adapter.getQueuedFutures().stream().map(f -> {
      try {
        return f.get(1, TimeUnit.SECONDS);
      } catch (InterruptedException | ExecutionException | TimeoutException e) {
        throw new RuntimeException(e);
      }
    }).collect(Collectors.toSet()));
  }

  @Test
  public void testThread() {
    final String callerThreadName = Thread.currentThread().getName();
    final String eventLoopThreadName = "foo";
    try (LegacyFutureAdapter adapter = LegacyFutureAdapter.newBuilder()
        .setEventLoopThreadFactory(
            new ThreadFactoryBuilder().setDaemon(true).setNameFormat(eventLoopThreadName).build())
        .build()) {
      adapter.start();
      final CompletableFuture<Integer> cf1 =
          adapter.toCompletableFuture(Futures.immediateFuture(1));
      cf1.thenAccept(i -> {
        assertEquals(1, i);
        assertEquals(callerThreadName, Thread.currentThread().getName());
      }).join();
      final CompletableFuture<Integer> cf2 =
          adapter.toCompletableFuture(TestHelper.delayedFuture(2, Duration.ofMillis(100)));
      cf2.thenAccept(i -> {
        assertEquals(2, i);
        assertEquals(eventLoopThreadName, Thread.currentThread().getName());
      });
    }
  }

}
