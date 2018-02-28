package com.ims.mvcframework.demo.service;

import com.ims.mvcframework.annotation.Service;

/**
 * @author demo
 */
@Service
public class DemoService {
    public String get(String name) {
        System.out.println(name);
        return name;
    }
}
