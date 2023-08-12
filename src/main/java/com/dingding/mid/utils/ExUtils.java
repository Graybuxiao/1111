package com.dingding.mid.utils;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.dingding.mid.dto.json.UserInfo;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.impl.javax.el.ExpressionFactory;
import org.camunda.bpm.engine.impl.javax.el.ValueExpression;
import org.camunda.bpm.engine.impl.juel.ExpressionFactoryImpl;
import org.camunda.bpm.engine.impl.juel.SimpleContext;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.*;

/**
 * @author LoveMyOrange
 * @create 2022-10-16 22:13
 */
@Component
public class ExUtils {

    public Boolean strEqualsMethod(String controlId,String value){
        List<String> list = Arrays.asList(value);
            String s = list.get(0);
            return s.equals(controlId);
    }
    public Boolean strEqualsMethod(String controlId,String...values){
        List<String> list = Arrays.asList(values);
        if(list.size()>1){
            return Boolean.FALSE;
        }
        else{
            String s = list.get(0);
            return s.equals(controlId);
        }
    }

    public Boolean strContains(String controlId,String...values){
        List<String> list = Arrays.asList(values);
        return list.contains(controlId);
    }

    public Boolean strContains(String controlId,Number...values){
        Long aLong = Long.valueOf(controlId);
        List<Number> numbers = Arrays.asList(values);
        return numbers.contains(aLong);
    }

    public Boolean strContainsMethod(String controlId,String...values){
        List<String> strings = Arrays.asList(values);
        return strings.contains(controlId);
    }


    public Boolean userStrContainsMethod(String controlId,String...values){
        List<String> strings = Arrays.asList(values);
        List<UserInfo> userInfos = JSONObject.parseObject(controlId, new TypeReference<List<UserInfo>>() {
        });
        List<String> idsList= new ArrayList<>();
        for (UserInfo userInfo : userInfos) {
            idsList.add(userInfo.getId());
        }
        Collection<String> intersection = CollUtil.intersection(strings, idsList);
        if(CollUtil.isEmpty(intersection)){
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    public Boolean deptStrContainsMethod(String controlId,String...values){
        List<String> strings = Arrays.asList(values);
        List<UserInfo> userInfos = JSONObject.parseObject(controlId, new TypeReference<List<UserInfo>>() {
        });
        List<String> idsList= new ArrayList<>();
        for (UserInfo userInfo : userInfos) {
            idsList.add(userInfo.getId());
        }
        Collection<String> intersection = CollUtil.intersection(strings, idsList);
        if(CollUtil.isEmpty(intersection)){
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    public Boolean numberContains(Number controlId,Number...values){
        List<Number> list = Arrays.asList(values);
        return list.contains(controlId);
    }
    public Boolean b(String controlId,Number...values){
        List<Number> numbers = Arrays.asList(values);

        Double a = Double.valueOf(controlId);
        BigDecimal a1 = BigDecimal.valueOf(a);
        Double b = Double.valueOf(numbers.get(0).doubleValue());
        BigDecimal a2 = BigDecimal.valueOf(b);

        Double c = Double.valueOf(numbers.get(1).doubleValue());
        BigDecimal a3 = BigDecimal.valueOf(c);


        if( NumberUtil.isGreater(a1,a2)  && NumberUtil.isLess(a1,a3)  ){
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }



    public Boolean ab(String controlId,Number...values){
        List<Number> numbers = Arrays.asList(values);

        Double a = Double.valueOf(controlId);
        BigDecimal a1 = BigDecimal.valueOf(a);
        Double b = Double.valueOf(numbers.get(0).doubleValue());
        BigDecimal a2 = BigDecimal.valueOf(b);

        Double c = Double.valueOf(numbers.get(1).doubleValue());
        BigDecimal a3 = BigDecimal.valueOf(c);


        if(NumberUtil.isGreaterOrEqual(a1,a2)  &&NumberUtil.isLess(a1,a3)){
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }
    public Boolean ba(String controlId,Number...values){

        List<Number> numbers = Arrays.asList(values);

        Double a = Double.valueOf(controlId);
        BigDecimal a1 = BigDecimal.valueOf(a);
        Double b = Double.valueOf(numbers.get(0).doubleValue());
        BigDecimal a2 = BigDecimal.valueOf(b);

        Double c = Double.valueOf(numbers.get(1).doubleValue());
        BigDecimal a3 = BigDecimal.valueOf(c);


        if(NumberUtil.isGreater(a1,a2)  &&NumberUtil.isLessOrEqual(a1,a3)){
            return Boolean.TRUE;
        }
        return Boolean.FALSE;


    }
    public Boolean aba(String controlId,Number...values){
        List<Number> numbers = Arrays.asList(values);

        Double a = Double.valueOf(controlId);
        BigDecimal a1 = BigDecimal.valueOf(a);
        Double b = Double.valueOf(numbers.get(0).doubleValue());
        BigDecimal a2 = BigDecimal.valueOf(b);

        Double c = Double.valueOf(numbers.get(1).doubleValue());
        BigDecimal a3 = BigDecimal.valueOf(c);


        if(NumberUtil.isGreaterOrEqual(a1,a2)  &&NumberUtil.isLessOrEqual(a1,a3)){
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }


    /**
     conditionExpression.append(" "+ EXPRESSION_CLASS+"numberEquals("+id+","+str+") " );
     conditionExpression.append(" "+ EXPRESSION_CLASS+"numberGt("+id+","+str+") " );
     conditionExpression.append(" "+ EXPRESSION_CLASS+"numberGtEquals("+id+","+str+") " );
     conditionExpression.append(" "+ EXPRESSION_CLASS+"numberLt("+id+","+str+") " );
     conditionExpression.append(" "+ EXPRESSION_CLASS+"numberLtEquals("+id+","+str+") " );
     */
    public Boolean numberEquals(String controlId,String value){
        Double a = Double.valueOf(controlId);
        Double b = Double.valueOf(value);
        boolean equals = a.equals(b);
        return equals;
    }
    public Boolean numberGt(String controlId,String value){
        Double a = Double.valueOf(controlId);
        BigDecimal a1 = BigDecimal.valueOf(a);
        Double b = Double.valueOf(value);
        BigDecimal a2 = BigDecimal.valueOf(b);
        boolean greater = NumberUtil.isGreater(a1, a2);
        return greater;
    }

    public Boolean numberGtEquals(String controlId,String value){
        Double a = Double.valueOf(controlId);
        BigDecimal a1 = BigDecimal.valueOf(a);
        Double b = Double.valueOf(value);
        BigDecimal a2 = BigDecimal.valueOf(b);
        boolean greater = NumberUtil.isGreaterOrEqual(a1, a2);
        return greater;
    }

    public Boolean numberLt(String controlId,String value){
        Double a = Double.valueOf(controlId);
        BigDecimal a1 = BigDecimal.valueOf(a);
        Double b = Double.valueOf(value);
        BigDecimal a2 = BigDecimal.valueOf(b);
        boolean greater = NumberUtil.isLess(a1, a2);
        return greater;
    }
    public Boolean numberLtEquals(String controlId,String value){
        Double a = Double.valueOf(controlId);
        BigDecimal a1 = BigDecimal.valueOf(a);
        Double b = Double.valueOf(value);
        BigDecimal a2 = BigDecimal.valueOf(b);
        boolean greater = NumberUtil.isLessOrEqual(a1, a2);
        return greater;
    }
}
