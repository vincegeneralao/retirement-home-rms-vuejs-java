package com.rms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of POST /api/admin/clients. */
public record CreateClientRequest(
        @NotBlank(message = "fullName is required")
        @Size(max = 120, message = "fullName must be at most 120 characters")
        String fullName,

        @NotBlank(message = "roomNumber is required")
        @Size(max = 30, message = "roomNumber must be at most 30 characters")
        String roomNumber,

        @NotBlank(message = "address is required")
        @Size(max = 255, message = "address must be at most 255 characters")
        String address
) {
}
