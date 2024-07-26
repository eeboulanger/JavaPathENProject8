package com.openclassrooms.tourguide.service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Handles all operations involving the external library GpsUtil. Calls to the library are done asynchronously with
 * threads from executor service handled by the thread service.
 * The number of threads can be modified in the thread service.
 */

@Service
public class GpsUtilService {

    private final Logger logger = LoggerFactory.getLogger(GpsUtilService.class);
    private final GpsUtil gpsUtil;
    private final ThreadService threadService = new ThreadService();

    public GpsUtilService(GpsUtil gpsUtil) {
        this.gpsUtil = gpsUtil;
    }

    /**
     * Fetches the users current location
     *
     * @param userId of the user
     * @return a completable future of visited location for further operations
     */

    public CompletableFuture<VisitedLocation> getUserLocation(UUID userId) {
        return CompletableFuture.supplyAsync(() -> gpsUtil.getUserLocation(userId), threadService.getThread())
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        logger.error("Failed to fetch user location: " + exception.getMessage());
                    }
                    threadService.releaseThread();
                });
    }

    /**
     * Fetches all attractions
     * @return a completable future of list of attractions for further operations if needed
     */
    public CompletableFuture<List<Attraction>> getAttractions() {
        return CompletableFuture.supplyAsync(gpsUtil::getAttractions, threadService.getThread())
                .whenComplete((result, exception) -> {
                            if (exception != null) {
                                logger.error("Failed to fetch list of attractions: " + exception.getMessage());
                            }
                            threadService.releaseThread();
                        }
                );
    }
}
