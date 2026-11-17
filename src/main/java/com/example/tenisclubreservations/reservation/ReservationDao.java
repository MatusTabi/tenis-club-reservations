package com.example.tenisclubreservations.reservation;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ReservationDao {
  private final EntityManager em;

  public Optional<Reservation> find(long id) {
    return em.createQuery("select r from Reservation r where r.id = :id and r.removedAt is null", Reservation.class)
        .setParameter("id", id).getResultStream().findFirst();
  }

  public boolean overlaps(long courtId, Instant start, Instant end, Long except) {
    return em.createQuery("select count(r) from Reservation r where r.removedAt is null and r.court.id = :court and r.startTime < :end and r.endTime > :start and (:id is null or r.id <> :id)", Long.class)
        .setParameter("court", courtId).setParameter("start", start).setParameter("end", end)
        .setParameter("id", except).getSingleResult() > 0;
  }

  public boolean blocksCourtDeletion(long courtId, Instant now) {
    return em.createQuery("select count(r) from Reservation r where r.removedAt is null and r.court.id = :court and r.endTime > :now", Long.class)
        .setParameter("court", courtId).setParameter("now", now).getSingleResult() > 0;
  }

  public List<Reservation> byCourt(long courtId) {
    return em.createQuery("select r from Reservation r where r.removedAt is null and r.court.id = :court order by r.createdAt desc, r.id desc", Reservation.class)
        .setParameter("court", courtId).getResultList();
  }

  public List<Reservation> byPhone(String phone, boolean futureOnly, Instant now) {
    return em.createQuery("select r from Reservation r where r.removedAt is null and (:phone is null or r.customer.phoneNumber = :phone) and (:futureOnly = false or r.startTime > :now) order by r.createdAt desc, r.id desc", Reservation.class)
        .setParameter("phone", phone).setParameter("futureOnly", futureOnly).setParameter("now", now).getResultList();
  }

  public void persist(Reservation reservation) {
    em.persist(reservation);
  }

  public void flush() {
    em.flush();
  }
}
