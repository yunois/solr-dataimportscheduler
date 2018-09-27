package org.apache.solr.handler.dataimport.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimerTask;
/**
 * @author xu.yang@yunois.com
 */
public abstract class BaseTimerTask extends TimerTask {

    protected String syncEnabled;
    protected String[] syncCores;
    protected String server;
    protected String port;
    protected String webapp;
    protected String params;
    protected String interval;
    protected String cores;
    protected SolrDataImportProperties p;
    protected boolean singleCore;
    protected String reBuildIndexParams;
    protected String reBuildIndexBeginTime;
    protected String reBuildIndexInterval;
    protected String initialDelay;
    protected String threadPoolCount;
    protected static final Logger logger = LoggerFactory.getLogger(BaseTimerTask.class);

    public BaseTimerTask(String webAppName) throws Exception {
        p = new SolrDataImportProperties();
        this.reloadParams();
        this.fixParams(webAppName);
        if (!this.syncEnabled.equals("1")) {
            throw new Exception("Schedule disabled");
        } else {
            if (this.syncCores != null && (this.syncCores.length != 1 || !this.syncCores[0].isEmpty())) {
                this.singleCore = false;
                logger.info("<index update process> Multiple cores identified in dataimport.properties. Sync active for: " + this.cores);
            } else {
                this.singleCore = true;
                logger.info("<index update process> Single core identified in dataimport.properties");
            }

        }
    }

    protected void reloadParams() {
        this.p.loadProperties(true);
        this.syncEnabled = p.getProperty(SolrDataImportProperties.SYNC_ENABLED);
        this.cores = p.getProperty(SolrDataImportProperties.SYNC_CORES);
        this.server = p.getProperty(SolrDataImportProperties.SERVER);
        this.port = p.getProperty(SolrDataImportProperties.PORT);
        this.webapp = p.getProperty(SolrDataImportProperties.WEBAPP);
        this.params = p.getProperty(SolrDataImportProperties.PARAMS);
        this.interval = p.getProperty(SolrDataImportProperties.INTERVAL);
        this.syncCores = cores != null ? this.cores.split(",") : null;
        this.reBuildIndexParams = p.getProperty(SolrDataImportProperties.REBUILDINDEXPARAMS);
        this.reBuildIndexBeginTime = p.getProperty(SolrDataImportProperties.REBUILDINDEXBEGINTIME);
        this.reBuildIndexInterval = p.getProperty(SolrDataImportProperties.REBUILDINDEXINTERVAL);
        this.initialDelay = p.getProperty(SolrDataImportProperties.INITIAL_DELAY);
        this.threadPoolCount = p.getProperty(SolrDataImportProperties.THREAD_POOL_COUNT);
    }

    protected void fixParams(String webAppName) {
        if (this.server == null || this.server.isEmpty()) {
            this.server = "localhost";
        }

        if (this.port == null || this.port.isEmpty()) {
            this.port = "8080";
        }

        if (this.webapp == null || this.webapp.isEmpty()) {
            this.webapp = webAppName;
        }

        if (this.interval == null || this.interval.isEmpty() || this.getIntervalLong() <= 0) {
            this.interval = "30";
        }

        if (this.reBuildIndexBeginTime == null || this.reBuildIndexBeginTime.isEmpty()) {
            this.reBuildIndexBeginTime = "00:00:00";
        }

        if (this.reBuildIndexInterval == null || this.reBuildIndexInterval.isEmpty() || this.getReBuildIndexIntervalInt() <= 0) {
            this.reBuildIndexInterval = "0";
        }

    }

    protected void prepUrlSendHttpPost(String params) {
        String coreUrl = "http://" + this.server + ":" + this.port + "/" + this.webapp + params;
        this.sendHttpPost(coreUrl, (String) null);
    }

    protected void prepUrlSendHttpPost(String coreName, String params) {
        String coreUrl = "http://" + this.server + ":" + this.port + "/" + this.webapp + "/" + coreName + params;
        this.sendHttpPost(coreUrl, coreName);
    }

    protected void sendHttpPost(String completeUrl, String coreName) {
        DateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss SSS");
        Date startTime = new Date();
        String core = coreName == null ? "" : "[" + coreName + "] ";
        logger.info(core + "<index update process> Process started at .............. " + df.format(startTime));

        try {
            URL url = new URL(completeUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("type", "submit");
            conn.setDoOutput(true);
            conn.connect();
            logger.info(core + "<index update process> Full URL\t\t\t\t" + conn.getURL());
            logger.info(core + "<index update process> Response message\t\t\t" + conn.getResponseMessage());
            logger.info(core + "<index update process> Response code\t\t\t" + conn.getResponseCode());
            if (conn.getResponseCode() != 200) {
                this.reloadParams();
            }

            conn.disconnect();
            logger.info(core + "<index update process> Disconnected from server\t\t" + this.server);
            Date endTime = new Date();
            logger.info(core + "<index update process> Process ended at ................ " + df.format(endTime));
        } catch (MalformedURLException var9) {
            logger.error("Failed to assemble URL for HTTP POST", var9);
        } catch (IOException var10) {
            logger.error("Failed to connect to the specified URL while trying to send HTTP POST", var10);
        } catch (Exception var11) {
            logger.error("Failed to send HTTP POST", var11);
        }

    }

    public int getThreadPoolCountInt() {
        try {
            return Integer.parseInt(this.threadPoolCount);
        } catch (NumberFormatException var2) {
            logger.warn("Unable to convert 'threadPoolCount' to number. Using default value (10) instead", var2);
            return 10;
        }
    }

    public long getIntervalLong() {
        try {
            return Long.parseLong(this.interval);
        } catch (NumberFormatException var2) {
            logger.warn("Unable to convert 'interval' to number. Using default value (30) instead", var2);
            return 30;
        }
    }

    public long getInitialDelayLong() {
        try {
            return Long.parseLong(this.initialDelay);
        } catch (NumberFormatException var2) {
            logger.warn("Unable to convert 'initialDelay' to number. Using default value (30) instead", var2);
            return 30;
        }
    }

    public int getReBuildIndexIntervalInt() {
        try {
            return Integer.parseInt(this.reBuildIndexInterval);
        } catch (NumberFormatException var2) {
            logger.info("Unable to convert 'reBuildIndexInterval' to number. do't rebuild index.", var2);
            return 0;
        }
    }

    public Date getReBuildIndexBeginTime() {
        Date beginDate = null;

        try {
            SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");
            String dateStr = sdfDate.format(new Date());
            beginDate = sdfDate.parse(dateStr);
            if (this.reBuildIndexBeginTime != null && !this.reBuildIndexBeginTime.isEmpty()) {
                SimpleDateFormat sdf;
                if (this.reBuildIndexBeginTime.matches("\\d{2}:\\d{2}:\\d{2}")) {
                    sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    beginDate = sdf.parse(dateStr + " " + this.reBuildIndexBeginTime);
                } else if (this.reBuildIndexBeginTime.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
                    sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    beginDate = sdf.parse(this.reBuildIndexBeginTime);
                }
                return beginDate;
            } else {
                return beginDate;
            }
        } catch (ParseException var5) {
            logger.warn("Unable to convert 'reBuildIndexBeginTime' to date. use now time.", var5);
            return beginDate;
        }
    }
}
