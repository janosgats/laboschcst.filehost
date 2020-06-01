

package com.laboschqpa.filehost.api.service;

import com.laboschqpa.filehost.config.annotation.ExceptionWrappedFileServingClass;
import com.laboschqpa.filehost.enums.IndexedFileStatus;
import com.laboschqpa.filehost.exceptions.fileserving.FileServingException;
import com.laboschqpa.filehost.model.file.DeletableFile;
import com.laboschqpa.filehost.model.file.factory.DeletableFileFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Log4j2
@RequiredArgsConstructor
@Service
@ExceptionWrappedFileServingClass
public class FileDeleterService {
    private final DeletableFileFactory deletableFileFactory;

    public void deleteFile(Long fileIdToDelete) {
        DeletableFile deletableFile = deletableFileFactory.fromIndexedFileId(fileIdToDelete);
        log.debug("Deleting file: {}", fileIdToDelete);

        if (deletableFile.getStatus() == IndexedFileStatus.DELETED) {
            throw new FileServingException("File " + fileIdToDelete + " is already deleted!");
        }
        deletableFile.delete();
    }
}
