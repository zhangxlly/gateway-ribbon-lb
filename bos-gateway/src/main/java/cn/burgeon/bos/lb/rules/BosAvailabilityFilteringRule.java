package cn.burgeon.bos.lb.rules;

import com.netflix.loadbalancer.AvailabilityFilteringRule;
import com.netflix.loadbalancer.Server;

import java.util.List;

/**
 * @author bzlwo
 */
public class BosAvailabilityFilteringRule extends AvailabilityFilteringRule {

    @Override
    public Server choose(Object key) {

        Server chosenServer = super.choose(key);

        int count = 1;
        List<Server> reachableServers = this.getLoadBalancer().getReachableServers();
        List<Server> allServers = this.getLoadBalancer().getAllServers();

        if (reachableServers.size() > 0) {
            while (!reachableServers.contains(chosenServer) && count++ < allServers.size()) {
                chosenServer = reachableServers.get(0);
            }
        }
        return chosenServer;
    }
}
