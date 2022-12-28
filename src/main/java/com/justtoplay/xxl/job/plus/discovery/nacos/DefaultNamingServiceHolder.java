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

import com.alibaba.boot.nacos.discovery.properties.NacosDiscoveryProperties;
import com.alibaba.boot.nacos.discovery.properties.Register;
import com.alibaba.nacos.api.annotation.NacosInjected;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.utils.NetUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * nacos namingService-默认
 *
 * @author justtoplay
 * @since 2022/06/26 23:27
 */
@Component
@ConditionalOnMissingBean(NamingServiceHolder.class)
public class DefaultNamingServiceHolder implements NamingServiceHolder {

    @NacosInjected
    private NamingService namingService;

    @Autowired
    private NacosDiscoveryProperties discoveryProperties;

    @Value("${spring.application.name:}")
    private String applicationName;

    @Value("${local.server.port}")
    private Integer port;

    @Override
    public NamingService get() {
        return namingService;
    }

    @Override
    public String getExecutorAddress() {

        Register register = discoveryProperties.getRegister();

        if (StringUtils.isEmpty(register.getIp())) {
            register.setIp(NetUtils.localIP());
        }

        if (register.getPort() == 0) {
            register.setPort(port);
        }

        return register.getIp() + ":" + register.getPort();
    }

    @Override
    public String getServiceName() {
        String serviceName = discoveryProperties.getRegister().getServiceName();

        if (StringUtils.isEmpty(serviceName)){
            serviceName = applicationName;
        }
        return serviceName;
    }
}
