package com.laboschqpa.filehost.model.streamtracking;

import com.laboschqpa.filehost.entity.IndexedFileEntity;
import com.laboschqpa.filehost.enums.UploadType;
import com.laboschqpa.filehost.model.inputstream.QuotaAllocatingInputStream;
import com.laboschqpa.filehost.service.IndexedFileQuotaAllocator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@RequiredArgsConstructor
@Service
public class QuotaAllocatingInputStreamFactory {
    private final IndexedFileQuotaAllocator indexedFileQuotaAllocator;

    public QuotaAllocatingInputStream from(InputStream inputStream, IndexedFileEntity indexedFileEntity, Long approximateFileSize) {
        final boolean shouldEnforceUserAndTeamQuota = indexedFileEntity.getUploadType() != UploadType.IMAGE_VARIANT;

        return new QuotaAllocatingInputStream(inputStream, indexedFileEntity, approximateFileSize, indexedFileQuotaAllocator, shouldEnforceUserAndTeamQuota);
    }
}
