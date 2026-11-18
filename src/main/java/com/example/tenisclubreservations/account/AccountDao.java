package com.example.tenisclubreservations.account;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AccountDao {
  private final EntityManager em;

  public Optional<UserAccount> byId(long id) {
    UserAccount account = em.find(UserAccount.class, id);
    return account == null || account.isDeleted() ? Optional.empty() : Optional.of(account);
  }

  public Optional<UserAccount> byUsername(String username) {
    return em.createQuery("select a from UserAccount a where a.activeUsername = :username", UserAccount.class)
        .setParameter("username", username).getResultStream().findFirst();
  }

  List<UserAccount> list() {
    return em.createQuery("select a from UserAccount a where a.removedAt is null order by a.id", UserAccount.class).getResultList();
  }

  Role role(RoleType type) {
    return em.createQuery("select r from Role r where r.role = :role", Role.class)
        .setParameter("role", type).getSingleResult();
  }

  long adminCount() {
    return em.createQuery("select count(a) from UserAccount a where a.removedAt is null and a.role.role = :role", Long.class)
        .setParameter("role", RoleType.ADMIN).getSingleResult();
  }

  void save(UserAccount account) { em.persist(account); }
}
