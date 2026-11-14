package com.rms.dto;

/** A client row as returned to the admin client directory. */
public record ClientView(
        Long id,
        String fullName,
        String roomNumber,
        String address,
        String location,
        boolean active
) {
}
