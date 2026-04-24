package com.research.workbench.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;

@Service
public class RelaxFeatureService {

    public Map<String, Object> bazi(String profile) {
        return Map.of(
                "result", "娱乐模式解读：你的气场偏“执行型研究者”，适合先定问题再开搜，少开新坑，多做闭环。\n输入信息：" + profile
        );
    }

    public Map<String, Object> whatToEat(String preference) {
        List<String> options = List.of("番茄牛腩饭", "麻辣香锅", "寿司便当", "鸡胸肉沙拉", "黄焖鸡米饭", "牛肉面");
        String choice = options.get(ThreadLocalRandom.current().nextInt(options.size()));
        return Map.of("result", "今天吃这个：" + choice + "。偏好参考：" + (preference == null ? "无" : preference));
    }
}
