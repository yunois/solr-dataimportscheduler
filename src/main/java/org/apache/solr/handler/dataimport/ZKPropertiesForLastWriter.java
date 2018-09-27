/*
 * Project Name:solr-import-schedule
 * File Name:ZKPropertiesForLastWriter.java
 * Package Name:org.apache.solr.handler.dataimport.ZKPropertiesForLastWriter
 * Date:2018-09-27 16:50:10
 * Copyright (c) 2018, ehking All Rights Reserved.
 */
package org.apache.solr.handler.dataimport;

import org.apache.solr.common.cloud.SolrZkClient;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

/**
 * @author xu.yang@yunois.com
 * <p>
 *  用原生的last_index_time,会有丢数据的风险。
 *  此方法基于原ZKPropertiesWriter改动
 *  在读取zk中last_index_time的时候，动态往前推before_second秒
 *  如：
 *  dataimport.properties:
 *      before_second=300
 *      onlinepayorder.last_index_time=2018-09-25 01\:58\:36
 *      last_index_time=2018-09-25 01\:58\:36
 *
 *  如用到此方式，需在dataConfig tag下面增加一个tag "propertyWriter"
 *  <propertyWriter dateFormat="yyyy-MM-dd HH:mm:ss" type="ZKPropertiesForLastWriter" directory="data" filename="dataimport.properties"/>
 *
 *  A SolrCloud-friendly extension of {@link SimplePropertiesWriter}.
 *  This implementation ignores the "directory" parameter, saving
 *  the properties file under /configs/[solrcloud collection name]/
 */
public class ZKPropertiesForLastWriter extends SimplePropertiesWriter {
  
  private static final Logger log = LoggerFactory
      .getLogger(ZKPropertiesForLastWriter.class);
  
  private String path;
  private SolrZkClient zkClient;
  
  private static final String BEFORE_SECOND = "before_second";
  private static final String LAST_INDEX_TIME = "last_index_time";
  private static final SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  
  @Override
  public void init(DataImporter dataImporter, Map<String, String> params) {
    super.init(dataImporter, params);
    zkClient = dataImporter.getCore().getCoreDescriptor().getCoreContainer()
        .getZkController().getZkClient();
  }
  
  @Override
  protected void findDirectory(DataImporter dataImporter, Map<String, String> params) {
    String collection = dataImporter.getCore().getCoreDescriptor()
        .getCloudDescriptor().getCollectionName();
    path = "/configs/" + collection + "/" + filename;
  }
  
  @Override
  public boolean isWritable() {
    return true;
  }
  
  @Override
  public void persist(Map<String, Object> propObjs) {
    Properties existing = mapToProperties(readIndexerProperties());
    existing.putAll(mapToProperties(propObjs));
    StringWriter output = new StringWriter();
    try {
      existing.store(output, null);
      byte[] bytes = output.toString().getBytes(StandardCharsets.UTF_8);
      if (!zkClient.exists(path, false)) {
        try {
          zkClient.makePath(path, false);
        } catch (NodeExistsException e) {}
      }
      zkClient.setData(path, bytes, false);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.warn(
          "Could not persist properties to " + path + " :" + e.getClass(), e);
    } catch (Exception e) {
      log.warn(
          "Could not persist properties to " + path + " :" + e.getClass(), e);
    }
  }
  
  @Override
  public Map<String, Object> readIndexerProperties() {
    Properties props = new Properties();
    try {
      byte[] data = zkClient.getData(path, null, null, false);
      if (data != null) {
        props.load(new StringReader(new String(data, StandardCharsets.UTF_8)));
      }
      this.processNewLastIndex(props);
    } catch (Exception e) {
      log.warn(
          "Could not read DIH properties from " + path + " :" + e.getClass(), e);
    }
    return propertiesToMap(props);
  }
    
    /**
     * 处理last_index_time
     * @param props
     * @throws ParseException
     */
  private void processNewLastIndex(Properties props) throws ParseException {
      log.info("---> processNewLastIndex props:{}",props);
      if(!props.containsKey(BEFORE_SECOND)){
          return;
      }
      int before_second =  Integer.parseInt(String.valueOf(props.get(BEFORE_SECOND)));
      String lastIndex = String.valueOf(props.get(LAST_INDEX_TIME));
      Date lastDate = sdfDate.parse(lastIndex);
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(lastDate);
      calendar.add(Calendar.SECOND, -before_second);
      String newLastIndex = sdfDate.format(calendar.getTime());
      if(0 == before_second){
          return;
      }
      for(Object key:props.keySet()){
          if(String.valueOf(key).indexOf(LAST_INDEX_TIME)>=0){
              props.put(key,newLastIndex);
          }
      }
      log.info("---> processNewLastIndex new props:{}",props);
  }
}
