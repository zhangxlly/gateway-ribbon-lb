package cn.burgeon.bos.lb.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p></p >
 *
 * @author zxl
 * @email xiaoliang.zhang@payby.com
 * @date HelloWorldController.java v1.0  2020/7/22 5:18 PM
 */
@RestController
public class HelloWorldController {

    @GetMapping("/hello")
    public String hello() {
        return "hello spring cloud, 1002";
    }
}
