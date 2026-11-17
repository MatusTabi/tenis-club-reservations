package com.example.tenisclubreservations.court;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CourtDao {
  private final EntityManager em;

  public Optional<Court> find(long id) {
    return em.createQuery(
            "select c from Court c join fetch c.surface where c.id = :id and c.removedAt is null",
            Court.class)
        .setParameter("id", id)
        .getResultStream()
        .findFirst();
  }

  public List<Court> list() {
    return em.createQuery(
            "select c from Court c join fetch c.surface where c.removedAt is null order by"
                + " c.courtNumber",
            Court.class)
        .getResultList();
  }

  public boolean exists(int number, Long except) {
    return em.createQuery(
                "select count(c) from Court c where c.activeNumber = :number and (:id is null or"
                    + " c.id <> :id)",
                Long.class)
            .setParameter("number", number)
            .setParameter("id", except)
            .getSingleResult()
        > 0;
  }

  public boolean assigned(long surfaceId) {
    return em.createQuery(
                "select count(c) from Court c where c.surface.id = :id and c.removedAt is null",
                Long.class)
            .setParameter("id", surfaceId)
            .getSingleResult()
        > 0;
  }

  public void persist(Court court) {
    em.persist(court);
  }
}
