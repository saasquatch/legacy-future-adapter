package com.saasquatch.legacyfutureadapter;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Builder for {@link LegacyFutureAdapter}. The builder itself is NOT thread safe.
 *
 * @author sli
 */
@NotThreadSafe
public final class LegacyFutureAdapterBuilder {

  private ThreadFactory eventLoopThreadFactory = null;

  LegacyFutureAdapterBuilder() {}

  /**
   * Sets the {@link ThreadFactory} to be used for creating the event loop thread. The
   * {@link ThreadFactory} will only be used to create one thread and one thread only.
   */
  public LegacyFutureAdapterBuilder setEventLoopThreadFactory(
      @Nonnull ThreadFactory eventLoopThreadFactory) {
    this.eventLoopThreadFactory = Objects.requireNonNull(eventLoopThreadFactory);
    return this;
  }

  @Nonnull
  public LegacyFutureAdapter build() {
    return new LegacyFutureAdapter(
        eventLoopThreadFactory == null ? Executors.defaultThreadFactory() : eventLoopThreadFactory);
  }

}
