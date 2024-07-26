package com.openclassrooms.tourguide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import com.openclassrooms.tourguide.model.AttractionDistanceDTO;
import com.openclassrooms.tourguide.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gpsUtil.GpsUtil;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.user.User;
import tripPricer.Provider;

public class TestTourGuideService {

    private TourGuideService tourGuideService;
    private UserService userService;

    @BeforeEach
    public void setUp() {
        userService = new UserService();
        GpsUtilService gpsUtilService = new GpsUtilService(new GpsUtil());
        RewardCentralService rewardCentralService = new RewardCentralService(new RewardCentral());

        RewardsService rewardsService = new RewardsService(gpsUtilService, rewardCentralService);
        InternalTestHelper.setInternalUserNumber(0);
        tourGuideService = new TourGuideService(gpsUtilService, rewardsService, userService, rewardCentralService);
    }

    @Test
    public void getUserLocation() {
        //Given one user
        User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");

        //When tracking user location
        VisitedLocation visitedLocation = tourGuideService.trackUserLocation(user).join();
        tourGuideService.tracker.stopTracking();

        //Then visited location should be added to user
        assertEquals(visitedLocation.userId, user.getUserId());
    }

    @Test
    public void addUser() {
        User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
        User user2 = new User(UUID.randomUUID(), "jon2", "000", "jon2@tourGuide.com");

        userService.addUser(user);
        userService.addUser(user2);

        User retrivedUser = userService.getUser(user.getUserName());
        User retrivedUser2 = userService.getUser(user2.getUserName());

        tourGuideService.tracker.stopTracking();

        assertEquals(user, retrivedUser);
        assertEquals(user2, retrivedUser2);
    }

    @Test
    public void getAllUsers() {
        User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
        User user2 = new User(UUID.randomUUID(), "jon2", "000", "jon2@tourGuide.com");

        userService.addUser(user);
        userService.addUser(user2);

        List<User> allUsers = userService.getAllUsers();

        tourGuideService.tracker.stopTracking();

        assertTrue(allUsers.contains(user));
        assertTrue(allUsers.contains(user2));
    }

    @Test
    public void trackUser() {
        //Given one user
        User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");

        //When tracking user location
        VisitedLocation visitedLocation = tourGuideService.trackUserLocation(user).join();
        tourGuideService.tracker.stopTracking();

        //Then user id should be added to the visited location
        assertEquals(user.getUserId(), visitedLocation.userId);
    }


    @Test
    public void getNearbyAttractions() {
        //Given one user
        User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
        VisitedLocation visitedLocation = tourGuideService.trackUserLocation(user).join();

        //When fetching the closest attractions
        List<AttractionDistanceDTO> attractions = tourGuideService
                .getNearByAttractions(visitedLocation, user).join();
        tourGuideService.tracker.stopTracking();

        //Then five attractions should be returned
        assertEquals(5, attractions.size());
    }

    //TODO implement code to return all 10 trip deals, currently returning only 5
    public void getTripDeals() {
        //Given one user
        User user = new User(
                UUID.randomUUID(),
                "jon",
                "000",
                "jon@tourGuide.com");

        //When get deals
        List<Provider> providers = tourGuideService.getTripDeals(user);
        tourGuideService.tracker.stopTracking();

        assertEquals(10, providers.size());
    }

}
