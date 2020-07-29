package cn.burgeon.bos.lb.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p></p >
 *
 * @author zxl
 * @email xiaoliang.zhang@payby.com
 * @date HealthController.java v1.0  2020/7/23 10:31 AM
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public String health() throws InterruptedException {
        System.out.println("1001 in");
        //Thread.sleep(10000);
        System.out.println("1001 out");

        return "ok";
    }
}
