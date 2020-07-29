package cn.burgeon.bos.lb.rules;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.AbstractLoadBalancerRule;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
/**
 * @author bzlwo
 */
public class BosKeepSessionByCookieRule extends AbstractLoadBalancerRule {
     private static String COOKIE_KEY="BOSSERVERID";
    @Override
    public Server choose(Object key) {
        ILoadBalancer loadBalancer = getLoadBalancer();
        List<Server> allServers = loadBalancer.getAllServers();
        if(allServers.size()==1){
            return allServers.get(0);
        }

        int size = allServers.size();
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();

        String remoteAddr = request.getRemoteAddr();
        // TODO hashcode会有负数导致索引越界
        int hashCode = Math.abs(remoteAddr.hashCode());
        int index=hashCode%size;
        Server server = allServers.get(index);
        if(server==null || !server.isAlive() || server.isReadyToServe()){
            if(loadBalancer.getReachableServers().size()>0){
                return server;
            }
        }
        return null;
    }

    private int getServerIndex(HttpServletRequest request){
       return 0;
    }

    @Override
    public void initWithNiwsConfig(IClientConfig clientConfig) {

    }
}
