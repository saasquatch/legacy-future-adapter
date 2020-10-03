package com.saasquatch.legacyfutureadapter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.google.common.util.concurrent.Futures;
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

  @Test
  public void testFailedFutures() {
    final CompletableFuture<Object> npeFuture = legacyFutureAdapter
        .toCompletableFuture(Futures.immediateFailedFuture(new NullPointerException()));
    try {
      npeFuture.get(10, TimeUnit.MILLISECONDS);
      fail();
    } catch (ExecutionException e) {
      assertTrue(e.getCause() instanceof NullPointerException);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    final CompletableFuture<Object> cancelledFuture =
        legacyFutureAdapter.toCompletableFuture(Futures.immediateCancelledFuture());
    assertTrue(cancelledFuture.isCancelled());
  }

  @Test
  public void testLotsOfFutures() {
    final int[] ints = IntStream.range(0, 1024).toArray();
    final List<Future<Integer>> futures = Arrays.stream(ints)
        .mapToObj(i -> TestHelper.delayedFuture(i,
            Duration.ofMillis(ThreadLocalRandom.current().nextInt(10, 1000))))
        .collect(Collectors.toList());
    final List<CompletableFuture<Integer>> cfs =
        futures.stream().map(legacyFutureAdapter::toCompletableFuture).collect(Collectors.toList());
    final int[] resultInts = cfs.stream().mapToInt(f -> {
      try {
        return f.get(2, TimeUnit.SECONDS);
      } catch (InterruptedException | ExecutionException | TimeoutException e) {
        throw new RuntimeException(e);
      }
    }).toArray();
    assertArrayEquals(ints, resultInts);
  }

}
