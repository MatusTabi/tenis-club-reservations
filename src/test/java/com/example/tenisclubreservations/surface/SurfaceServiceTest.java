package com.example.tenisclubreservations.surface;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.tenisclubreservations.common.ApiException;
import com.example.tenisclubreservations.common.MutationGuard;
import com.example.tenisclubreservations.court.CourtDao;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SurfaceServiceTest {
  @Test
  void rejectsAssignedSurfaceDeletion() {
    SurfaceDao surfaces = mock(SurfaceDao.class);
    CourtDao courts = mock(CourtDao.class);
    MutationGuard guard = mock(MutationGuard.class);
    when(surfaces.find(1)).thenReturn(Optional.of(new Surface()));
    when(courts.assigned(1)).thenReturn(true);
    var service = new SurfaceService(surfaces, courts, guard);
    assertEquals(
        409, assertThrows(ApiException.class, () -> service.delete(1)).getStatusCode().value());
    verify(guard).lock();
  }

  @Test
  void checksNormalizedDuplicateName() {
    SurfaceDao surfaces = mock(SurfaceDao.class);
    when(surfaces.exists(eq("clay"), isNull())).thenReturn(true);
    var service = new SurfaceService(surfaces, mock(CourtDao.class), mock(MutationGuard.class));
    assertEquals(
        409,
        assertThrows(
                ApiException.class,
                () ->
                    service.create(
                        new SurfaceRequest(Map.of("name", " Clay ", "pricePerMinute", 1))))
            .getStatusCode()
            .value());
  }

  @Test
  void patchRetainsOmittedPriceAndRejectsInvalidFields() {
    SurfaceDao surfaces = mock(SurfaceDao.class);
    var entity = new Surface();
    entity.update("Clay", "clay", new java.math.BigDecimal("2.00"));
    when(surfaces.find(1)).thenReturn(Optional.of(entity));
    var service = new SurfaceService(surfaces, mock(CourtDao.class), mock(MutationGuard.class));
    var result = service.update(1, new SurfaceRequest(Map.of("name", "Grass")), true);
    assertEquals("Grass", result.name());
    assertEquals(new java.math.BigDecimal("2.00"), result.pricePerMinute());
    assertThrows(ApiException.class, () -> service.update(1, new SurfaceRequest(Map.of()), true));
    assertThrows(
        ApiException.class, () -> service.update(1, new SurfaceRequest(Map.of("other", 1)), true));
  }

  @Test
  void createTrimsNameNormalizesKeyAndPersists() {
    SurfaceDao surfaces = mock(SurfaceDao.class);
    MutationGuard guard = mock(MutationGuard.class);
    var service = new SurfaceService(surfaces, mock(CourtDao.class), guard);
    var result = service.create(new SurfaceRequest(Map.of("name", " Clay ", "pricePerMinute", 0)));
    assertEquals("Clay", result.name());
    assertEquals(new BigDecimal("0.00"), result.pricePerMinute());
    verify(guard).lock();
    verify(surfaces).exists("clay", null);
    verify(surfaces)
        .persist(org.mockito.ArgumentMatchers.argThat(s -> s.getActiveName().equals("clay")));
  }

  @Test
  void updatePreservesOmittedNameAndExcludesOwnIdFromDuplicateCheck() {
    SurfaceDao surfaces = mock(SurfaceDao.class);
    var entity = new Surface();
    ReflectionTestUtils.setField(entity, "id", 1L);
    entity.update("Clay", "clay", new BigDecimal("2.00"));
    when(surfaces.find(1)).thenReturn(Optional.of(entity));
    var service = new SurfaceService(surfaces, mock(CourtDao.class), mock(MutationGuard.class));
    var result = service.update(1, new SurfaceRequest(Map.of("pricePerMinute", 1000)), true);
    assertEquals("Clay", result.name());
    assertEquals(new BigDecimal("1000.00"), result.pricePerMinute());
    verify(surfaces).exists("clay", 1L);
  }

  @Test
  void rejectsInvalidUpdatesAndConflictsWithoutChangingEntity() {
    SurfaceDao surfaces = mock(SurfaceDao.class);
    var entity = new Surface();
    ReflectionTestUtils.setField(entity, "id", 1L);
    entity.update("Clay", "clay", new BigDecimal("2.00"));
    when(surfaces.find(1)).thenReturn(Optional.of(entity));
    when(surfaces.exists("grass", 1L)).thenReturn(true);
    var service = new SurfaceService(surfaces, mock(CourtDao.class), mock(MutationGuard.class));
    var nullField = new HashMap<String, Object>();
    nullField.put("name", null);
    for (var input :
        java.util.List.of(
            nullField,
            Map.<String, Object>of("name", "  "),
            Map.<String, Object>of("name", "x".repeat(51)),
            Map.<String, Object>of("pricePerMinute", "1"),
            Map.<String, Object>of("pricePerMinute", new BigDecimal("1.001")))) {
      assertEquals(
          400,
          assertThrows(ApiException.class, () -> service.update(1, new SurfaceRequest(input), true))
              .getStatusCode()
              .value());
    }
    assertEquals(
        400,
        assertThrows(
                ApiException.class,
                () -> service.update(1, new SurfaceRequest(Map.of("name", "Grass")), false))
            .getStatusCode()
            .value());
    assertEquals(
        409,
        assertThrows(
                ApiException.class,
                () -> service.update(1, new SurfaceRequest(Map.of("name", " Grass ")), true))
            .getStatusCode()
            .value());
    assertEquals("Clay", entity.getName());
    assertEquals("clay", entity.getActiveName());
    assertEquals(new BigDecimal("2.00"), entity.getPricePerMinute());
  }

  @Test
  void deleteRejectsAssignedAndSoftDeletesUnassignedSurface() {
    SurfaceDao surfaces = mock(SurfaceDao.class);
    CourtDao courts = mock(CourtDao.class);
    MutationGuard guard = mock(MutationGuard.class);
    var entity = new Surface();
    entity.update("Clay", "clay", new BigDecimal("2.00"));
    when(surfaces.find(1)).thenReturn(Optional.of(entity));
    when(courts.assigned(1)).thenReturn(true, false);
    var service = new SurfaceService(surfaces, courts, guard);
    assertEquals(
        409, assertThrows(ApiException.class, () -> service.delete(1)).getStatusCode().value());
    assertFalse(entity.isDeleted());
    service.delete(1);
    assertTrue(entity.isDeleted());
    assertNull(entity.getActiveName());
    assertEquals("Clay", entity.getName());
    verify(guard, org.mockito.Mockito.times(2)).lock();
  }

  @Test
  void missingSurfaceReturns404WithoutPersistingOrCheckingAssignments() {
    SurfaceDao surfaces = mock(SurfaceDao.class);
    CourtDao courts = mock(CourtDao.class);
    var service = new SurfaceService(surfaces, courts, mock(MutationGuard.class));
    assertEquals(
        404, assertThrows(ApiException.class, () -> service.get(1)).getStatusCode().value());
    assertEquals(
        404,
        assertThrows(
                ApiException.class,
                () -> service.update(1, new SurfaceRequest(Map.of("name", "Clay")), true))
            .getStatusCode()
            .value());
    assertEquals(
        404, assertThrows(ApiException.class, () -> service.delete(1)).getStatusCode().value());
    verify(courts, never()).assigned(1);
    verify(surfaces, never()).persist(any());
  }
}
