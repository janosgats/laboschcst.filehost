package com.laboschqpa.filehost.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IsUserAuthorizedToResourceResponseDto {
    @Builder.Default
    private boolean authenticated = true;
    @Builder.Default
    private boolean authorized = false;
}

