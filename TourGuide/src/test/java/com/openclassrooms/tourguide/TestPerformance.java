package com.openclassrooms.tourguide;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.service.RewardsService;
import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.user.User;

import static org.junit.jupiter.api.Assertions.*;

public class TestPerformance {

    /*
     * A note on performance improvements:
     *
     * The number of users generated for the high volume tests can be easily
     * adjusted via this method:
     *
     * InternalTestHelper.setInternalUserNumber(100000);
     *
     *
     * These tests can be modified to suit new solutions, just as long as the
     * performance metrics at the end of the tests remains consistent.
     *
     * These are performance metrics that we are trying to hit:
     *
     * highVolumeTrackLocation: 100,000 users within 15 minutes:
     * assertTrue(TimeUnit.MINUTES.toSeconds(15) >=
     * TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
     *
     * highVolumeGetRewards: 100,000 users within 20 minutes:
     * assertTrue(TimeUnit.MINUTES.toSeconds(20) >=
     * TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
     */
    private GpsUtil gpsUtil;
    private RewardsService rewardsService;
    private TourGuideService tourGuideService;
    private final int internalUsers = 100000;
    private List<User> allUsers;

    @BeforeEach
    public void setUp() {
        gpsUtil = new GpsUtil();
        rewardsService = new RewardsService(gpsUtil, new RewardCentral());
        InternalTestHelper.setInternalUserNumber(internalUsers);
        tourGuideService = new TourGuideService(gpsUtil, rewardsService);
        tourGuideService.tracker.stopTracking(); //Shut down the tracker thread
        allUsers = tourGuideService.getAllUsers();
    }

    @Test
    public void highVolumeTrackLocation() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        //Given all users call track user location method at the same time
        List<CompletableFuture<VisitedLocation>> list = new ArrayList<>();
        allUsers.forEach(user -> list.add(tourGuideService.trackUserLocation(user)));

        //When fetching location for users
        list.forEach(completableFuture -> {
            try {
                completableFuture.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        stopWatch.stop();

        //Then locations should be fetched within 15 minutes
        System.out.println("highVolumeTrackLocation: Time Elapsed: "
                + TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()) + " seconds.");
        assertTrue(TimeUnit.MINUTES.toSeconds(15) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
    }

    // Users should be incremented up to 100,000, and test finishes within 20
    // minutes
    @Test
    public void highVolumeGetRewards() {
        Attraction attraction = gpsUtil.getAttractions().get(0);

        //Given all users has a new visited location nearby an attraction
        allUsers.forEach(u ->
                u.addToVisitedLocations(new VisitedLocation(u.getUserId(), attraction, new Date())));

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        //When calculating the user rewards
        allUsers.forEach(rewardsService::calculateRewards);

        //Then the attraction should be added to the user reward
        for (User user : allUsers) {
            assertFalse(user.getUserRewards().isEmpty());
        }

        stopWatch.stop();

        //Then the user reward should be added within 20 minutes
        System.out.println("highVolumeGetRewards: Time Elapsed: " + TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime())
                + " seconds.");
        assertTrue(TimeUnit.MINUTES.toSeconds(20) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
    }

}
