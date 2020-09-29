package com.saasquatch.legacyfutureadapter;

import java.io.Closeable;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

public final class LegacyFutureAdapter implements Closeable {

  private Thread eventLoopThread;
  private final BlockingQueue<FutureHolder<?>> futureHolders = new LinkedBlockingQueue<>();
  private final AtomicReference<LegacyFutureAdapterState> stateRef =
      new AtomicReference<>(LegacyFutureAdapterState.CREATED);
  // 0 means no timeout
  private final long defaultTimeoutNanos;
  private final ThreadFactory eventLoopThreadFactory;

  LegacyFutureAdapter(long defaultTimeoutNanos, ThreadFactory eventLoopThreadFactory) {
    this.defaultTimeoutNanos = defaultTimeoutNanos;
    this.eventLoopThreadFactory = eventLoopThreadFactory;
  }

  public static LegacyFutureAdapterBuilder newBuilder() {
    return new LegacyFutureAdapterBuilder();
  }

  public void start() {
    if (!stateRef.compareAndSet(LegacyFutureAdapterState.CREATED,
        LegacyFutureAdapterState.STARTED)) {
      throw new IllegalStateException("Invalid state: " + stateRef.get());
    }
    eventLoopThread = eventLoopThreadFactory.newThread(this::doEventLoop);
    eventLoopThread.start();
  }

  @Override
  public void close() {
    stateRef.set(LegacyFutureAdapterState.STOPPED);
    if (eventLoopThread != null) {
      eventLoopThread.interrupt();
    }
  }

  public <T> CompletableFuture<T> toCompletableFuture(@Nonnull Future<T> f) {
    return toCf(f, 0);
  }

  public <T> CompletableFuture<T> toCompletableFuture(@Nonnull Future<T> f,
      @Nonnull Duration timeout) {
    final long nanos = timeout.toNanos();
    if (nanos < 1) {
      throw new IllegalArgumentException("timeout has to be at least 1 nanosecond");
    }
    return toCf(f, nanos);
  }

  public List<Future<?>> getQueuedFutures() {
    final List<Future<Object>> futures =
        futureHolders.stream().map(futureHolder -> futureHolder.f).collect(Collectors.toList());
    return Collections.unmodifiableList(futures);
  }

  private <T> CompletableFuture<T> toCf(Future<T> f, long timeoutNanos) {
    Objects.requireNonNull(f);
    if (stateRef.get() != LegacyFutureAdapterState.STARTED) {
      throw new IllegalStateException("Invalid state: " + stateRef.get());
    }
    if (f instanceof CompletableFuture) {
      return (CompletableFuture<T>) f;
    }
    final CompletableFuture<T> cf = new CompletableFuture<>();
    if (!potentiallyCompleteFuture(f, cf, 0, 0)) {
      futureHolders.add(new FutureHolder<>(f, cf, Math.max(timeoutNanos, defaultTimeoutNanos)));
    }
    return cf;
  }

  private void doEventLoop() {
    while (stateRef.get() == LegacyFutureAdapterState.STARTED) {
      doSingleLoop();
    }
  }

  private void doSingleLoop() {
    final FutureHolder<?> futureHolder;
    try {
      futureHolder = futureHolders.take();
    } catch (InterruptedException e) {
      // This means someone called close
      return;
    }
    final long elapsedNanos = System.nanoTime() - futureHolder.startNanoTime;
    if (!potentiallyCompleteFuture(futureHolder.f, futureHolder.cf, elapsedNanos,
        futureHolder.timeoutNanos)) {
      // The future has not been completed. Put it back into the queue.
      futureHolders.add(futureHolder);
    }
  }

  private static <T> boolean potentiallyCompleteFuture(Future<T> f, CompletableFuture<T> cf,
      long elapsedNanos, long timeoutNanos) {
    if (f.isDone()) {
      try {
        return cf.complete(f.get());
      } catch (ExecutionException e) {
        return cf.completeExceptionally(e.getCause());
      } catch (Throwable e) {
        // This covers CancellationException
        return cf.completeExceptionally(e);
      }
    } else if (timeoutNanos > 0 && elapsedNanos > timeoutNanos) {
      return cf.completeExceptionally(new TimeoutException());
    }
    return false;
  }

  private static enum LegacyFutureAdapterState {
    CREATED, STARTED, STOPPED,;
  }

  private static class FutureHolder<T> {

    final long startNanoTime = System.nanoTime();
    final Future<Object> f;
    final CompletableFuture<Object> cf;
    final long timeoutNanos;

    @SuppressWarnings("unchecked")
    FutureHolder(Future<T> f, CompletableFuture<T> cf, long timeoutNanos) {
      this.f = (Future<Object>) f;
      this.cf = (CompletableFuture<Object>) cf;
      this.timeoutNanos = timeoutNanos;
    }

  }

}
