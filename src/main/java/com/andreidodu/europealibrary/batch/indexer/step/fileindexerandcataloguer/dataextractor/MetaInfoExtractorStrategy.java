package com.andreidodu.europealibrary.batch.indexer.step.fileindexerandcataloguer.dataextractor;

import com.andreidodu.europealibrary.model.FileMetaInfo;

import java.util.Optional;

public interface MetaInfoExtractorStrategy {
    boolean accept(String filename);

    Optional<FileMetaInfo> extract(String filename);
}