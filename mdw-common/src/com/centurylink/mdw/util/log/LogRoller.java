package com.centurylink.mdw.util.log;

import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class LogRoller implements Runnable {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private static volatile LogRoller instance;
    public static LogRoller getInstance() {
        if (instance == null) {
            synchronized (LogRoller.class) {
                if (instance == null) {
                    instance = new LogRoller();
                }
            }
        }
        return instance;
    }

    private LogRoller() {}

    private ScheduledFuture schedule;
    private int retain;
    private List<String> files;

    public void start() {
        if (schedule != null) {
            schedule.cancel(false);
        }
        retain = PropertyManager.getIntegerProperty(PropertyNames.MDW_LOG_ROLLER_RETAIN, 30);
        files = PropertyManager.getListProperty(PropertyNames.MDW_LOG_ROLLER_FILES);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        Long midnight = LocalDateTime.now().until(LocalDate.now().plusDays(1).atStartOfDay(), ChronoUnit.MILLIS);
        // make sure we don't run before midnight
        Long extra = TimeUnit.SECONDS.toMillis(PropertyManager.getIntegerProperty("mdw.logging.roller.extra", 30));
        logger.info("Log Roller execution scheduled for: " + new Date(midnight + extra + System.currentTimeMillis()));
        schedule = scheduler.scheduleAtFixedRate(this, midnight + extra, TimeUnit.DAYS.toMillis(1), TimeUnit.MILLISECONDS);
    }

    public void stop() {
        if (schedule != null) {
            schedule.cancel(true);
        }
    }

    @Override
    public void run() {
        LocalDateTime now = LocalDateTime.now();
        // if I'm at 11 pm, it could mean the switch FROM Daylight Saving has occurred
        if (now.getHour() == 23) {
            // reschedule for midnight
            start();
            return;
        }

        if (files != null) {
            LocalDate today = LocalDate.now();
            logger.info("LogRoller executing at " + new Date());
            for (String file : files) {
                try {
                    Files.deleteIfExists(toPath(file, today.minusDays(retain + 1)));
                    Path path = new File(file).toPath();
                    Files.copy(path, toPath(file, today.minusDays(1)), REPLACE_EXISTING);
                    Files.write(path, new byte[0], StandardOpenOption.TRUNCATE_EXISTING);
                }
                catch (IOException ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        }

        // if I just ran at 1 am, it could mean the switch TO Daylight Saving has occurred
        // (adjust prior to next cycle)
        if (now.getHour() == 1) {
            start();
        }
    }

    /**
     * Rolled file naming (eg catalina.out.2019-11-20).
     */
    private Path toPath(String file, LocalDate localDate) {
        return new File(file + "." + localDate).toPath();
    }

    /**
     * Runs log rolling immediately.
     * Arguments:
     *   retainDays = number of days to retain before deleting older logs
     *   files = comma-separated list of files
     */
    public static void main(String[] args) {
        if (args.length != 2)
            throw new IllegalArgumentException("args: [retainDays] [files]");
        LogRoller logRoller = new LogRoller();
        logRoller.retain = Integer.parseInt(args[0]);
        logRoller.files = Arrays.asList(args[1].split(","));
        logRoller.run();
    }
}
