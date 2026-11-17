package com.example.tenisclubreservations.customer;

import jakarta.persistence.EntityManager;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CustomerDao {
  private final EntityManager em;

  public Optional<Customer> findByPhone(String phone) {
    return em.createQuery("select c from Customer c where c.phoneNumber = :phone", Customer.class)
        .setParameter("phone", phone).getResultStream().findFirst();
  }

  public void persist(Customer customer) {
    em.persist(customer);
  }
}
