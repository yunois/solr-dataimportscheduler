package org.apache.solr.handler.dataimport.scheduler;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author xu.yang@yunois.com
 */
public class ApplicationListener implements ServletContextListener {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationListener.class);

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {

    }

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {

        ServletContext servletContext = servletContextEvent.getServletContext();
        try {
            // 增量更新任务计划
            // create the timer and timer task objects
            DeltaImportHTTPPostScheduler task = new DeltaImportHTTPPostScheduler(servletContext.getServletContextName());

            // get our interval from HTTPPostScheduler
            int threadPoolCount = task.getThreadPoolCountInt();
            long interval = task.getIntervalLong();
            long initialDelay = task.getInitialDelayLong();

            // schedule the task
            ScheduledExecutorService deltaImportExecutorService = new ScheduledThreadPoolExecutor(threadPoolCount, new BasicThreadFactory.Builder().namingPattern("deltaImport-schedule-pool-%d").daemon(true).build());

            deltaImportExecutorService.scheduleAtFixedRate(task, 60 * initialDelay, interval, TimeUnit.SECONDS);

            // 重做索引任务计划
            FullImportHTTPPostScheduler fullImportTask = new FullImportHTTPPostScheduler(servletContext.getServletContextName());

            int reBuildIndexInterval = fullImportTask.getReBuildIndexIntervalInt();

            if (reBuildIndexInterval <= 0) {
                logger.warn("Full Import Schedule disabled");
                return;
            }

            Calendar fullImportCalendar = Calendar.getInstance();
            Date beginDate = fullImportTask.getReBuildIndexBeginTime();
            fullImportCalendar.setTime(beginDate);
            fullImportCalendar.add(Calendar.MINUTE, reBuildIndexInterval);
            long fullImportStartTimeMillis = fullImportCalendar.getTime().getTime();
            long nowFullImportStartTimeMillis = System.currentTimeMillis();

            // schedule the task
            ScheduledExecutorService fullImportExecutorService = new ScheduledThreadPoolExecutor(threadPoolCount, new BasicThreadFactory.Builder().namingPattern("fullImport-schedule-pool-%d").daemon(true).build());

            fullImportExecutorService.scheduleAtFixedRate(fullImportTask, fullImportStartTimeMillis - nowFullImportStartTimeMillis, 1000L * 60 * reBuildIndexInterval, TimeUnit.MILLISECONDS);

        } catch (Exception e) {
            if (e.getMessage().endsWith("disabled")) {
                logger.warn("Schedule disabled");
            } else {
                logger.error("Problem initializing the scheduled task: ", e);
            }
        }

    }

}
