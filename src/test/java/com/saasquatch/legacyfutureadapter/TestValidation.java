package com.saasquatch.legacyfutureadapter;

import static org.junit.jupiter.api.Assertions.assertThrows;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.google.common.util.concurrent.SettableFuture;

public class TestValidation {

  private static LegacyFutureAdapter legacyFutureAdapter;

  @BeforeAll
  public static void beforeAll() {
    legacyFutureAdapter = LegacyFutureAdapter.newBuilder().build();
    legacyFutureAdapter.start();
  }

  @AfterAll
  public static void afterAll() {
    legacyFutureAdapter.close();
  }

  @Test
  public void testBuilder() {
    assertThrows(NullPointerException.class,
        () -> LegacyFutureAdapter.newBuilder().setEventLoopThreadFactory(null));
  }

  @Test
  public void test() {
    assertThrows(NullPointerException.class, () -> legacyFutureAdapter.toCompletableFuture(null));
    assertThrows(NullPointerException.class,
        () -> legacyFutureAdapter.toCompletableFuture(null, Duration.ofSeconds(1)));
    assertThrows(NullPointerException.class,
        () -> legacyFutureAdapter.toCompletableFuture(SettableFuture.create(), null));
    assertThrows(IllegalArgumentException.class,
        () -> legacyFutureAdapter.toCompletableFuture(SettableFuture.create(), Duration.ZERO));
  }

}
