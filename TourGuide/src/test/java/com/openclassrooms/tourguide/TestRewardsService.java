package com.openclassrooms.tourguide;

import java.util.*;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.service.RewardsService;
import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

import static org.junit.jupiter.api.Assertions.*;

public class TestRewardsService {

    @Test
    @Disabled
    public void userGetRewards() {
        GpsUtil gpsUtil = new GpsUtil();
        RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());

        InternalTestHelper.setInternalUserNumber(0);
        TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);

        User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
        Attraction attraction = gpsUtil.getAttractions().get(0);
        user.addToVisitedLocations(new VisitedLocation(user.getUserId(), attraction, new Date()));
        tourGuideService.trackUserLocation(user);
        List<UserReward> userRewards = user.getUserRewards();
        tourGuideService.tracker.stopTracking();
        assertTrue(userRewards.size() == 1);
    }

    @Test
    public void isWithinAttractionProximity() {
        GpsUtil gpsUtil = new GpsUtil();
        RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
        Attraction attraction = gpsUtil.getAttractions().get(0);
        assertTrue(rewardsService.isWithinAttractionProximity(attraction, attraction));
    }

    @Test
    public void nearAllAttractions() {
        //Given all available attractions are within proximity buffer
        GpsUtil gpsUtil = new GpsUtil();
        RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
        rewardsService.setProximityBuffer(Integer.MAX_VALUE);

        InternalTestHelper.setInternalUserNumber(1);
        TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);
        User user = tourGuideService.getAllUsers().get(0);

        //When calculating user rewards
        rewardsService.calculateRewards(user);
        List<UserReward> userRewards = tourGuideService.getUserRewards(user);
        tourGuideService.tracker.stopTracking();

        //Then all rewards should be added to the user
        assertEquals(gpsUtil.getAttractions().size(), userRewards.size());

        //Then reward point calculation for each attraction should be run asynchronously
        Set<Attraction> attractions = new HashSet<>();
        user.getUserRewards().forEach(reward ->
                attractions.add(reward.attraction));

        rewardsService.calculatePoints(attractions, user).join();

        user.getUserRewards().forEach(reward -> {
            assertTrue(reward.getRewardPoints() > 0);
            System.out.println(reward.attraction.attractionName + " " + reward.getRewardPoints());
        });

    }
}
