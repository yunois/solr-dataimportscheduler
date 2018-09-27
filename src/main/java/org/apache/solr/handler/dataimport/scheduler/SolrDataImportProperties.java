package org.apache.solr.handler.dataimport.scheduler;

import org.apache.solr.core.SolrResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

/**
 * @author xu.yang@yunois.com
 */
public class SolrDataImportProperties {
	private Properties properties;
	public static final String SYNC_ENABLED = "syncEnabled";
	public static final String SYNC_CORES = "syncCores";
	public static final String SERVER = "server";
	public static final String PORT = "port";
	public static final String WEBAPP = "webapp";
	public static final String PARAMS = "params";
	public static final String INTERVAL = "interval";
	public static final String REBUILDINDEXPARAMS = "reBuildIndexParams";
	public static final String REBUILDINDEXBEGINTIME = "reBuildIndexBeginTime";
	public static final String REBUILDINDEXINTERVAL = "reBuildIndexInterval";
	public static final String INITIAL_DELAY = "initialDelay";
	public static final String THREAD_POOL_COUNT = "threadPoolCount";
	private static final Logger logger = LoggerFactory.getLogger(SolrDataImportProperties.class);

    public void loadProperties(boolean force) {
        try (SolrResourceLoader loader = new SolrResourceLoader((String) null)) {

            logger.info("Instance dir = {}", loader.getInstanceDir());
            String configDir = loader.getConfigDir();
            configDir = SolrResourceLoader.normalizeDir(configDir);
            if (force || this.properties == null) {
                this.properties = new Properties();
                String dataImportPropertiesPath = configDir + "dataimport.properties";
                FileInputStream fis = new FileInputStream(dataImportPropertiesPath);
                this.properties.load(fis);
            }
        } catch (FileNotFoundException var6) {
            logger.error("Error locating DataImportScheduler dataimport.properties file", var6);
        } catch (IOException var7) {
            logger.error("Error reading DataImportScheduler dataimport.properties file", var7);
        } catch (Exception var8) {
            logger.error("Error loading DataImportScheduler properties", var8);
        }

    }

    public String getProperty(String key) {
        return this.properties.getProperty(key);
    }
}
