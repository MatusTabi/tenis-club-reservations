package com.example.tenisclubreservations.reservation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.example.tenisclubreservations.common.ApiException;
import com.example.tenisclubreservations.common.MutationGuard;
import com.example.tenisclubreservations.court.Court;
import com.example.tenisclubreservations.court.CourtDao;
import com.example.tenisclubreservations.customer.Customer;
import com.example.tenisclubreservations.customer.CustomerDao;
import com.example.tenisclubreservations.surface.Surface;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReservationServiceTest {
  private final ReservationDao reservations = mock(ReservationDao.class);
  private final CourtDao courts = mock(CourtDao.class);
  private final CustomerDao customers = mock(CustomerDao.class);
  private final MutationGuard guard = mock(MutationGuard.class);
  private final ReservationService service = new ReservationService(reservations, courts, customers, guard);
  private final Court court = mock(Court.class);
  private final Surface surface = mock(Surface.class);
  private final Instant start = Instant.now().truncatedTo(ChronoUnit.MINUTES).plus(1, ChronoUnit.DAYS);

  @BeforeEach
  void setUp() {
    lenient().when(courts.findByNumber(1)).thenReturn(Optional.of(court));
    lenient().when(court.getId()).thenReturn(10L);
    lenient().when(court.getCourtNumber()).thenReturn(1);
    lenient().when(court.getSurface()).thenReturn(surface);
    lenient().when(surface.getPricePerMinute()).thenReturn(new BigDecimal("1.00"));
  }

  private Map<String, Object> input(int minutes) {
    return new HashMap<>(Map.of("courtNumber", 1, "startTime", start.toString(), "endTime", start.plus(minutes, ChronoUnit.MINUTES).toString(), "gameType", "SINGLES", "phoneNumber", "123", "name", "Alice"));
  }

  private Reservation existing(Instant begin) {
    Reservation r = mock(Reservation.class);
    Customer customer = new Customer("Alice", "123");
    lenient().when(r.getId()).thenReturn(7L);
    lenient().when(r.getCourt()).thenReturn(court);
    lenient().when(r.getCustomer()).thenReturn(customer);
    lenient().when(r.getStartTime()).thenReturn(begin);
    lenient().when(r.getEndTime()).thenReturn(begin.plusSeconds(3600));
    lenient().when(r.getGameType()).thenReturn(GameType.SINGLES);
    lenient().when(r.getTotalPrice()).thenReturn(new BigDecimal("60.00"));
    when(reservations.find(7)).thenReturn(Optional.of(r));
    return r;
  }

  @Test
  void rejectsDurationsOutsideBoundsBeforeWriting() {
    for (int duration : new int[] {-1, 0, 181}) {
      assertEquals(400, assertThrows(ApiException.class, () -> service.create(new ReservationRequest(input(duration)))).getStatusCode().value());
    }
    verify(customers, never()).persist(any());
    verify(reservations, never()).persist(any());
  }

  @Test
  void overlapAndCustomerNameMismatchRejectWithoutCreatingCustomer() {
    when(reservations.overlaps(eq(10L), any(), any(), isNull())).thenReturn(true);
    assertEquals(409, assertThrows(ApiException.class, () -> service.create(new ReservationRequest(input(60)))).getStatusCode().value());
    when(reservations.overlaps(eq(10L), any(), any(), isNull())).thenReturn(false);
    when(customers.findByPhone("123")).thenReturn(Optional.of(new Customer("Bob", "123")));
    assertEquals(409, assertThrows(ApiException.class, () -> service.create(new ReservationRequest(input(60)))).getStatusCode().value());
    verify(customers, never()).persist(any());
  }

  @Test
  void rejectsStartedMutationsAndMissingReservation() {
    existing(Instant.now().minusSeconds(60));
    assertEquals(409, assertThrows(ApiException.class, () -> service.delete(7)).getStatusCode().value());
    assertEquals(409, assertThrows(ApiException.class, () -> service.update(7, new ReservationRequest(Map.of("gameType", "DOUBLES")), true)).getStatusCode().value());
    assertEquals(404, assertThrows(ApiException.class, () -> service.get(999)).getStatusCode().value());
  }

  @Test
  void validatesPatchAndPutAndExcludesCurrentReservationFromOverlap() {
    existing(start);
    assertEquals(400, assertThrows(ApiException.class, () -> service.update(7, new ReservationRequest(Map.of("name", "Bob")), true)).getStatusCode().value());
    assertEquals(400, assertThrows(ApiException.class, () -> service.update(7, new ReservationRequest(Map.of("gameType", "SINGLES")), false)).getStatusCode().value());
    when(reservations.overlaps(eq(10L), eq(start), eq(start.plusSeconds(3600)), eq(7L))).thenReturn(true);
    assertEquals(409, assertThrows(ApiException.class, () -> service.update(7, new ReservationRequest(Map.of("gameType", "DOUBLES")), true)).getStatusCode().value());
    verify(reservations).overlaps(10L, start, start.plusSeconds(3600), 7L);
  }
}
