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

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.justtoplay.xxl.job.plus.discovery.DiscoveryProvider;
import com.justtoplay.xxl.job.plus.event.ServiceDownEvent;
import com.justtoplay.xxl.job.plus.event.ServiceRefreshEvent;
import com.justtoplay.xxl.job.plus.event.ServiceUpEvent;
import com.justtoplay.xxl.job.plus.thread.GetDiscoveryStatusThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * nacos服务发现
 *
 * @author justtoplay
 * @since 2022/06/26 19:24
 */
@Component
@ConditionalOnClass(NamingService.class)
@ConditionalOnMissingBean(DiscoveryProvider.class)
@AutoConfigureAfter(NamingServiceHolder.class)
public class NacosDiscoveryProvider implements DiscoveryProvider, DisposableBean {

    private final static Logger logger = LoggerFactory.getLogger(NacosDiscoveryProvider.class);

    @Autowired
    private NamingServiceHolder namingServiceHolder;

    @Autowired
    private ApplicationContext applicationContext;

    private String currentExecutorAddress;

    private String executorServiceName;

    @PostConstruct
    public void init() {
        logger.info(">>>>>>>>>>> xxl-job-plus, NacosDiscoveryProvider init");

        if (namingServiceHolder != null) {
            currentExecutorAddress = namingServiceHolder.getExecutorAddress();
            executorServiceName = namingServiceHolder.getServiceName();
            GetDiscoveryStatusThread.getInstance().start(currentExecutorAddress, executorServiceName,
                    NacosDiscoveryProvider.this);
        }
    }

    /**
     * 获取地址列表
     *
     * @param serviceName
     * @return
     */
    @Override
    public List<String> getServiceAddressList(String serviceName) {
        try {
            final List<Instance> instances = namingServiceHolder.get().getAllInstances(serviceName, true);
            if (instances != null) {
                return instances.stream().map(instance ->
                        instance.getIp() + ":" + instance.getPort()).collect(Collectors.toList());
            }
        } catch (NacosException e) {
            logger.error("NacosDiscoveryProvider getServiceAddressList error: serviceName={}, msg={}", serviceName, e.getMessage());
        }
        return new ArrayList<>();
    }

    /**
     * 获取当前服务注册ip:port
     *
     * @return
     */
    @Override
    public String getCurrentServiceAddress() {
        return currentExecutorAddress;
    }

    /**
     * 监听当前服务是否启用状态
     */
    @Override
    public void listenCurrentServiceStatus() {
        try {
            namingServiceHolder.get().subscribe(executorServiceName, new EventListener() {
                @Override
                public void onEvent(Event event) {
                    if (event instanceof NamingEvent) {
                        NamingEvent namingEvent = (NamingEvent) event;
                        if (namingEvent.getInstances().size() == 0) {
                            applicationContext.publishEvent(new ServiceDownEvent(this));
                        }

                        for (Instance instance : namingEvent.getInstances()) {
                            String address = instance.getIp() + ":" + instance.getPort();
                            if (address.equals(currentExecutorAddress)) {
                                if (instance.isEnabled() && instance.isHealthy()) {
                                    applicationContext.publishEvent(new ServiceUpEvent(this));
                                } else {
                                    applicationContext.publishEvent(new ServiceDownEvent(this));
                                }
                            }
                        }
                    }
                }
            });
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 监听服务状态
     *
     * @param serviceName
     */
    @Override
    public void listenServiceStatus(String serviceName) {
        try {
            namingServiceHolder.get().subscribe(serviceName, new EventListener() {
                @Override
                public void onEvent(Event event) {
                    if (event instanceof NamingEvent) {
                        NamingEvent namingEvent = (NamingEvent) event;
                        List<String> addressList = new ArrayList<>();
                        for (Instance instance : namingEvent.getInstances()) {
                            if (instance.isEnabled() && instance.isHealthy()) {
                                String address = instance.getIp() + ":" + instance.getPort();
                                if (!addressList.contains(address)) {
                                    addressList.add(address);
                                }
                            }
                        }
                        applicationContext.publishEvent(new ServiceRefreshEvent(this, addressList));
                    }
                }
            });
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void destroy() {
        GetDiscoveryStatusThread.getInstance().toStop();
    }
}
