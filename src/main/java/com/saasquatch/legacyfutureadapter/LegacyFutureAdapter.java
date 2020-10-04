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
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

/**
 * An event loop based tool that waits for multiple {@link Future}s that do not support callbacks
 * with a single thread.
 *
 * @see #newBuilder()
 * @author sli
 */
@ThreadSafe
public final class LegacyFutureAdapter implements Closeable {

  // mutable reference
  private Thread eventLoopThread;
  // mutable reference
  private LegacyFutureAdapterState state = LegacyFutureAdapterState.CREATED;
  private final ReadWriteLock stateLock = new ReentrantReadWriteLock();
  private final BlockingQueue<FutureHolder> futureHolders = new LinkedBlockingQueue<>();
  private final ThreadFactory eventLoopThreadFactory;

  LegacyFutureAdapter(@Nonnull ThreadFactory eventLoopThreadFactory) {
    this.eventLoopThreadFactory = eventLoopThreadFactory;
  }

  /**
   * @return A new instance of {@link LegacyFutureAdapterBuilder}
   * @see #create()
   */
  public static LegacyFutureAdapterBuilder newBuilder() {
    return new LegacyFutureAdapterBuilder();
  }

  /**
   * Convenience method for {@code newBuilder().build()}
   *
   * @see #newBuilder()
   */
  public static LegacyFutureAdapter create() {
    return newBuilder().build();
  }

  /**
   * Start the event loop thread. This method has to be called before
   * {@link #toCompletableFuture(Future)} can be used.
   */
  public void start() {
    stateLock.writeLock().lock();
    try {
      if (!state.canStart) {
        throw invalidStateException(state);
      }
      state = LegacyFutureAdapterState.STARTED;
      eventLoopThread = eventLoopThreadFactory.newThread(this::doEventLoop);
      eventLoopThread.start();
    } finally {
      stateLock.writeLock().unlock();
    }
  }

  /**
   * Stop accepting new {@link Future}s but keep the event loop thread running. Note that
   * {@link #stop()} can only be called once and can only be called after {@link #start()} and
   * cannot be called after {@link #close()}. Once {@link #stop()} is called,
   * {@link #toCompletableFuture(Future)} will no longer work, but since the event loop is kept
   * running, all the incomplete {@link CompletableFuture}s will eventually complete.
   */
  public void stop() {
    stateLock.writeLock().lock();
    try {
      if (!state.canStop) {
        throw invalidStateException(state);
      }
      state = LegacyFutureAdapterState.STOPPED;
    } finally {
      stateLock.writeLock().unlock();
    }
  }

  /**
   * Stop accepting new {@link Future}s and stop the event loop thread. To be compliant with
   * {@link Closeable}, this method can be called multiple times and it has no effect after it's
   * been called once. Once {@link #close()} is called, {@link #toCompletableFuture(Future)} will no
   * longer work, and all the unfinished {@link CompletableFuture}s, even those with explicit
   * timeouts, will be left incomplete.
   */
  @Override
  public void close() {
    stateLock.writeLock().lock();
    try {
      state = LegacyFutureAdapterState.CLOSED;
      if (eventLoopThread != null) {
        eventLoopThread.interrupt();
      }
    } finally {
      stateLock.writeLock().unlock();
    }
  }

  /**
   * Convert the given {@link Future} into a {@link CompletableFuture} without a timeout. Only use
   * this method if you are certain that the given {@link Future} will actually finish. If the
   * future hangs forever, a reference of it will be kept in memory forever.
   *
   * @see #toCompletableFuture(Future, Duration)
   */
  public <T> CompletableFuture<T> toCompletableFuture(@Nonnull Future<T> f) {
    return toCf(f, 0);
  }

  /**
   * Convert the given {@link Future} into a {@link CompletableFuture} with a timeout. If the given
   * {@link Future} does not complete within the given timeout, then the result
   * {@link CompletableFuture} will complete exceptionally with a {@link TimeoutException}.
   *
   * @param timeout The timeout to be applied. It has to be between 1 nanosecond and
   *        {@link Long#MAX_VALUE} nanoseconds (292 years). The timeout starts counting when this
   *        method is called.
   * @throws ArithmeticException if the timeout is larger than {@link Long#MAX_VALUE} nanoseconds
   * @see #toCompletableFuture(Future)
   */
  public <T> CompletableFuture<T> toCompletableFuture(@Nonnull Future<T> f,
      @Nonnull Duration timeout) {
    final long nanos = timeout.toNanos(); // This will throw ArithmeticException for overflow
    if (nanos < 1) {
      throw new IllegalArgumentException("timeout has to be at least 1 nanosecond");
    }
    return toCf(f, nanos);
  }

  /**
   * @return all the {@link Future}s currently in the queue
   */
  public List<Future<Object>> getQueuedFutures() {
    return futureHolders.stream().map(futureHolder -> futureHolder.f)
        .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
  }

  private LegacyFutureAdapterState getCurrentState() {
    stateLock.readLock().lock();
    try {
      return state;
    } finally {
      stateLock.readLock().unlock();
    }
  }

  private <T> CompletableFuture<T> toCf(Future<T> f, long timeoutNanos) {
    Objects.requireNonNull(f);
    final LegacyFutureAdapterState currentState = getCurrentState();
    if (!currentState.acceptFutures) {
      throw invalidStateException(currentState);
    }
    final CompletableFuture<T> cf = new CompletableFuture<>();
    /*
     * If the future is already completed, then return a completed CompletableFuture in the caller
     * thread
     */
    if (!potentiallyCompleteFuture(f, cf, 0, 0)) {
      futureHolders.add(new FutureHolder(f, cf, timeoutNanos));
    }
    return cf;
  }

  private void doEventLoop() {
    while (getCurrentState().runEventLoop) {
      doSingleLoop();
    }
  }

  private void doSingleLoop() {
    final FutureHolder futureHolder;
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

  // visible for testing
  static <T> boolean potentiallyCompleteFuture(@Nonnull Future<T> f,
      @Nonnull CompletableFuture<T> cf, long elapsedNanos, long timeoutNanos) {
    if (f.isDone()) {
      try {
        return cf.complete(f.get());
      } catch (ExecutionException e) {
        return cf.completeExceptionally(e.getCause());
      } catch (Throwable e) {
        // This covers CancellationException
        return cf.completeExceptionally(e);
      }
    } else if (timeoutNanos > 0 && elapsedNanos - timeoutNanos > 0) {
      return cf.completeExceptionally(new TimeoutException());
    }
    return false;
  }

  private static RuntimeException invalidStateException(LegacyFutureAdapterState state) {
    return new IllegalStateException("Invalid state: " + state);
  }

  private static enum LegacyFutureAdapterState {

    CREATED(true, false, false, false), STARTED(false, true, true, true), STOPPED(false, false,
        true, false), CLOSED(false, false, false, false),;

    final boolean canStart;
    final boolean acceptFutures;
    final boolean runEventLoop;
    final boolean canStop;

    LegacyFutureAdapterState(boolean canStart, boolean acceptFutures, boolean runEventLoop,
        boolean canStop) {
      this.canStart = canStart;
      this.acceptFutures = acceptFutures;
      this.runEventLoop = runEventLoop;
      this.canStop = canStop;
    }

  }

  private static final class FutureHolder {

    final long startNanoTime = System.nanoTime();
    final Future<Object> f;
    final CompletableFuture<Object> cf;
    final long timeoutNanos;

    @SuppressWarnings("unchecked")
    FutureHolder(Future<?> f, CompletableFuture<?> cf, long timeoutNanos) {
      this.f = (Future<Object>) f;
      this.cf = (CompletableFuture<Object>) cf;
      this.timeoutNanos = timeoutNanos;
    }

  }

}
