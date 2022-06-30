/*
 * Copyright (c) 2020-2022, justtoplay (justtoplay@126.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.justtoplay.xxl.job.plus.prop;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;

/**
 * 配置
 *
 * @author justtoplay
 * @since 2022/06/25 21:44
 */
@ConfigurationProperties(XxlJobPlusProperties.PREFIX)
public class XxlJobPlusProperties {

    public static final String PREFIX = "xxl.job.plus";

    private AdminProperties admin = new AdminProperties();

    private ExecutorProperties executor = new ExecutorProperties();

    @JsonIgnore
    @Autowired
    private Environment environment;

    @PostConstruct
    public void init() {
        if (!StringUtils.hasLength(this.getExecutor().getServiceName())) {
            String serviceName = environment
                    .resolvePlaceholders("${xxl.job.plus.executor.serviceName:}");
            if (!StringUtils.hasLength(serviceName)) {
                serviceName = environment.resolvePlaceholders(
                        "${spring.application.name}");
            }
            this.getExecutor().setServiceName(serviceName);
        }
    }

    public AdminProperties getAdmin() {
        return admin;
    }

    public void setAdmin(AdminProperties admin) {
        this.admin = admin;
    }

    public ExecutorProperties getExecutor() {
        return executor;
    }

    public void setExecutor(ExecutorProperties executor) {
        this.executor = executor;
    }

    public static class AdminProperties {

        private String serviceName;

        private String contextPath = "/xxl-job-admin";

        private String accessToken = "default_token";

        public String getServiceName() {
            return serviceName;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        public String getContextPath() {
            return contextPath;
        }

        public void setContextPath(String contextPath) {
            this.contextPath = contextPath;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }
    }

    public static class ExecutorProperties {

        private String serviceName;

        private String logPath = "./logs/xxl-job/jobhandler";

        private Integer logRetentionDays = 30;

        public String getServiceName() {
            return serviceName;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        public String getLogPath() {
            return logPath;
        }

        public void setLogPath(String logPath) {
            this.logPath = logPath;
        }

        public Integer getLogRetentionDays() {
            return logRetentionDays;
        }

        public void setLogRetentionDays(Integer logRetentionDays) {
            this.logRetentionDays = logRetentionDays;
        }
    }
}
