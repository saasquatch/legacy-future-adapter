package com.saasquatch.legacyfutureadapter;

import static com.saasquatch.legacyfutureadapter.LegacyFutureAdapter.potentiallyCompleteFuture;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

public class TestCompletingFuture {

  @Test
  public void testCompleted() {
    final ListenableFuture<Object> f = Futures.immediateFuture(1);
    {
      final CompletableFuture<Object> cf = new CompletableFuture<>();
      potentiallyCompleteFuture(f, cf, 0, 0);
      assertTrue(cf.isDone());
      assertFalse(cf.isCancelled());
      assertFalse(cf.isCompletedExceptionally());
    }
    {
      final CompletableFuture<Object> cf = new CompletableFuture<>();
      potentiallyCompleteFuture(f, cf, 2, 1);
      assertTrue(cf.isDone());
      assertFalse(cf.isCancelled());
      assertFalse(cf.isCompletedExceptionally());
    }
  }

  @Test
  public void testCancelled() {
    final ListenableFuture<Object> f = Futures.immediateCancelledFuture();
    {
      final CompletableFuture<Object> cf = new CompletableFuture<>();
      potentiallyCompleteFuture(f, cf, 0, 0);
      assertTrue(cf.isDone());
      assertTrue(cf.isCancelled());
      assertTrue(cf.isCompletedExceptionally());
    }
    {
      final CompletableFuture<Object> cf = new CompletableFuture<>();
      potentiallyCompleteFuture(f, cf, 2, 1);
      assertTrue(cf.isDone());
      assertTrue(cf.isCancelled());
      assertTrue(cf.isCompletedExceptionally());
    }
  }

  @Test
  public void testErrored() {
    final ListenableFuture<Object> f = Futures.immediateFailedFuture(new IOException());
    {
      final CompletableFuture<Object> cf = new CompletableFuture<>();
      potentiallyCompleteFuture(f, cf, 0, 0);
      assertTrue(cf.isDone());
      assertFalse(cf.isCancelled());
      assertTrue(cf.isCompletedExceptionally());
    }
    {
      final CompletableFuture<Object> cf = new CompletableFuture<>();
      potentiallyCompleteFuture(f, cf, 2, 1);
      assertTrue(cf.isDone());
      assertFalse(cf.isCancelled());
      assertTrue(cf.isCompletedExceptionally());
    }
  }

  @Test
  public void testIncomplete() {
    final ListenableFuture<Object> f = SettableFuture.create();
    {
      final CompletableFuture<Object> cf = new CompletableFuture<>();
      potentiallyCompleteFuture(f, cf, 0, 0);
      assertFalse(cf.isDone());
      assertFalse(cf.isCancelled());
      assertFalse(cf.isCompletedExceptionally());
    }
    {
      final CompletableFuture<Object> cf = new CompletableFuture<>();
      potentiallyCompleteFuture(f, cf, 2, 1);
      assertTrue(cf.isDone());
      assertFalse(cf.isCancelled());
      assertTrue(cf.isCompletedExceptionally());
    }
  }

  @Test
  public void testTimeout() throws Exception {
    final ListenableFuture<Object> f = SettableFuture.create();
    {
      final CompletableFuture<Object> cf = new CompletableFuture<>();
      potentiallyCompleteFuture(f, cf, 1, 2);
      assertFalse(cf.isDone());
      assertFalse(cf.isCancelled());
      assertFalse(cf.isCompletedExceptionally());
    }
    {
      final CompletableFuture<Object> cf = new CompletableFuture<>();
      potentiallyCompleteFuture(f, cf, Long.MAX_VALUE, 0);
      assertFalse(cf.isDone());
      assertFalse(cf.isCancelled());
      assertFalse(cf.isCompletedExceptionally());
    }
    {
      final CompletableFuture<Object> cf = new CompletableFuture<>();
      // nanoTime overflow
      potentiallyCompleteFuture(f, cf, Long.MIN_VALUE + Integer.MAX_VALUE, Long.MAX_VALUE);
      assertTrue(cf.isDone());
      assertFalse(cf.isCancelled());
      assertTrue(cf.isCompletedExceptionally());
    }
  }

}
