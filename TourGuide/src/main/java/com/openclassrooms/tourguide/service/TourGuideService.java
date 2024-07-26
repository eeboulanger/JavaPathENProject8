package com.openclassrooms.tourguide.service;

import com.openclassrooms.tourguide.model.AttractionDistanceDTO;
import com.openclassrooms.tourguide.tracker.Tracker;
import com.openclassrooms.tourguide.model.User;
import com.openclassrooms.tourguide.model.UserReward;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;

import tripPricer.Provider;
import tripPricer.TripPricer;

/**
 * Fetches user information and sets up users for testing
 */
@Service
public class TourGuideService {
    private final Logger logger = LoggerFactory.getLogger(TourGuideService.class);
    private final TripPricer tripPricer = new TripPricer();
    public final Tracker tracker;
    private final RewardsService rewardsService;
    private final GpsUtilService gpsUtilService;
    private final RewardCentralService rewardCentralService;
    private final UserService userService;
    private final List<Attraction> attractions;
    boolean testMode = true;

    public TourGuideService(GpsUtilService gpsUtilService, RewardsService rewardsService, UserService userService, RewardCentralService rewardCentralService) {
        this.gpsUtilService = gpsUtilService;
        this.rewardsService = rewardsService;
        this.userService = userService;
        this.rewardCentralService = rewardCentralService;

        attractions = gpsUtilService.getAttractions().join();

        Locale.setDefault(Locale.US);

        if (testMode) {
            logger.info("TestMode enabled");
            logger.debug("Initializing users");
            userService.initializeInternalUsers();
            logger.debug("Finished initializing users");
        }
        tracker = new Tracker(userService, this); //Track users automatically
        addShutDownHook();
    }

    public List<UserReward> getUserRewards(User user) {
        return user.getUserRewards();
    }

    /**
     * Returns the last visited location or track current location if there's no last location
     */
    public VisitedLocation getUserLocation(User user) {
        return (!user.getVisitedLocations().isEmpty()) ? user.getLastVisitedLocation()
                : trackUserLocation(user).join();
    }

    /**
     * Fetches the users current location
     */
    public CompletableFuture<VisitedLocation> trackUserLocation(User user) {
        return gpsUtilService.getUserLocation(user.getUserId())
                .thenApply(location -> {
                    user.addToVisitedLocations(location);
                    rewardsService.calculateRewards(user, null);
                    return location;
                });
    }

    /**
     * Fetch trip deals based on users current reward points and preferences
     */
    public List<Provider> getTripDeals(User user) {
        //Sum all user reward points
        int cumulativeRewardPoints = user.getUserRewards().stream()
                .mapToInt(UserReward::getRewardPoints).sum();

        //Get list of providers(trip deals) based on reward points preferences and user
        List<Provider> providers = tripPricer.getPrice(
                userService.getTripPricerApiKey(),
                user.getUserId(),
                user.getUserPreferences().getNumberOfAdults(),
                user.getUserPreferences().getNumberOfChildren(),
                user.getUserPreferences().getTripDuration(),
                cumulativeRewardPoints);

        //Update users trip deals
        user.setTripDeals(providers);

        //Return list of providers(trip deals)
        return providers;
    }

    /**
     * Finds the 5 closest attractions to the users current location.
     *
     * @param visitedLocation is the users current location
     * @param user
     * @return attraction name, location, distance to user, and reward points
     */
    public CompletableFuture<List<AttractionDistanceDTO>> getNearByAttractions(VisitedLocation visitedLocation, User user) {
        Location userLocation = new Location(visitedLocation.location.latitude, visitedLocation.location.longitude);

        //Calculate distance for each attraction and store in map
        HashMap<Attraction, Double> attractionDistance = new HashMap<>();
        attractions.forEach(attraction ->
                attractionDistance.put(attraction, rewardsService.getDistance(userLocation,
                        new Location(attraction.latitude, attraction.longitude)))
        );

        return CompletableFuture.supplyAsync(() -> attractionDistance.entrySet().stream()
                .sorted(Map.Entry.comparingByValue()) //Sort by distance
                .limit(5) //Limit to the five closest
                .map(Map.Entry::getKey)
                .map(attraction -> new AttractionDistanceDTO( //Convert to DTO
                        attraction.attractionName,
                        new Location(attraction.latitude, attraction.longitude),
                        userLocation,
                        attractionDistance.get(attraction), //Distance stored in map
                        rewardCentralService.getAttractionRewardPoints(attraction.attractionId, user.getUserId())
                                .join()
                ))
                .toList());
    }


private void addShutDownHook() {
    Runtime.getRuntime().addShutdownHook(new Thread() {
        public void run() {
            tracker.stopTracking();
        }
    });
}
}
