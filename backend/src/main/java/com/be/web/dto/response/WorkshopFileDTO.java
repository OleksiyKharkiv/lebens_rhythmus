package com.be.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkshopFileDTO {
    private Long id;
    private String filename;
    private String url;
    private String contentType;
    private Long fileSize;
}