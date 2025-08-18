package com.bucketlist.infra;

import com.bucketlist.domain.SuggestionFeedback;
import com.bucketlist.domain.Verdict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SuggestionFeedbackRepository extends JpaRepository<SuggestionFeedback, UUID> {
    
    @Query("""
        SELECT sf FROM SuggestionFeedback sf 
        JOIN Suggestion s ON sf.suggestionId = s.id
        WHERE sf.profileId = :profileId AND sf.verdict = :verdict
        ORDER BY sf.createdAt DESC
        """)
    List<SuggestionFeedback> findByProfileIdAndVerdict(@Param("profileId") UUID profileId, @Param("verdict") Verdict verdict);
    
    boolean existsBySuggestionIdAndProfileId(UUID suggestionId, UUID profileId);
}