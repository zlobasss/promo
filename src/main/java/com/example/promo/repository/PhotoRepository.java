package com.example.promo.repository;

import com.example.promo.entity.PhotoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PhotoRepository extends JpaRepository<PhotoEntity, Long> {
    PhotoEntity findByPhotoId(String photoId);
}
