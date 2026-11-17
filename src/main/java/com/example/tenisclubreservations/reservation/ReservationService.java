package com.example.tenisclubreservations.reservation;

import com.example.tenisclubreservations.common.ApiException;
import com.example.tenisclubreservations.common.Input;
import com.example.tenisclubreservations.common.MutationGuard;
import com.example.tenisclubreservations.court.Court;
import com.example.tenisclubreservations.court.CourtDao;
import com.example.tenisclubreservations.customer.Customer;
import com.example.tenisclubreservations.customer.CustomerDao;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationService {
  private static final Set<String> FIELDS = Set.of("courtNumber", "startTime", "endTime", "gameType", "phoneNumber", "name");
  private final ReservationDao reservations;
  private final CourtDao courts;
  private final CustomerDao customers;
  private final MutationGuard guard;

  @Transactional(readOnly = true)
  public ReservationResponse get(long id) {
    return ReservationResponse.from(find(id));
  }

  @Transactional(readOnly = true)
  public List<ReservationResponse> byCourt(int number) {
    Court court = court(number);
    return reservations.byCourt(court.getId()).stream().map(ReservationResponse::from).toList();
  }

  @Transactional(readOnly = true)
  public List<ReservationResponse> byPhone(String phone, boolean futureOnly) {
    return reservations.byPhone(phone == null ? null : phone(phone), futureOnly, Instant.now()).stream()
        .map(ReservationResponse::from).toList();
  }

  public ReservationResponse create(ReservationRequest request) {
    guard.lock();
    Reservation reservation = new Reservation();
    apply(reservation, request.fields(), false, Instant.now());
    reservations.persist(reservation);
    return ReservationResponse.from(reservation);
  }

  public ReservationResponse update(long id, ReservationRequest request, boolean partial) {
    guard.lock();
    Instant now = Instant.now();
    Reservation reservation = find(id);
    ensureFuture(reservation, now);
    apply(reservation, request.fields(), partial, now);
    reservations.flush();
    return ReservationResponse.from(reservation);
  }

  public void delete(long id) {
    guard.lock();
    Instant now = Instant.now();
    Reservation reservation = find(id);
    ensureFuture(reservation, now);
    reservation.softDelete();
  }

  private void apply(Reservation r, Map<String, Object> input, boolean partial, Instant now) {
    Input.fields(input, FIELDS, partial);
    if (partial && input.containsKey("phoneNumber") != input.containsKey("name")) {
      throw Input.invalid("Phone number and name must be supplied together");
    }
    Court court = input.containsKey("courtNumber") ? court(number(input.get("courtNumber"))) : r.getCourt();
    Instant start = input.containsKey("startTime") ? time(input.get("startTime")) : r.getStartTime();
    Instant end = input.containsKey("endTime") ? time(input.get("endTime")) : r.getEndTime();
    GameType type = input.containsKey("gameType") ? gameType(input.get("gameType")) : r.getGameType();
    String phone = input.containsKey("phoneNumber") ? phone(input.get("phoneNumber")) : r.getCustomer().getPhoneNumber();
    String name = input.containsKey("name") ? Input.name(input.get("name")) : r.getCustomer().getName();
    long minutes = Duration.between(start, end).toMinutes();
    if (!start.isAfter(now) || minutes < 1 || minutes > 180) {
      throw Input.invalid("Reservation must start in the future and last 1 to 180 minutes");
    }
    if (reservations.overlaps(court.getId(), start, end, r.getId())) {
      throw conflict("Court is already reserved");
    }
    Customer customer = customers.findByPhone(phone).orElse(null);
    if (customer != null && !customer.getName().equals(name)) {
      throw conflict("Phone number belongs to a customer with a different name");
    }
    if (customer == null) {
      customer = new Customer(name, phone);
      customers.persist(customer);
    }
    boolean priceChanged = r.getId() == null || !court.getId().equals(r.getCourt().getId())
        || !start.equals(r.getStartTime()) || !end.equals(r.getEndTime()) || type != r.getGameType();
    BigDecimal price = priceChanged ? price(court.getSurface().getPricePerMinute(), minutes, type) : r.getTotalPrice();
    r.update(court, customer, start, end, type, price);
  }

  static BigDecimal price(BigDecimal rate, long minutes, GameType type) {
    return rate.multiply(BigDecimal.valueOf(minutes))
        .multiply(type == GameType.DOUBLES ? new BigDecimal("1.5") : BigDecimal.ONE)
        .setScale(2, RoundingMode.HALF_UP);
  }

  static Instant time(Object value) {
    if (!(value instanceof String text)) throw Input.invalid("Expected offset timestamp");
    try {
      OffsetDateTime time = OffsetDateTime.parse(text);
      Instant instant = time.toInstant();
      if (time.getSecond() != 0 || time.getNano() != 0 || instant.getEpochSecond() % 60 != 0) {
        throw Input.invalid("Timestamp must be minute aligned");
      }
      return instant;
    } catch (DateTimeParseException ex) {
      throw Input.invalid("Expected offset timestamp");
    }
  }

  private static GameType gameType(Object value) {
    try {
      if (value instanceof String text) return GameType.valueOf(text);
    } catch (IllegalArgumentException ignored) {
      // Report invalid input below.
    }
    throw Input.invalid("Invalid game type");
  }

  private static String phone(Object value) {
    if (!(value instanceof String text) || text.trim().isEmpty() || text.trim().length() > 100) {
      throw Input.invalid("Phone number must contain 1 to 100 characters");
    }
    return text.trim();
  }

  private int number(Object value) {
    long number = Input.positiveInteger(value);
    if (number > Integer.MAX_VALUE) throw Input.invalid("Court number is too large");
    return (int) number;
  }

  private Court court(int number) {
    return courts.findByNumber(number).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Court not found"));
  }

  private Reservation find(long id) {
    return reservations.find(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Reservation not found"));
  }

  private void ensureFuture(Reservation reservation, Instant now) {
    if (!reservation.getStartTime().isAfter(now)) throw conflict("Reservation has already started");
  }

  private static ApiException conflict(String message) {
    return new ApiException(HttpStatus.CONFLICT, message);
  }
}
