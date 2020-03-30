package com.laboschqpa.filehost.api.service;

import com.laboschqpa.filehost.config.annotation.ExceptionWrappedFileServingClass;
import com.laboschqpa.filehost.config.filter.FileServingHttpServletRequest;
import com.laboschqpa.filehost.enums.IndexedFileStatus;
import com.laboschqpa.filehost.exceptions.InvalidHttpRequestException;
import com.laboschqpa.filehost.exceptions.fileserving.FileIsNotAvailableException;
import com.laboschqpa.filehost.exceptions.fileserving.FileSavingException;
import com.laboschqpa.filehost.model.SaveableFile;
import com.laboschqpa.filehost.model.SaveableFileFactory;
import com.laboschqpa.filehost.model.ServiceableFile;
import com.laboschqpa.filehost.model.ServiceableFileFactory;
import com.laboschqpa.filehost.repo.IndexedFileEntityRepository;
import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.http.fileupload.FileItemIterator;
import org.apache.tomcat.util.http.fileupload.FileItemStream;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.apache.tomcat.util.http.fileupload.util.Streams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.*;

@RequiredArgsConstructor
@Service
@ExceptionWrappedFileServingClass
public class FileServingService {
    private static final Logger logger = LoggerFactory.getLogger(FileServingService.class);

    private final ServletFileUpload servletFileUpload = new ServletFileUpload();

    private final ServiceableFileFactory serviceableFileFactory;
    private final SaveableFileFactory saveableFileFactory;
    private final IndexedFileEntityRepository indexedFileEntityRepository;

    public ResponseEntity<Resource> downloadFile(FileServingHttpServletRequest request) {
        ServiceableFile serviceableFile = serviceableFileFactory.from(request.getIndexedFileServingRequestDto());

        if (serviceableFile.isAvailable()) {
            String ifNoneMatchHeaderValue = request.getHeader(HttpHeaders.IF_NONE_MATCH);
            if (ifNoneMatchHeaderValue != null
                    && !ifNoneMatchHeaderValue.isBlank()
                    && ifNoneMatchHeaderValue.equals(serviceableFile.getETag())) {
                logger.trace("ETag matches. Returning 304 - Not modified");
                return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
            }

            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            httpHeaders.setContentLength(serviceableFile.getSize());
            httpHeaders.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + serviceableFile.getOriginalFileName() + "\"");

            return new ResponseEntity<>(new InputStreamResource(serviceableFile.getStream()), httpHeaders, HttpStatus.OK);
        } else {
            throw new FileIsNotAvailableException("The requested file is not available for download. File status: " + serviceableFile.getStatus());
        }
    }

    public void uploadFile(HttpServletRequest request) throws IOException, FileUploadException {
        if (!StringUtils.startsWithIgnoreCase(request.getContentType(), "multipart/")) {
            throw new InvalidHttpRequestException("The request is not a multipart request.");
        }

        FileItemIterator iterator = servletFileUpload.getItemIterator(request);

        while (iterator.hasNext()) {
            FileItemStream item = iterator.next();
            String fieldName = item.getFieldName();

            try (InputStream inputStream = item.openStream()) {
                if (item.isFormField()) {
                    logger.trace("Multipart form field {} with value {} detected.", fieldName, Streams.asString(inputStream));
                } else {
                    String originalFileName = item.getName();
                    logger.trace("Multipart file field {} with fileName {} detected.", fieldName, originalFileName);
                    saveNewFile(inputStream, originalFileName);
                }
            }
        }
    }

    private void saveNewFile(InputStream fileUploadingInputStream, String originalFileName) {
        SaveableFile newSaveableFile = null;
        try {
            newSaveableFile = saveableFileFactory.fromFileUploadStream(originalFileName);
            newSaveableFile.saveFromStream(fileUploadingInputStream);
        } catch (Exception e) {
            if (newSaveableFile != null) {
                newSaveableFile.getIndexedFileEntity().setStatus(IndexedFileStatus.FAILED);
                indexedFileEntityRepository.save(newSaveableFile.getIndexedFileEntity());
            }
            throw new FileSavingException("Exception while handling saving of the uploaded file!", e);
        }
    }


}
