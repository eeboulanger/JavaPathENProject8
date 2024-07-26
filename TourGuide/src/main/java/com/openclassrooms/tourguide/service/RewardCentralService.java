package com.openclassrooms.tourguide.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import rewardCentral.RewardCentral;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Handles all operations involving the external library RewardCentral.
 * Calls to the library are done asynchronously with threads from executor service handled by the thread service.
 * The number of threads can be modified in the thread service.
 */
@Service
public class RewardCentralService {

    private final Logger logger = LoggerFactory.getLogger(RewardCentralService.class);
    private final RewardCentral rewardCentral;
    private final ThreadService threadService = new ThreadService();

    public RewardCentralService(RewardCentral rewardCentral) {
        this.rewardCentral = rewardCentral;
    }


    /**
     * Fetches reward points for an attraction
     *
     * @param attractionId of the attraction
     * @param userId       id of the user
     * @return a completable future of the number of points for further operations
     */
    public CompletableFuture<Integer> getAttractionRewardPoints(UUID attractionId, UUID userId) {
        return CompletableFuture.supplyAsync(() ->
                        rewardCentral.getAttractionRewardPoints(attractionId, userId), threadService.getThread())
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        logger.error("Failed to fetch reward points : " + exception.getMessage());
                    }
                    threadService.releaseThread();
                });
    }
}
