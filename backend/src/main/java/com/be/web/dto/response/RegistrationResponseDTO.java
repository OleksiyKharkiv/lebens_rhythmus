package com.be.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Registration no longer logs the user in immediately (LR: email
 * verification is now mandatory before login) — this replaces the old
 * UserLoginResponseDTO (token+profile) return, since there is no session
 * to hand back yet.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationResponseDTO {
    private String message;
    private String email;
}
