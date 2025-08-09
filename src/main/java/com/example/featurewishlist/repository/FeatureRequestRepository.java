package com.example.featurewishlist.repository;

import com.example.featurewishlist.model.FeatureRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeatureRequestRepository extends JpaRepository<FeatureRequest, Long> {
}
