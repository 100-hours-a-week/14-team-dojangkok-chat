package com.dojangkok.chat.repository;

import com.dojangkok.chat.domain.FileAsset;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface FileAssetRepository extends MongoRepository<FileAsset, String> {

    Optional<FileAsset> findByFileKey(String fileKey);

    List<FileAsset> findAllByFileKeyIn(List<String> fileKeys);
}
