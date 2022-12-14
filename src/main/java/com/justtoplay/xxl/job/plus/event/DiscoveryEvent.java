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

package com.justtoplay.xxl.job.plus.event;

import org.springframework.context.ApplicationEvent;

/**
 * 服务发现事件
 *
 * @author justtoplay
 * @since 2022/06/26 20:11
 */
public class DiscoveryEvent extends ApplicationEvent {
    public DiscoveryEvent(Object source) {
        super(source);
    }
}
