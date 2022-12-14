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

package com.justtoplay.xxl.job.plus.discovery.nacos;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.NacosServiceManager;
import com.alibaba.nacos.api.naming.NamingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

/**
 * nacos namingService-spring cloud
 *
 * @author justtoplay
 * @since 2022/06/26 23:27
 */
@Component
@ConditionalOnClass({NacosServiceManager.class})
public class CloudNamingServiceHolder implements NamingServiceHolder {

    @Autowired
    private NacosServiceManager nacosServiceManager;

    @Autowired
    private NacosDiscoveryProperties nacosDiscoveryProperties;

    @Override
    public NamingService get() {
        return nacosServiceManager.getNamingService();
    }

    @Override
    public String getExecutorAddress() {
        return nacosDiscoveryProperties.getIp() + ":" + nacosDiscoveryProperties.getPort();
    }

    @Override
    public String getServiceName() {
        return nacosDiscoveryProperties.getService();
    }
}
