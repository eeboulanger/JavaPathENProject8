package com.openclassrooms.tourguide;

import java.util.*;

import com.openclassrooms.tourguide.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.model.User;
import com.openclassrooms.tourguide.model.UserReward;

import static org.junit.jupiter.api.Assertions.*;

public class TestRewardsService {

    private GpsUtil gpsUtil;
    private RewardsService rewardsService;
    private GpsUtilService gpsUtilService;
    private RewardCentralService rewardCentralService;
    private UserService userService;

    @BeforeEach
    public void setUp() {
        gpsUtil = new GpsUtil();
        gpsUtilService = new GpsUtilService(gpsUtil);
        RewardCentralService rewardCentralService = new RewardCentralService(new RewardCentral());

        rewardsService = new RewardsService(gpsUtilService, rewardCentralService);
        userService = new UserService();
    }

    @Test
    public void userGetRewards() {

        //Given one user with one visited location close to an attraction
        InternalTestHelper.setInternalUserNumber(0);
        TourGuideService tourGuideService = new TourGuideService(gpsUtilService, rewardsService, userService, rewardCentralService);
        User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
        Attraction attraction = gpsUtilService.getAttractions().join().get(0);
        user.addToVisitedLocations(new VisitedLocation(user.getUserId(), attraction, new Date()));

        //When tracking user location
        tourGuideService.trackUserLocation(user).join();

        //Then reward for the attraction should be added to the user
        List<UserReward> userRewards = user.getUserRewards();
        tourGuideService.tracker.stopTracking();

        assertEquals(1, userRewards.size());
    }

    @Test
    public void isWithinAttractionProximity() {
        Attraction attraction = gpsUtilService.getAttractions().join().get(0);
        assertTrue(rewardsService.isWithinAttractionProximity(attraction, attraction));
    }

    @Test
    public void nearAllAttractions() {
        //Given all available attractions are within proximity buffer
        rewardsService.setProximityBuffer(Integer.MAX_VALUE);

        InternalTestHelper.setInternalUserNumber(10);
        TourGuideService tourGuideService = new TourGuideService(gpsUtilService, rewardsService, userService, rewardCentralService);
        User user = userService.getAllUsers().get(0);

        //When calculating user rewards
        rewardsService.calculateRewards(user, null);
        List<UserReward> userRewards = tourGuideService.getUserRewards(user);
        tourGuideService.tracker.stopTracking();

        //Then all rewards should be added to the user and
        // reward point calculation for each attraction should be run asynchronously
        assertEquals(gpsUtil.getAttractions().size(), userRewards.size());
    }
}
