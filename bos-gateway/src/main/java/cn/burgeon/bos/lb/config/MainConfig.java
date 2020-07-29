package cn.burgeon.bos.lb.config;

import com.netflix.loadbalancer.IPing;
import com.netflix.loadbalancer.PingUrl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * <p></p >
 *
 * @author zxl
 * @email xiaoliang.zhang@payby.com
 * @date MainConfig.java v1.0  2020/7/23 10:32 AM
 */
@Configuration
public class MainConfig {

//    @Value("${load-balanced-helth-ssl:false}")
//    private boolean ssl;
//
//    @Value("${load-balanced-helth-check.uri:/}")
//    private String healthUri;
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    //@Bean
//    public IPing pingUrl(){
//        return new PingUrl(ssl,healthUri);
//    }
}
