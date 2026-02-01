package com.be.web.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LanguageRequestDTO {
    private String nameDe;
    private String nameEn;
    private String nameUa;
    private String code;
    private boolean active;
}
