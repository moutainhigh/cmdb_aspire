package com.aspire.mirror.log.server.config;

/**
 * @Title: DruidProperties.java
 * @Package com.aspire.mirror.alert.server.config
 * @Description: Druid db configuration
 * Copyright: Copyright (c) 2017
 * Company:咪咕文化 tsg
 * @author tsg-frank
 * @date 2017年5月23日 下午3:32:38
 * @version V1.0
 */

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * druid
 */
@ConfigurationProperties(prefix = "druid")
public class DruidProperties {
    private String url;
    private String username;
    private String password;

    private int initialSize;
    private int minIdle;
    private int maxActive;
    private long maxWait;

    private String validationQuery;

    private boolean testOnBorrow;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getInitialSize() {
        return initialSize;
    }

    public void setInitialSize(int initialSize) {
        this.initialSize = initialSize;
    }

    public int getMinIdle() {
        return minIdle;
    }

    public void setMinIdle(int minIdle) {
        this.minIdle = minIdle;
    }

    public int getMaxActive() {
        return maxActive;
    }

    public void setMaxActive(int maxActive) {
        this.maxActive = maxActive;
    }

    public long getMaxWait() {
        return maxWait;
    }

    public void setMaxWait(long maxWait) {
        this.maxWait = maxWait;
    }

    public String getValidationQuery() {
        return validationQuery;
    }

    public void setValidationQuery(String validationQuery) {
        this.validationQuery = validationQuery;
    }

    public boolean isTestOnBorrow() {
        return testOnBorrow;
    }

    public void setTestOnBorrow(boolean testOnBorrow) {
        this.testOnBorrow = testOnBorrow;
    }
}