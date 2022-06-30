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

package com.justtoplay.xxl.job.plus.controller;

import com.xxl.job.core.biz.ExecutorBiz;
import com.xxl.job.core.biz.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 执行器接受命令控制器，代替netty
 *
 * @author justtoplay
 * @since 2022/06/26 13:32
 */
@RestController
public class ExecutorController {
    @Autowired
    private ExecutorBiz executorBiz;

    @PostMapping("/beat")
    public ReturnT<String> beat() {
        return executorBiz.beat();
    }

    @PostMapping("/idleBeat")
    public ReturnT<String> idleBeat(@RequestBody IdleBeatParam idleBeatParam) {
        return executorBiz.idleBeat(idleBeatParam);
    }

    @PostMapping("/run")
    public ReturnT<String> run(@RequestBody TriggerParam triggerParam) {
        return executorBiz.run(triggerParam);
    }

    @PostMapping("/kill")
    public ReturnT<String> kill(@RequestBody KillParam killParam) {
        return executorBiz.kill(killParam);
    }

    @PostMapping("/log")
    public ReturnT<LogResult> log(@RequestBody LogParam logParam) {
        return executorBiz.log(logParam);
    }
}
