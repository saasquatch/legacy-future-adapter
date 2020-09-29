package com.saasquatch.legacyfutureadapter;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import javax.annotation.Nonnull;

public final class LegacyFutureAdapterBuilder {

  private long defaultTimeoutNanos = 0;
  private ThreadFactory eventLoopThreadFactory = null;

  LegacyFutureAdapterBuilder() {}

  public LegacyFutureAdapterBuilder setDefaultTimeout(@Nonnull Duration defaultTimeout) {
    final long nanos = defaultTimeout.toNanos();
    if (nanos < 1) {
      throw new IllegalArgumentException("defaultTimeout has to be at least 1 nanosecond");
    }
    this.defaultTimeoutNanos = nanos;
    return this;
  }

  public LegacyFutureAdapterBuilder setEventLoopThreadFactory(
      ThreadFactory eventLoopThreadFactory) {
    this.eventLoopThreadFactory = Objects.requireNonNull(eventLoopThreadFactory);
    return this;
  }

  @Nonnull
  public LegacyFutureAdapter build() {
    return new LegacyFutureAdapter(defaultTimeoutNanos,
        eventLoopThreadFactory == null ? Executors.defaultThreadFactory() : eventLoopThreadFactory);
  }

}
