package com.example.tenisclubreservations.common;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
@MappedSuperclass
public abstract class BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private LocalDateTime updatedAt;

  private LocalDateTime removedAt;

  @PrePersist
  void createTimestamps() {
    createdAt = updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  void updateTimestamp() {
    updatedAt = LocalDateTime.now();
  }

  public void softDelete() {
    removedAt = LocalDateTime.now();
  }

  public boolean isDeleted() {
    return removedAt != null;
  }
}
