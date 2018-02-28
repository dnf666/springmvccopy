package com.ims.mvcframework.demo.action;

import com.ims.mvcframework.annotation.Auwired;
import com.ims.mvcframework.annotation.Controller;
import com.ims.mvcframework.annotation.RequestMapping;
import com.ims.mvcframework.annotation.RequestParam;
import com.ims.mvcframework.demo.service.DemoService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author demo
 */
@Controller
@RequestMapping("web")
public class DemoAction {
    @Auwired()
     DemoService demoService;
    @RequestMapping("query.json")
    public void query(HttpServletRequest request, HttpServletResponse response ,@RequestParam("name") String name){
        String result = demoService.get(name);
        try {
            response.getWriter().write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
