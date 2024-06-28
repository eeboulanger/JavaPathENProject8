package com.openclassrooms.tourguide.model;

import gpsUtil.location.Location;

public class AttractionDistanceDTO {

    private String attractionName;
    private Location attractionLocation;
    private Location userLocation;
    private Double distance;
    private int rewardPoints;

    public AttractionDistanceDTO(String attractionName, Location attractionLocation, Location userLocation, Double distance, int rewardPoints) {
        this.attractionName = attractionName;
        this.attractionLocation = attractionLocation;
        this.userLocation = userLocation;
        this.distance = distance;
        this.rewardPoints = rewardPoints;
    }

    public String getAttractionName() {
        return attractionName;
    }

    public void setAttractionName(String attractionName) {
        this.attractionName = attractionName;
    }

    public Location getAttractionLocation() {
        return attractionLocation;
    }

    public void setAttractionLocation(Location attractionLocation) {
        this.attractionLocation = attractionLocation;
    }

    public Location getUserLocation() {
        return userLocation;
    }

    public void setUserLocation(Location userLocation) {
        this.userLocation = userLocation;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }

    public int getRewardPoints() {
        return rewardPoints;
    }

    public void setRewardPoints(int rewardPoints) {
        this.rewardPoints = rewardPoints;
    }

    @Override
    public String toString() {
        return "AttractionDistanceDTO{" +
                "attractionName='" + attractionName + '\'' +
                ", attractionLocation=" + attractionLocation +
                ", userLocation=" + userLocation +
                ", distance=" + distance +
                ", rewardPoints=" + rewardPoints +
                '}';
    }
}
