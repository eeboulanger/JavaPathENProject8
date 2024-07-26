package com.openclassrooms.tourguide.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;

/**
 * Handles access to thread pool with sempahore as limit
 */
@Service
public class ThreadService {

    private static final int SEMAPHORE_LIMIT = 100;
    private final Semaphore semaphore;
    private final ExecutorService executorService;
    private final Logger logger = LoggerFactory.getLogger(ThreadService.class);

    public ThreadService() {
        //Initialize limits of threads
        semaphore = new Semaphore(SEMAPHORE_LIMIT);
        executorService = Executors.newCachedThreadPool();
    }

    /**
     * Acquires a semaphore and returns executor service
     *
     * @return the ExecutorService instance if the semaphore is successfully acquired
     * @throws RuntimeException if acquiring the semaphore is interrupted
     */
    public ExecutorService getThread() {
        try {
            semaphore.acquire();
            return executorService;
        } catch (InterruptedException e) {
            logger.error("Semaphore acquisition error: " + e);
            throw new RuntimeException("Failed to acquire semaphore: " + e.getMessage());
        }
    }

    /**
     * Releases the semaphore
     */
    public void releaseThread() {
        semaphore.release();
    }
}

