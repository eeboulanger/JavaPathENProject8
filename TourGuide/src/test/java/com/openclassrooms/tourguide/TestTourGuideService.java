package com.openclassrooms.tourguide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import com.openclassrooms.tourguide.model.AttractionDistanceDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gpsUtil.GpsUtil;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.service.RewardsService;
import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.user.User;
import tripPricer.Provider;

public class TestTourGuideService {

    private TourGuideService tourGuideService;

    @BeforeEach
    public void setUp() {
        GpsUtil gpsUtil = new GpsUtil();
        RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
        InternalTestHelper.setInternalUserNumber(0);
        tourGuideService = new TourGuideService(gpsUtil, rewardsService);
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

        tourGuideService.addUser(user);
        tourGuideService.addUser(user2);

        User retrivedUser = tourGuideService.getUser(user.getUserName());
        User retrivedUser2 = tourGuideService.getUser(user2.getUserName());

        tourGuideService.tracker.stopTracking();

        assertEquals(user, retrivedUser);
        assertEquals(user2, retrivedUser2);
    }

    @Test
    public void getAllUsers() {
        User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
        User user2 = new User(UUID.randomUUID(), "jon2", "000", "jon2@tourGuide.com");

        tourGuideService.addUser(user);
        tourGuideService.addUser(user2);

        List<User> allUsers = tourGuideService.getAllUsers();

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
                .getNearByAttractions(visitedLocation, user);
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
