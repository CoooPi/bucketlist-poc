package com.bucketlist.infra;

import com.bucketlist.domain.Suggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SuggestionRepository extends JpaRepository<Suggestion, UUID> {
    
    @Query("""
        SELECT s FROM Suggestion s 
        WHERE s.profileId = :profileId 
        AND s.id NOT IN (
            SELECT sf.suggestionId FROM SuggestionFeedback sf 
            WHERE sf.profileId = :profileId
        )
        ORDER BY s.createdAt ASC
        """)
    List<Suggestion> findUnratedByProfileId(@Param("profileId") UUID profileId);
    
    boolean existsByContentHash(String contentHash);
}