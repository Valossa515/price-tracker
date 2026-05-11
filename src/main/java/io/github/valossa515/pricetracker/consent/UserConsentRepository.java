package io.github.valossa515.pricetracker.consent;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserConsentRepository extends JpaRepository<UserConsent, UUID> {

    @Query("""
            SELECT c FROM UserConsent c
             WHERE c.userId = :userId
               AND c.documentType = :documentType
             ORDER BY c.acceptedAt DESC
             LIMIT 1
            """)
    Optional<UserConsent> findLatest(
            @Param("userId") String userId,
            @Param("documentType") ConsentDocumentType documentType);

    @Modifying
    @Query("DELETE FROM UserConsent c WHERE c.userId = :userId")
    int deleteByUserId(@Param("userId") String userId);
}
