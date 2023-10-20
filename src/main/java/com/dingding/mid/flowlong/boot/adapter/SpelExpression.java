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
package com.dingding.mid.flowlong.boot.adapter;

import com.dingding.mid.flowlong.Expression;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Map;
import java.util.Map.Entry;

/**
 * Spring el表达式解析器
 *
 * <p>
 * 尊重知识产权，CV 请保留版权，爱组搭 http://aizuda.com 出品，不允许非法使用，后果自负
 * </p>
 *
 * @author ximu
 * @since 1.0
 */
public class SpelExpression implements Expression {
    private final ExpressionParser parser;

    public SpelExpression() {
        parser = new SpelExpressionParser();
    }

    @Override
    public <T> T eval(Class<T> T, String expr, Map<String, Object> args) {
        EvaluationContext context = new StandardEvaluationContext();
        for (Entry<String, Object> entry : args.entrySet()) {
            context.setVariable(entry.getKey(), entry.getValue());
        }
        return parser.parseExpression(expr).getValue(context, T);
    }

}
