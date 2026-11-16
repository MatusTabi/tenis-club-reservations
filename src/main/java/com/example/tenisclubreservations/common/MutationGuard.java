package com.example.tenisclubreservations.common;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MutationGuard {
  private final EntityManager entityManager;

  public void lock() {
    entityManager
        .createNativeQuery("SELECT id FROM mutation_guard WHERE id = 1 FOR UPDATE")
        .getSingleResult();
  }
}
