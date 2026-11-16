package com.example.tenisclubreservations.surface;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.tenisclubreservations.common.ApiException;
import com.example.tenisclubreservations.common.MutationGuard;
import com.example.tenisclubreservations.court.CourtDao;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

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
}
