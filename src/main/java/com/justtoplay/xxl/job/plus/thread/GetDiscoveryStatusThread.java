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

package com.justtoplay.xxl.job.plus.thread;

import com.justtoplay.xxl.job.plus.discovery.DiscoveryProvider;
import com.justtoplay.xxl.job.plus.event.ServiceUpEvent;
import com.justtoplay.xxl.job.plus.util.SpringContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 获取注册中心有没有注册成功
 *
 * @author justtoplay
 * @since 2022/06/26 22:46
 */
public class GetDiscoveryStatusThread {

    private static final Logger logger = LoggerFactory.getLogger(GetDiscoveryStatusThread.class);

    private static GetDiscoveryStatusThread instance = new GetDiscoveryStatusThread();

    public static GetDiscoveryStatusThread getInstance() {
        return instance;
    }

    private Thread getResisterStatusThread;

    private volatile boolean toStop = false;

    private static final int INTERVAL = 3;

    public void start(String address, String serviceName, DiscoveryProvider discoveryProvider) {
        // valid
        if (address == null || address.trim().length() == 0) {
            logger.warn(">>>>>>>>>>> xxl-job-plus, executor get discovery status fail, address is null.");
            return;
        }
        if (serviceName == null || serviceName.trim().length() == 0) {
            logger.warn(">>>>>>>>>>> xxl-job-plus, executor get discovery status fail, serviceName is null.");
            return;
        }
        if (discoveryProvider == null) {
            logger.warn(">>>>>>>>>>> xxl-job-plus, executor get discovery status fail, discoveryProvider is null.");
            return;
        }

        if (getResisterStatusThread == null) {
            getResisterStatusThread = new Thread(() -> {
                while (!toStop) {
                    try {
                        List<String> addressList = discoveryProvider.getServiceAddressList(serviceName);
                        if (addressList.contains(address)) {
                            SpringContextUtil.getApplicationContext().publishEvent(new ServiceUpEvent(this));
                            toStop = true;
                        }
                    } catch (Exception e) {
                        if (!toStop) {
                            logger.error(e.getMessage(), e);
                        }
                    }

                    try {
                        if (!toStop) {
                            TimeUnit.SECONDS.sleep(INTERVAL);
                        }
                    } catch (InterruptedException e) {
                        if (!toStop) {
                            logger.warn(">>>>>>>>>>> xxl-job-plus, executor get discovery status thread interrupted, error msg:{}", e.getMessage());
                        }
                    }
                }
            });
            getResisterStatusThread.setDaemon(true);
            getResisterStatusThread.setName("xxl-job-plus, executor GetDiscoveryStatusThread");
        }

        //todo 启动前需要检查状态是不是已经启动过了
//        if (getResisterStatusThread.getState() != Thread.State.RUNNABLE) {
        getResisterStatusThread.start();
//        }
    }

    public void toStop() {
        toStop = true;

        // interrupt and wait
        if (getResisterStatusThread != null) {
            getResisterStatusThread.interrupt();
            try {
                getResisterStatusThread.join();
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }

    }
}
