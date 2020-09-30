package com.saasquatch.legacyfutureadapter;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import javax.annotation.Nonnull;

public final class LegacyFutureAdapterBuilder {

  private ThreadFactory eventLoopThreadFactory = null;

  LegacyFutureAdapterBuilder() {}

  public LegacyFutureAdapterBuilder setEventLoopThreadFactory(
      ThreadFactory eventLoopThreadFactory) {
    this.eventLoopThreadFactory = Objects.requireNonNull(eventLoopThreadFactory);
    return this;
  }

  @Nonnull
  public LegacyFutureAdapter build() {
    return new LegacyFutureAdapter(
        eventLoopThreadFactory == null ? Executors.defaultThreadFactory() : eventLoopThreadFactory);
  }

}
