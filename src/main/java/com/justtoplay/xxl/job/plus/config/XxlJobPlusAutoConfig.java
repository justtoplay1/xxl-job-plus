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

package com.justtoplay.xxl.job.plus.config;

import com.justtoplay.xxl.job.plus.discovery.DiscoveryProvider;
import com.justtoplay.xxl.job.plus.event.DiscoveryEvent;
import com.justtoplay.xxl.job.plus.event.ServiceDownEvent;
import com.justtoplay.xxl.job.plus.event.ServiceRefreshEvent;
import com.justtoplay.xxl.job.plus.event.ServiceUpEvent;
import com.justtoplay.xxl.job.plus.executor.XxlJobPlusSpringExecutor;
import com.justtoplay.xxl.job.plus.prop.XxlJobPlusProperties;
import com.xxl.job.core.biz.ExecutorBiz;
import com.xxl.job.core.biz.impl.ExecutorBizImpl;
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;

import javax.annotation.Resource;

/**
 * 自动配置
 *
 * @author justtoplay
 * @since 2022/06/25 22:05
 */
@EnableConfigurationProperties(XxlJobPlusProperties.class)
@Configuration
public class XxlJobPlusAutoConfig implements ApplicationListener<DiscoveryEvent> {
    private static final Logger logger = LoggerFactory.getLogger(XxlJobPlusAutoConfig.class);

    @Resource
    private XxlJobPlusProperties xxlJobPlusProperties;

    @Resource
    private ApplicationContext applicationContext;

    @Resource
    private DiscoveryProvider discoveryProvider;

    private final Object lock = new Object();

    private boolean isRunning = false;

    @Async
    @Override
    public void onApplicationEvent(DiscoveryEvent event) {
        synchronized (lock) {
            XxlJobPlusSpringExecutor xxlJobExecutor = applicationContext.getBean("xxlJobExecutor", XxlJobPlusSpringExecutor.class);
            if (event instanceof ServiceUpEvent) {
                if (!isRunning) {
                    xxlJobExecutor.start();
                }
            } else if (event instanceof ServiceDownEvent) {
                if (isRunning) {
                    xxlJobExecutor.stop();
                }
            } else if (event instanceof ServiceRefreshEvent) {
                ServiceRefreshEvent serviceRefreshEvent = (ServiceRefreshEvent) event;
                if (isRunning) {
                    if (serviceRefreshEvent.getAddressList().size() == 0) {
                        xxlJobExecutor.stop();
                    } else {
                        xxlJobExecutor.refresh(serviceRefreshEvent.getAddressList());
                    }
                } else {
                    if (serviceRefreshEvent.getAddressList().size() > 0) {
                        xxlJobExecutor.start();
                    }
                }
            }
            isRunning = xxlJobExecutor.getRunningStatus();
        }
    }

    @Lazy
    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        return new XxlJobPlusSpringExecutor(xxlJobPlusProperties, discoveryProvider);
    }

    @Lazy
    @Bean
    public ExecutorBiz executorBizImpl() {
        return new ExecutorBizImpl();
    }
}
