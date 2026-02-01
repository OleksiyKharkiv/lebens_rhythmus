package com.be.web.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileRequestDTO {
    private String fileName;
    private String filePath;
    private String fileType;
    private Long fileSize;
    private Long workshopId;
}
