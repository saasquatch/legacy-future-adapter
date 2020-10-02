package com.saasquatch.legacyfutureadapter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import com.google.common.util.concurrent.SettableFuture;

public class TestLifecycle {

  @Test
  public void testClose() {
    try (LegacyFutureAdapter legacyFutureAdapter = LegacyFutureAdapter.newBuilder().build()) {
      assertThrows(IllegalStateException.class,
          () -> legacyFutureAdapter.toCompletableFuture(SettableFuture.create()));
      legacyFutureAdapter.start();
      assertThrows(IllegalStateException.class, legacyFutureAdapter::start);
      assertDoesNotThrow(() -> legacyFutureAdapter.toCompletableFuture(SettableFuture.create()));
      legacyFutureAdapter.close();
      assertDoesNotThrow(legacyFutureAdapter::close);
      assertThrows(IllegalStateException.class, legacyFutureAdapter::stop);
      assertThrows(IllegalStateException.class,
          () -> legacyFutureAdapter.toCompletableFuture(SettableFuture.create()));
    }
  }

  @Test
  public void testCloseBeforeStart() {
    final LegacyFutureAdapter legacyFutureAdapter = LegacyFutureAdapter.newBuilder().build();
    assertDoesNotThrow(legacyFutureAdapter::close);
    assertThrows(IllegalStateException.class, legacyFutureAdapter::start);
  }

  @Test
  public void testStopBeforeStart() {
    try (LegacyFutureAdapter legacyFutureAdapter = LegacyFutureAdapter.newBuilder().build()) {
      assertDoesNotThrow(legacyFutureAdapter::stop);
      assertThrows(IllegalStateException.class, legacyFutureAdapter::start);
      assertDoesNotThrow(legacyFutureAdapter::close);
    }
  }

  @Test
  public void testStop() {
    try (LegacyFutureAdapter legacyFutureAdapter = LegacyFutureAdapter.newBuilder().build()) {
      legacyFutureAdapter.start();
      assertDoesNotThrow(legacyFutureAdapter::stop);
      assertThrows(IllegalStateException.class,
          () -> legacyFutureAdapter.toCompletableFuture(SettableFuture.create()));
      assertThrows(IllegalStateException.class, legacyFutureAdapter::stop);
      assertDoesNotThrow(legacyFutureAdapter::close);
      assertDoesNotThrow(legacyFutureAdapter::close);
    }
  }

  @Test
  public void testAfterStop() throws Exception {
    try (LegacyFutureAdapter legacyFutureAdapter = LegacyFutureAdapter.newBuilder().build()) {
      legacyFutureAdapter.start();
      final CompletableFuture<Integer> cf1 = legacyFutureAdapter
          .toCompletableFuture(TestHelper.delayedFuture(1, Duration.ofMillis(100)));
      final CompletableFuture<Integer> cf2 = legacyFutureAdapter
          .toCompletableFuture(TestHelper.delayedFuture(2, Duration.ofMillis(200)));
      assertFalse(cf1.isDone());
      Thread.sleep(110);
      assertTrue(cf1.isDone());
      assertFalse(cf2.isDone());
      legacyFutureAdapter.stop();
      assertFalse(cf2.isDone());
      Thread.sleep(100);
      assertTrue(cf2.isDone());
    }
  }

  @Test
  public void testAfterClose() throws Exception {
    final CompletableFuture<Integer> cf1;
    final CompletableFuture<Integer> cf2;
    try (LegacyFutureAdapter legacyFutureAdapter = LegacyFutureAdapter.newBuilder().build()) {
      legacyFutureAdapter.start();
      cf1 = legacyFutureAdapter
          .toCompletableFuture(TestHelper.delayedFuture(1, Duration.ofMillis(100)));
      cf2 = legacyFutureAdapter
          .toCompletableFuture(TestHelper.delayedFuture(2, Duration.ofMillis(200)));
      assertFalse(cf1.isDone());
      Thread.sleep(110);
      assertTrue(cf1.isDone());
      assertFalse(cf2.isDone());
    }
    assertFalse(cf2.isDone());
    Thread.sleep(100);
    assertFalse(cf2.isDone());
  }
}
