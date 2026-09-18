package com.ems.dto;

import lombok.Data;

/**
 * Location captured by the browser's Geolocation API at the moment of
 * check-in or check-out. Both fields are optional — if the employee denies
 * location permission, or the browser doesn't support geolocation, we still
 * let them check in/out, just without coordinates.
 */
@Data
public class LocationRequest {
    private Double latitude;
    private Double longitude;
}