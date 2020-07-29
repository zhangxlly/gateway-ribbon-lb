package cn.burgeon.bos.lb.components;

import com.netflix.loadbalancer.IPing;
import com.netflix.loadbalancer.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Set;

/**
 * <p></p >
 *
 * @author zxl
 * @email xiaoliang.zhang@payby.com
 * @date HealthExamination.java v1.0  2020/7/23 10:45 AM
 */
@Component
@Slf4j
public class HealthExamination implements IPing{

    @Autowired
    private RestTemplate restTemplate;

    @Value("${load-balanced-helth-ssl:false}")
    private boolean ssl;
    @Value("${load-balanced-helth-check.uri:/}")
    private String healthUri;

    private String healthPrefix;

    private Set<String> failedService= new HashSet<String>();
    @PostConstruct
    private void init(){
        this.healthPrefix = ssl?"https://":"http://";
    }
    @Override
    public boolean isAlive(Server server) {
        try {
            ResponseEntity<String> health = restTemplate.getForEntity(healthPrefix+server.getId()+healthUri, String.class);
            if (health.getStatusCode() == HttpStatus.OK) {
                if(failedService.remove(server.getId())){
                    //失败后恢复加一个log
                    log.info("ping server"+server.toString()+" success.");
                }
                //System.out.println("ping server"+server.toString()+" success");
                return true;
            }
            return false;
        } catch (Exception e) {
            //失败的时候加到失败列表里面
            failedService.add(server.getId());
            log.info("ping server"+server.toString()+" failed."+e.getMessage());
            //System.out.println("ping server"+server.toString()+" failed");
            return false;
        }
    }
}
