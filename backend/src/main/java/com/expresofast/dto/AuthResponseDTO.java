package com.expresofast.dto;

import java.util.List;

public record AuthResponseDTO(
        String token,
        String username,
        List<String> roles,
        long expirationTime) {
}