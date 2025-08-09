package com.example.featurewishlist.repository;

import com.example.featurewishlist.model.Vote;
import com.example.featurewishlist.model.FeatureRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VoteRepository extends JpaRepository<Vote, Long> {
    long countByFeature(FeatureRequest feature);

    Optional<Vote> findByFeatureIdAndVoterId(Long featureId, String voterId);

    List<Vote> findByVoterId(String voterId);
}
