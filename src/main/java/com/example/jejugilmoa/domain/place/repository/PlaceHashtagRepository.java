package com.example.jejugilmoa.domain.place.repository;

import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.place.entity.PlaceHashtag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PlaceHashtagRepository extends JpaRepository<PlaceHashtag, Long> {
    Optional<PlaceHashtag> findByPlace(Place place);
    List<PlaceHashtag> findByPlace_IdIn(Collection<Long> placeIds);
}
