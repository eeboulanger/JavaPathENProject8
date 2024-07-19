package com.openclassrooms.tourguide.service;

import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.model.AttractionDistanceDTO;
import com.openclassrooms.tourguide.tracker.Tracker;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;

import rewardCentral.RewardCentral;
import tripPricer.Provider;
import tripPricer.TripPricer;

/**
 * Fetches user information and sets up users for testing
 */
@Service
public class TourGuideService {
    private final Logger logger = LoggerFactory.getLogger(TourGuideService.class);
    private final GpsUtil gpsUtil;
    private final RewardsService rewardsService;
    private final TripPricer tripPricer = new TripPricer();
    public final Tracker tracker;
    private final ThreadService threadService;
    boolean testMode = true;

    public TourGuideService(GpsUtil gpsUtil, RewardsService rewardsService) {
        this.gpsUtil = gpsUtil;
        this.rewardsService = rewardsService;

        threadService = new ThreadService();

        Locale.setDefault(Locale.US);

        if (testMode) {
            logger.info("TestMode enabled");
            logger.debug("Initializing users");
            initializeInternalUsers();
            logger.debug("Finished initializing users");
        }
        tracker = new Tracker(this);
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
     * Fetch trip deals based on users current reward points and preferences
     */
    public List<Provider> getTripDeals(User user) {
        //Sum all user reward points
        int cumulativeRewardPoints = user.getUserRewards().stream()
                .mapToInt(UserReward::getRewardPoints).sum();

        //Get list of providers(trip deals) based on reward points preferences and user
        List<Provider> providers = tripPricer.getPrice(
                tripPricerApiKey,
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
     * Fetches the users current location using gps util
     */
    public CompletableFuture<VisitedLocation> trackUserLocation(User user) {
        return CompletableFuture.supplyAsync(() ->
                        gpsUtil.getUserLocation(user.getUserId()), threadService.getThread())
                .thenApply(location -> {
                    user.addToVisitedLocations(location);
                    rewardsService.calculateRewards(user);
                    return location;
                })
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        logger.error("Failed to fetch user location: " + exception.getMessage());
                    }
                    threadService.releaseThread();
                });
    }

    /**
     * Finds the 5 closest attractions to the users current location.
     * @param visitedLocation is the users current location
     * @param user
     * @return attraction name, location, distance to user, and reward points
     */
    public List<AttractionDistanceDTO> getNearByAttractions(VisitedLocation visitedLocation, User user) {
        Location userLocation = new Location(visitedLocation.location.latitude, visitedLocation.location.longitude);

        //Calculate distance for each attraction and store in map
        HashMap<Attraction, Double> attractionDistance = new HashMap<>();
        gpsUtil.getAttractions().forEach(attraction ->
                attractionDistance.put(attraction, rewardsService.getDistance(userLocation,
                new Location(attraction.latitude, attraction.longitude)))
        );

        RewardCentral central = new RewardCentral(); //For calculating reward points

        //Sort out the 5 closest and convert to DTO
        return attractionDistance.entrySet().stream()
                .sorted(Map.Entry.comparingByValue()) //Sort by distance
                .limit(5) //Limit to the five closest
                .map(Map.Entry::getKey)
                .map(attraction -> new AttractionDistanceDTO( //Convert to DTO
                        attraction.attractionName,
                        new Location(attraction.latitude, attraction.longitude),
                        userLocation,
                        attractionDistance.get(attraction), //Distance stored in map
                        central.getAttractionRewardPoints(attraction.attractionId, user.getUserId())
                ))
                .toList();
    }


    private void addShutDownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                tracker.stopTracking();
            }
        });
    }


    /**
     * For testing
     */
    public User getUser(String userName) {
        return internalUserMap.get(userName);
    }

    public List<User> getAllUsers() {
        return internalUserMap.values().stream().collect(Collectors.toList());
    }

    public void addUser(User user) {
        if (!internalUserMap.containsKey(user.getUserName())) {
            internalUserMap.put(user.getUserName(), user);
        }
    }

    /**********************************************************************************
     *
     * Methods Below: For Internal Testing
     *
     **********************************************************************************/
    private static final String tripPricerApiKey = "test-server-api-key";
    // Database connection will be used for external users, but for testing purposes
// internal users are provided and stored in memory
    private final Map<String, User> internalUserMap = new HashMap<>();

    private void initializeInternalUsers() {
        IntStream.range(0, InternalTestHelper.getInternalUserNumber()).forEach(i -> {
            String userName = "internalUser" + i;
            String phone = "000";
            String email = userName + "@tourGuide.com";
            User user = new User(UUID.randomUUID(), userName, phone, email);
            generateUserLocationHistory(user);

            internalUserMap.put(userName, user);
        });
        logger.debug("Created " + InternalTestHelper.getInternalUserNumber() + " internal test users.");
    }

    private void generateUserLocationHistory(User user) {
        IntStream.range(0, 3).forEach(i -> {
            user.addToVisitedLocations(new VisitedLocation(user.getUserId(),
                    new Location(generateRandomLatitude(), generateRandomLongitude()), getRandomTime()));
        });
    }

    private double generateRandomLongitude() {
        double leftLimit = -180;
        double rightLimit = 180;
        return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
    }

    private double generateRandomLatitude() {
        double leftLimit = -85.05112878;
        double rightLimit = 85.05112878;
        return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
    }

    private Date getRandomTime() {
        LocalDateTime localDateTime = LocalDateTime.now().minusDays(new Random().nextInt(30));
        return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
    }

}
