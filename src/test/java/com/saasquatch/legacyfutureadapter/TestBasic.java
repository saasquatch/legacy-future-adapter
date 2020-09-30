package com.saasquatch.legacyfutureadapter;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

public class TestBasic {

  private static LegacyFutureAdapter legacyFutureAdapter;

  @BeforeAll
  public static void beforeAll() {
    legacyFutureAdapter = LegacyFutureAdapter.newBuilder()
        .setEventLoopThreadFactory(Executors.defaultThreadFactory()).build();
    legacyFutureAdapter.start();
  }

  @AfterAll
  public static void afterAll() {
    legacyFutureAdapter.close();
  }

  @Test
  public void testBasic() throws Exception {
    final ListenableFuture<Boolean> settableFuture =
        TestHelper.delayedFuture(true, Duration.ofMillis(50));
    final CompletableFuture<Boolean> adaptedFuture =
        legacyFutureAdapter.toCompletableFuture(settableFuture);
    assertThrows(TimeoutException.class, () -> adaptedFuture.get(30, TimeUnit.MILLISECONDS));
    assertTrue(adaptedFuture.get(30, TimeUnit.MILLISECONDS));
  }

  @Test
  public void testNeverCompleting() {
    final Future<Boolean> neverFuture = SettableFuture.create();
    final CompletableFuture<Boolean> adaptedFuture =
        legacyFutureAdapter.toCompletableFuture(neverFuture);
    assertThrows(TimeoutException.class, () -> adaptedFuture.get(100, TimeUnit.MILLISECONDS));
  }

}
