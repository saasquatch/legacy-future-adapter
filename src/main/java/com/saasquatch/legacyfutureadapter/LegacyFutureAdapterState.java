package com.saasquatch.legacyfutureadapter;

enum LegacyFutureAdapterState {

  /**
   * The {@link LegacyFutureAdapter} has just been created
   */
  CREATED(true, false, false, false),
  /**
   * {@link LegacyFutureAdapter#start()} has just been called
   */
  STARTED(false, true, true, true),
  /**
   * {@link LegacyFutureAdapter#stop()} has been called
   */
  STOPPED(false, false, true, false),
  /**
   * {@link LegacyFutureAdapter#close()} has been called
   */
  CLOSED(false, false, false, false),
  /**/;

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
