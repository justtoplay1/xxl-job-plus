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

package com.justtoplay.xxl.job.plus.executor;

import com.justtoplay.xxl.job.plus.discovery.DiscoveryProvider;
import com.justtoplay.xxl.job.plus.prop.XxlJobPlusProperties;
import com.justtoplay.xxl.job.plus.util.SpringContextUtil;
import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import com.xxl.job.core.glue.GlueFactory;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.log.XxlJobFileAppender;
import com.xxl.job.core.thread.ExecutorRegistryThread;
import com.xxl.job.core.thread.JobLogFileCleanThread;
import com.xxl.job.core.thread.JobThread;
import com.xxl.job.core.thread.TriggerCallbackThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 执行器
 *
 * @author justtoplay
 * @since 2022/06/26 17:29
 */
@SuppressWarnings("unchecked")
public class XxlJobPlusSpringExecutor extends XxlJobSpringExecutor {

    private static final Logger logger = LoggerFactory.getLogger(XxlJobPlusSpringExecutor.class);

    private final XxlJobPlusProperties xxlJobPlusProperties;

    private final DiscoveryProvider discoveryProvider;

    private boolean isRunning = false;

    public XxlJobPlusSpringExecutor(XxlJobPlusProperties xxlJobPlusProperties, DiscoveryProvider discoveryProvider) {
        this.xxlJobPlusProperties = xxlJobPlusProperties;
        this.discoveryProvider = discoveryProvider;
        discoveryProvider.listenCurrentServiceStatus();
        discoveryProvider.listenServiceStatus(xxlJobPlusProperties.getAdmin().getServiceName());
    }

    /**
     * start
     */
    @Override
    public void afterSingletonsInstantiated() {

    }

    public boolean getRunningStatus() {
        return isRunning;
    }

    @Override
    public void start() {
        try {
            //valid
            String adminServiceName = xxlJobPlusProperties.getAdmin().getServiceName();
            if (adminServiceName == null || adminServiceName.trim().length() == 0) {
                logger.warn(">>>>>>>>>>> xxl-job-plus, XxlJobPlusSpringExecutor start fail, adminServiceName is null.");
                return;
            }

            String serviceAddressList = "";
            for (String item : discoveryProvider.getServiceAddressList(adminServiceName)) {
                item = "http://" + item + (xxlJobPlusProperties.getAdmin().getContextPath().startsWith("/") ?
                        xxlJobPlusProperties.getAdmin().getContextPath() : "/" + xxlJobPlusProperties.getAdmin().getContextPath());
                serviceAddressList = serviceAddressList + "," + item;
            }

            //valid
            if (serviceAddressList.trim().length() == 0) {
                logger.warn(">>>>>>>>>>> xxl-job-plus, XxlJobPlusSpringExecutor start fail, adminAddressList is null.");
                return;
            }

            logger.info(">>>>>>>>>>> xxl-job-plus, XxlJobPlusSpringExecutor start");
            Method initJobHandlerMethodRepository = XxlJobSpringExecutor.class.getDeclaredMethod("initJobHandlerMethodRepository", ApplicationContext.class);
            initJobHandlerMethodRepository.setAccessible(true);
            initJobHandlerMethodRepository.invoke(this, SpringContextUtil.getApplicationContext());

            // refresh GlueFactory
            GlueFactory.refreshInstance(1);
            // init logpath
            XxlJobFileAppender.initLogPath(xxlJobPlusProperties.getExecutor().getLogPath());

            Method initAdminBizList = XxlJobExecutor.class.getDeclaredMethod("initAdminBizList"
                    , String.class, String.class);
            initAdminBizList.setAccessible(true);

            initAdminBizList.invoke(this, serviceAddressList.substring(1), xxlJobPlusProperties.getAdmin().getAccessToken());

            // init JobLogFileCleanThread
            Field instanceJobLogFileCleanThread = JobLogFileCleanThread.class.getDeclaredField("instance");
            instanceJobLogFileCleanThread.setAccessible(true);
            instanceJobLogFileCleanThread.set(JobLogFileCleanThread.getInstance(), new JobLogFileCleanThread());
            JobLogFileCleanThread.getInstance().start(xxlJobPlusProperties.getExecutor().getLogRetentionDays());

            // init TriggerCallbackThread
            Field instanceTriggerCallbackThread = TriggerCallbackThread.class.getDeclaredField("instance");
            instanceTriggerCallbackThread.setAccessible(true);
            instanceTriggerCallbackThread.set(TriggerCallbackThread.getInstance(), new TriggerCallbackThread());
            TriggerCallbackThread.getInstance().start();

            String address = "http://{ip_port}/".replace("{ip_port}", discoveryProvider.getCurrentServiceAddress());

            // init ExecutorRegistryThread
            Field instanceExecutorRegistryThread = ExecutorRegistryThread.class.getDeclaredField("instance");
            instanceExecutorRegistryThread.setAccessible(true);
            instanceExecutorRegistryThread.set(ExecutorRegistryThread.getInstance(), new ExecutorRegistryThread());
            ExecutorRegistryThread.getInstance().start(xxlJobPlusProperties.getExecutor().getServiceName(), address);
            isRunning = true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void destroy() {
        stop();
    }

    public void stop() {
        ExecutorRegistryThread.getInstance().toStop();

        // destroy jobThreadRepository
        if (getJobThreadRepository().size() > 0) {
            for (Map.Entry<Integer, JobThread> item : getJobThreadRepository().entrySet()) {
                JobThread oldJobThread = removeJobThread(item.getKey(), "web container destroy and kill the job.");
                // wait for job thread push result to callback queue
                if (oldJobThread != null) {
                    try {
                        oldJobThread.join();
                    } catch (InterruptedException e) {
                        logger.error(">>>>>>>>>>> xxl-job, JobThread destroy(join) error, jobId:{}", item.getKey(), e);
                    }
                }
            }
            getJobThreadRepository().clear();
        }
        getJobThreadRepository().clear();

        getJobHandlerRepository().clear();

        // destroy JobLogFileCleanThread
        JobLogFileCleanThread.getInstance().toStop();

        // destroy TriggerCallbackThread
        TriggerCallbackThread.getInstance().toStop();
        isRunning = false;
        logger.info(">>>>>>>>>>> xxl-job-plus, XxlJobPlusSpringExecutor destroy");
    }

    public void refresh(List<String> adminAddressList) {
        synchronized (XxlJobExecutor.class) {
            try {
                Method initAdminBizList = XxlJobExecutor.class.getDeclaredMethod("initAdminBizList"
                        , String.class, String.class);
                initAdminBizList.setAccessible(true);

                String serviceAddressList = "";
                for (String item : adminAddressList) {
                    item = "http://" + item + (xxlJobPlusProperties.getAdmin().getContextPath().startsWith("/") ?
                            xxlJobPlusProperties.getAdmin().getContextPath() : "/" + xxlJobPlusProperties.getAdmin().getContextPath());
                    serviceAddressList = serviceAddressList + "," + item;
                }

                getAdminBizList().clear();
                initAdminBizList.invoke(this, serviceAddressList, xxlJobPlusProperties.getAdmin().getAccessToken());
                logger.info(">>>>>>>>>>> xxl-job-plus, XxlJobPlusSpringExecutor refresh adminBizList");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private ConcurrentMap<Integer, JobThread> getJobThreadRepository() {
        try {
            Field jobThreadRepository = XxlJobExecutor.class.getDeclaredField("jobThreadRepository");
            jobThreadRepository.setAccessible(true);
            return (ConcurrentMap<Integer, JobThread>) jobThreadRepository.get(this);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return new ConcurrentHashMap<Integer, JobThread>();
    }

    private ConcurrentMap<String, IJobHandler> getJobHandlerRepository() {
        try {
            Field jobHandlerRepository = XxlJobExecutor.class.getDeclaredField("jobHandlerRepository");
            jobHandlerRepository.setAccessible(true);
            return (ConcurrentMap<String, IJobHandler>) jobHandlerRepository.get(this);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return new ConcurrentHashMap<String, IJobHandler>();
    }
}
