package org.gso.brinder.match.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GeoCoordinates {
    private String type = "Point";
    private double[] coordinates; // longitude, latitude

    public GeoCoordinates(double latitude, double longitude) {
        this.coordinates = new double[]{longitude, latitude};
    }
}