package com.openclassrooms.tourguide.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import com.openclassrooms.tourguide.model.User;
import com.openclassrooms.tourguide.model.UserReward;

/**
 * Calculates and adds user rewards to a user
 */
@Service
public class RewardsService {
    private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;
    private final Logger logger = LoggerFactory.getLogger(RewardsService.class);

    // proximity in miles
    private int defaultProximityBuffer = 10;
    private int proximityBuffer = defaultProximityBuffer;
    private int attractionProximityRange = 200;
    private final RewardCentralService rewardCentralService;
    private final GpsUtilService gpsUtilService;
    private final List<Attraction> attractions;

    public RewardsService(GpsUtilService gpsUtilService, RewardCentralService rewardCentralService) {
        this.gpsUtilService = gpsUtilService;
        this.rewardCentralService = rewardCentralService;
        attractions = gpsUtilService.getAttractions().join(); //Fetch the list of attractions
    }

    /**
     * Adds user reward points for all new visited locations close to attractions
     * @CountDownLatch latch is for testing purpose and can be null
     */
    public void calculateRewards(User user, CountDownLatch latch) {
        List<VisitedLocation> userLocations = user.getVisitedLocations();
        List<UserReward> rewards = new CopyOnWriteArrayList<>(user.getUserRewards());
        Set<Attraction> attractionList = new HashSet<>();

        attractions.parallelStream() //Stream all available attractions
                .filter(attraction -> rewards.parallelStream()
                        .noneMatch(reward ->
                                reward.attraction.attractionName.equals(attraction.attractionName)))//Filter out the attractions that are not already registered as user rewards
                .forEach(attraction ->
                        userLocations.parallelStream()
                                .filter(location -> nearAttraction(location, attraction))//Check if location is within proximity buffert
                                .findFirst()
                                .ifPresent(location -> {
                                    user.addUserReward(new UserReward(location, attraction));//Add user reward
                                    attractionList.add(attraction); //Add to attraction list for async update
                                })
                );

        //Update all user rewards with reward points async
        calculateRewardPoints(attractionList, user, latch);
    }

    /**
     * Takes a list of attractions and updates the reward points on each attraction for the given user
     *
     * @param getPointsList is the list of attractions for which to calculate reward points
     * @param user          whose user rewards will be updated
     * @CountDownLatch latch is optional for testing purpose and can be null
     */
    public void calculateRewardPoints(Set<Attraction> getPointsList, User user, CountDownLatch latch) {
        CompletableFuture.runAsync(() -> {
                    getPointsList.forEach(attraction ->
                            user.getUserRewards().stream()
                                    .filter(reward ->
                                            reward.attraction.attractionName.equals(attraction.attractionName))//Find the user reward to update by attraction name
                                    .findFirst()
                                    .ifPresent(reward ->
                                            reward.setRewardPoints(getRewardPoints(attraction, user).join()))); //Calculate points for the user reward
                }
        ).whenComplete((result, exception) -> {
            if (latch != null) {
                latch.countDown();
            }
        });
    }

    /**
     * Fetches the reward points for an attraction
     */
    private CompletableFuture<Integer> getRewardPoints(Attraction attraction, User user) {
        return rewardCentralService.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
    }

    public void setProximityBuffer(int proximityBuffer) {
        this.proximityBuffer = proximityBuffer;
    }

    public void setDefaultProximityBuffer() {
        proximityBuffer = defaultProximityBuffer;
    }


    public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
        return getDistance(attraction, location) > attractionProximityRange ? false : true;
    }

    //Remove? The method is doing the same thing as the one above ^
    private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
        return getDistance(attraction, visitedLocation.location) > proximityBuffer ? false : true;
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
