package com.laboschqpa.filehost.config.filter;

import com.laboschqpa.filehost.api.dto.StoredFileDto;
import lombok.Getter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

@Getter
public class StoredFileRequestWrapper extends HttpServletRequestWrapper {
    private StoredFileDto storedFileDto;

    public StoredFileRequestWrapper(HttpServletRequest request, StoredFileDto storedFileDto) {
        super(request);
        this.storedFileDto = storedFileDto;
    }
}