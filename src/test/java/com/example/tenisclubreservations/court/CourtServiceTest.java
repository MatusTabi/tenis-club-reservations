package com.example.tenisclubreservations.court;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.tenisclubreservations.common.ApiException;
import com.example.tenisclubreservations.common.MutationGuard;
import com.example.tenisclubreservations.reservation.ReservationDao;
import com.example.tenisclubreservations.surface.Surface;
import com.example.tenisclubreservations.surface.SurfaceDao;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

class CourtServiceTest {
  private final CourtDao courts = mock(CourtDao.class);
  private final SurfaceDao surfaces = mock(SurfaceDao.class);
  private final MutationGuard guard = mock(MutationGuard.class);
  private final ReservationDao reservations = mock(ReservationDao.class);
  private final CourtService service = new CourtService(courts, surfaces, guard, reservations);

  private Surface surface(long id, String name) {
    var surface = new Surface();
    ReflectionTestUtils.setField(surface, "id", id);
    surface.update(name, name.toLowerCase(Locale.ROOT), new BigDecimal("2.00"));
    return surface;
  }

  private Court court(long id, int number, Surface surface) {
    var court = new Court();
    ReflectionTestUtils.setField(court, "id", id);
    court.update(number, surface);
    return court;
  }

  private void assertStatus(int status, org.junit.jupiter.api.function.Executable action) {
    assertEquals(status, assertThrows(ApiException.class, action).getStatusCode().value());
  }

  @Test
  void createLocksChecksUniquenessAndPersistsRepresentation() {
    var surface = surface(2, "Clay");
    when(surfaces.find(2)).thenReturn(Optional.of(surface));
    var result = service.create(new CourtRequest(Map.of("courtNumber", 7, "surfaceId", 2)));
    assertEquals(7, result.courtNumber());
    assertEquals(2L, result.surface().id());
    assertEquals("Clay", result.surface().name());
    assertEquals(new BigDecimal("2.00"), result.surface().pricePerMinute());
    var order = inOrder(guard, surfaces, courts);
    order.verify(guard).lock();
    order.verify(surfaces).find(2);
    order.verify(courts).exists(7, null);
    order
        .verify(courts)
        .persist(argThat(c -> c.getCourtNumber() == 7 && c.getSurface() == surface));
  }

  @Test
  void getAndListMapDaoResults() {
    var first = court(10, 1, surface(2, "Clay"));
    var second = court(11, 3, surface(3, "Hard"));
    when(courts.find(10)).thenReturn(Optional.of(first));
    when(courts.list()).thenReturn(List.of(first, second));
    assertEquals(CourtResponse.from(first), service.get(10));
    assertEquals(List.of(CourtResponse.from(first), CourtResponse.from(second)), service.list());
    verifyNoInteractions(guard, surfaces);
  }

  @Test
  void missingCourtReturns404ForGetUpdateAndDelete() {
    assertStatus(404, () -> service.get(10));
    assertStatus(404, () -> service.update(10, new CourtRequest(Map.of("courtNumber", 2)), true));
    assertStatus(404, () -> service.delete(10));
    verify(guard, times(2)).lock();
    verifyNoInteractions(surfaces);
  }

  @Test
  void patchPreservesOmittedFieldsAndExcludesOwnIdFromDuplicateCheck() {
    var oldSurface = surface(2, "Clay");
    var newSurface = surface(3, "Hard");
    var court = court(10, 1, oldSurface);
    when(courts.find(10)).thenReturn(Optional.of(court));
    when(surfaces.find(3)).thenReturn(Optional.of(newSurface));
    var numberResult = service.update(10, new CourtRequest(Map.of("courtNumber", 4)), true);
    assertEquals(4, numberResult.courtNumber());
    assertSame(oldSurface, court.getSurface());
    var surfaceResult = service.update(10, new CourtRequest(Map.of("surfaceId", 3)), true);
    assertEquals(4, surfaceResult.courtNumber());
    assertSame(newSurface, court.getSurface());
    verify(courts, times(2)).exists(4, 10L);
    verify(guard, times(2)).lock();
  }

  @Test
  void fullUpdateReplacesBothFieldsAndAllowsMaximumCourtNumber() {
    var court = court(10, 1, surface(2, "Clay"));
    var replacement = surface(3, "Hard");
    when(courts.find(10)).thenReturn(Optional.of(court));
    when(surfaces.find(3)).thenReturn(Optional.of(replacement));
    var result =
        service.update(
            10, new CourtRequest(Map.of("courtNumber", Integer.MAX_VALUE, "surfaceId", 3)), false);
    assertEquals(Integer.MAX_VALUE, result.courtNumber());
    assertSame(replacement, court.getSurface());
    verify(courts).exists(Integer.MAX_VALUE, 10L);
  }

  static Stream<Object> invalidNumbers() {
    return Stream.of(
        0, -1, 1.5, "1", (long) Integer.MAX_VALUE + 1, new BigDecimal("9223372036854775808"));
  }

  @ParameterizedTest
  @MethodSource("invalidNumbers")
  void rejectsInvalidCourtNumbersWithoutPersisting(Object number) {
    assertStatus(
        400, () -> service.create(new CourtRequest(Map.of("courtNumber", number, "surfaceId", 2))));
    verify(courts, never()).persist(any());
    verifyNoInteractions(surfaces);
  }

  @Test
  void rejectsInvalidFieldsAndMissingOrInvalidSurfaceWithoutMutation() {
    var original = surface(2, "Clay");
    var court = court(10, 1, original);
    when(courts.find(10)).thenReturn(Optional.of(court));
    var nullField = new HashMap<String, Object>();
    nullField.put("surfaceId", null);
    for (var input :
        List.of(Map.<String, Object>of(), Map.<String, Object>of("other", 1), nullField)) {
      assertStatus(400, () -> service.update(10, new CourtRequest(input), true));
    }
    assertStatus(400, () -> service.update(10, new CourtRequest(Map.of("courtNumber", 2)), false));
    assertStatus(400, () -> service.update(10, new CourtRequest(Map.of("surfaceId", 0)), true));
    assertStatus(404, () -> service.update(10, new CourtRequest(Map.of("surfaceId", 99)), true));
    assertEquals(1, court.getCourtNumber());
    assertSame(original, court.getSurface());
    verify(courts, never()).exists(anyInt(), any());
  }

  @Test
  void duplicateNumberRejectsCreateAndUpdateWithoutSideEffects() {
    var surface = surface(2, "Clay");
    var court = court(10, 1, surface);
    when(surfaces.find(2)).thenReturn(Optional.of(surface));
    when(courts.find(10)).thenReturn(Optional.of(court));
    when(courts.exists(3, null)).thenReturn(true);
    when(courts.exists(3, 10L)).thenReturn(true);
    assertStatus(
        409, () -> service.create(new CourtRequest(Map.of("courtNumber", 3, "surfaceId", 2))));
    assertStatus(409, () -> service.update(10, new CourtRequest(Map.of("courtNumber", 3)), true));
    verify(courts, never()).persist(any());
    assertEquals(1, court.getCourtNumber());
    assertSame(surface, court.getSurface());
  }

  @Test
  void deleteLocksAndSoftDeletesWhileRetainingHistoricalValues() {
    var surface = surface(2, "Clay");
    var court = court(10, 1, surface);
    when(courts.find(10)).thenReturn(Optional.of(court));
    service.delete(10);
    var order = inOrder(guard, courts);
    order.verify(guard).lock();
    order.verify(courts).find(10);
    assertTrue(court.isDeleted());
    assertNull(court.getActiveNumber());
    assertEquals(1, court.getCourtNumber());
    assertSame(surface, court.getSurface());
  }
}
