package com.openclassrooms.tourguide.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

@Service
public class RewardsService {
    private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;

    // proximity in miles
    private int defaultProximityBuffer = 10;
    private int proximityBuffer = defaultProximityBuffer;
    private int attractionProximityRange = 200;
    private final GpsUtil gpsUtil;
    private final RewardCentral rewardsCentral;
    private final List<Attraction> attractions;

    public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral) {
        this.gpsUtil = gpsUtil;
        this.rewardsCentral = rewardCentral;
        attractions = gpsUtil.getAttractions();
    }


    public void setProximityBuffer(int proximityBuffer) {
        this.proximityBuffer = proximityBuffer;
    }

    public void setDefaultProximityBuffer() {
        proximityBuffer = defaultProximityBuffer;
    }

    public void calculateRewards(User user) {
        List<VisitedLocation> userLocations = new CopyOnWriteArrayList<>(user.getVisitedLocations());
        List<UserReward> rewards = new CopyOnWriteArrayList<>(user.getUserRewards());
        Set<Attraction> getPointsList = new HashSet<>();

        attractions.parallelStream()
                .filter(attraction -> rewards.parallelStream().noneMatch(reward ->
                        reward.attraction.attractionName.equals(attraction.attractionName)))//Filter out the attractions that are already registered as user rewards
                .forEach(attraction ->
                        userLocations.parallelStream()
                                .filter(location -> nearAttraction(location, attraction))//Check if location is nearby
                                .findFirst()
                                .ifPresent(location -> {
                                    user.addUserReward(new UserReward(location, attraction));
                                    getPointsList.add(attraction);
                                })
                );

        //Update with reward points async
        calculatePoints(getPointsList, user);
    }

    /**
     * Takes a list of attractions and updates the reward points on each attraction for the given user
     *
     * @param getPointsList is the list of attractions for which to calculate reward points
     * @param user          whose user rewards will be updated
     */
    public CompletableFuture<Void> calculatePoints(Set<Attraction> getPointsList, User user) {
        return CompletableFuture.runAsync(() -> getPointsList.forEach(attraction ->
                user.getUserRewards().stream().filter(reward ->
                                reward.attraction.attractionName.equals(attraction.attractionName))
                        .findFirst()
                        .ifPresent(reward -> reward.setRewardPoints(getRewardPoints(attraction, user)))
        ));
    }

    public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
        return getDistance(attraction, location) > attractionProximityRange ? false : true;
    }

    private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
        return getDistance(attraction, visitedLocation.location) > proximityBuffer ? false : true;
    }

    private int getRewardPoints(Attraction attraction, User user) {
        return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
    }

    public double getDistance(Location loc1, Location loc2) {
        double lat1 = Math.toRadians(loc1.latitude);
        double lon1 = Math.toRadians(loc1.longitude);
        double lat2 = Math.toRadians(loc2.latitude);
        double lon2 = Math.toRadians(loc2.longitude);

        double angle = Math.acos(Math.sin(lat1) * Math.sin(lat2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

        double nauticalMiles = 60 * Math.toDegrees(angle);
        double statuteMiles = STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
        return statuteMiles;
    }

}
