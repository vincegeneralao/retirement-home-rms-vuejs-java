package com.rms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A resident / client of the retirement home that support shifts are scheduled for.
 */
@Entity
@Table(name = "clients")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@AllArgsConstructor
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", length = 120, nullable = false)
    private String fullName;

    @Column(name = "room_number", length = 30, nullable = false)
    private String roomNumber;

    @Column(name = "address", length = 255, nullable = false)
    private String address;

    @Column(name = "active", nullable = false)
    private boolean active;

    /** "roomNumber · address" — used as the display location across views and reports. */
    public String location() {
        return roomNumber + " · " + address;
    }
}
