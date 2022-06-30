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

package com.justtoplay.xxl.job.plus.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * context工具
 *
 * @author justtoplay
 * @since 2022/06/26 22:58
 */
@Component
public class SpringContextUtil implements ApplicationContextAware {

    /**
     * Spring容器上下文
     */
    private static final Map<String, ApplicationContext> CONTEXT_HOLDER = new HashMap<>(1);

    /**
     * 获取Spring容器上下文
     *
     * @return 上下文
     */
    public static ApplicationContext getApplicationContext() {
        return CONTEXT_HOLDER.get(ApplicationContext.class.getName());
    }

    /**
     * 注入Spring容器上下文
     *
     * @param applicationContext 上下文
     * @throws BeansException bean异常
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        CONTEXT_HOLDER.put(ApplicationContext.class.getName(), applicationContext);
    }
}
