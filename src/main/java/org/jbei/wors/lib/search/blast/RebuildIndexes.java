package org.jbei.wors.lib.search.blast;

import org.jbei.wors.lib.common.logging.Logger;
import org.jbei.wors.lib.executor.Task;
import org.jbei.wors.lib.index.PartsIndex;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Task to rebuild the blast index
 *
 * @author Hector Plahar
 */
public class RebuildIndexes extends Task {

    private Timer timer = new Timer(true);
    private boolean stopped;
    private final Object LOCK_OBJECT = new Object();
    private int exceptionCount;
    private final int MONTH_IN_SECONDS = 2629746;

    @Override
    public void execute() {
        Path indexPath = Paths.get(Constants.DATA_DIR, Constants.BLAST_DB_FOLDER_NAME, Constants.BLAST_DB_NAME + ".nsq");

        // first run on task start up
        if (!Files.exists(indexPath)) {
            Logger.info("Index not found at path " + indexPath.toString() + ". Rebuilding...");
            rebuild();
        } else {
            try {
                // else check time since rebuild
                FileTime time = Files.getLastModifiedTime(indexPath);
                long timeSince = Instant.now().getEpochSecond() - time.toInstant().getEpochSecond();
                if (timeSince >= MONTH_IN_SECONDS) {
                    Logger.info("Stale index. Rebuilding...");
                    rebuild();
                }
            } catch (IOException e) {
                Logger.error(e);
            }
        }

        while (!stopped) {
            waitUntil(timeTillRun());
            rebuild();
        }

        Logger.info("Indexes rebuild task stopped");
        timer.cancel();
        timer.purge();
    }

    public void stop() {
        this.stopped = true;
    }

    private void rebuild() {
        Logger.info("Running blast rebuild task");

        try (PartsIndex partsIndex = new PartsIndex(true)) {
            partsIndex.rebuildAll();
        } catch (Exception ioe) {
            Logger.error(ioe);
            if (exceptionCount++ >= 10) {
                Logger.error(exceptionCount + " exceptions encountered. Aborting indexes rebuild");
                stopped = true;
            }
        }
    }

    private Date timeTillRun() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 7); // 1 week till next run
        calendar.set(Calendar.HOUR, 1);
        return calendar.getTime();
    }

    /**
     * Uses the defined lock object to wait until a specified time
     *
     * @param date date to notify lock object (awake thread)
     */
    private void waitUntil(Date date) {
        Logger.info("Waiting till " + date);

        // lock object
        synchronized (LOCK_OBJECT) {
            try {
                LOCK_OBJECT.wait();
            } catch (InterruptedException ie) {
                Logger.warn(ie.getMessage());
                stopped = true;
                return;
            }
        }

        // define and schedule task to "notify" at scheduled time
        timer.schedule(new TimerTask() {
            public void run() {
                synchronized (LOCK_OBJECT) {
                    LOCK_OBJECT.notify();
                }
            }
        }, date);
    }
}
