/* Copyright 2023-2025 jobob@qq.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dingding.mid.flowlong.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 流程缓存简单实现类
 *
 * <p>
 * 尊重知识产权，CV 请保留版权，爱组搭 http://aizuda.com 出品，不允许非法使用，后果自负
 * </p>
 *
 * @author hubin
 * @since 1.0
 */
public class FlowSimpleCache implements FlowCache {
    private final Map<String, Object> localCache = new ConcurrentHashMap<>();

    @Override
    public void put(String key, Object value) {
        localCache.put(key, value);
    }

    @Override
    public <T> T get(String key) {
        return (T) localCache.get(key);
    }

    @Override
    public void remove(String key) {
        localCache.remove(key);
    }
}
