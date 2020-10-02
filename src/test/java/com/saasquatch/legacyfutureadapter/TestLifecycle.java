package com.saasquatch.legacyfutureadapter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

}
