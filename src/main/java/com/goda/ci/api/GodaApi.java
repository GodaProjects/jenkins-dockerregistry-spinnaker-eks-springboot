package com.goda.ci.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController("/")
public class GodaApi {

    @Value("${env.name:defaultGoda}")
    private String nameFromEnv;

    @GetMapping
    public Map<String, Object> getHelloGodaApi(){
        Map<String, Object> godaMap = new HashMap<>();
        godaMap.put("msg","Hello all (Another change 2.0.0)! from Goda!");
        godaMap.put("envProp",nameFromEnv);
        godaMap.put("anotherProp",nameFromEnv);
        return godaMap;
    }
}
