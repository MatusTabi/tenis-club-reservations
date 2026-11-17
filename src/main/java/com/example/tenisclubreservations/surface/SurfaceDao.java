package com.example.tenisclubreservations.surface;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SurfaceDao {
  private final EntityManager em;

  public Optional<Surface> find(long id) {
    return em.createQuery(
            "select s from Surface s where s.id = :id and s.removedAt is null", Surface.class)
        .setParameter("id", id)
        .getResultStream()
        .findFirst();
  }

  public List<Surface> list() {
    return em.createQuery(
            "select s from Surface s where s.removedAt is null order by s.activeName",
            Surface.class)
        .getResultList();
  }

  public boolean exists(String name, Long except) {
    return em.createQuery(
                "select count(s) from Surface s where s.activeName = :name and (:id is null or s.id"
                    + " <> :id)",
                Long.class)
            .setParameter("name", name)
            .setParameter("id", except)
            .getSingleResult()
        > 0;
  }

  public void persist(Surface surface) {
    em.persist(surface);
  }
}
