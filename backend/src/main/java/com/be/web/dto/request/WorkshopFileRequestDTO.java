package com.be.web.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkshopFileRequestDTO {
    private Long workshopId;
    private String filename;
    private String fileUrl;
    private String contentType;
    private Long fileSize;
}
