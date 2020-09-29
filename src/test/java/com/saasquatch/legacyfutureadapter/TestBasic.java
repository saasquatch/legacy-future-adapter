package com.saasquatch.legacyfutureadapter;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.google.common.util.concurrent.SettableFuture;

public class TestBasic {

  private static ExecutorService threadPool;
  private static LegacyFutureAdapter legacyFutureAdapter;

  @BeforeAll
  public static void beforeAll() {
    threadPool = Executors.newCachedThreadPool();
    legacyFutureAdapter = LegacyFutureAdapter.newBuilder().build();
    legacyFutureAdapter.start();
  }

  @AfterAll
  public static void afterAll() {
    threadPool.shutdown();
    legacyFutureAdapter.close();
  }

  @Test
  public void testBasic() throws Exception {
    final SettableFuture<Boolean> settableFuture = SettableFuture.create();
    CompletableFuture.runAsync(() -> {
      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      settableFuture.set(true);
    }, threadPool);
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

  @Test
  public void testFinishAfterClose() {
    final SettableFuture<Boolean> settableFuture = SettableFuture.create();
    CompletableFuture.runAsync(() -> {
      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      settableFuture.set(true);
    }, threadPool);
    final CompletableFuture<Boolean> adaptedFuture;
    try (LegacyFutureAdapter adapter2 = LegacyFutureAdapter.newBuilder().build()) {
      adapter2.start();
      adaptedFuture = adapter2.toCompletableFuture(settableFuture);
    }
    assertThrows(TimeoutException.class, () -> adaptedFuture.get(60, TimeUnit.MILLISECONDS));
  }

}
