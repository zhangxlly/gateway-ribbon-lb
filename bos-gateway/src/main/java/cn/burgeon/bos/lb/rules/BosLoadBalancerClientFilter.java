package cn.burgeon.bos.lb.rules;

import org.apache.commons.lang.StringUtils;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.gateway.config.LoadBalancerProperties;
import org.springframework.cloud.gateway.filter.LoadBalancerClientFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient;
import org.springframework.http.HttpCookie;
import org.springframework.web.server.ServerWebExchange;

import java.net.URI;
import java.util.Objects;

public class BosLoadBalancerClientFilter extends LoadBalancerClientFilter {


    private static final String COOKIE = "SESSIONID";

    public BosLoadBalancerClientFilter(LoadBalancerClient loadBalancer, LoadBalancerProperties properties) {
        super(loadBalancer, properties);
    }

    @Override
    protected ServiceInstance choose(ServerWebExchange exchange) {
        // 获取请求中的cookie
        HttpCookie cookie = exchange.getRequest().getCookies().getFirst(COOKIE);
        if (cookie == null) {
            return super.choose(exchange);
        }
        String value = cookie.getValue();
        if (StringUtils.isEmpty(value)) {
            return super.choose(exchange);
        }
        if (this.loadBalancer instanceof RibbonLoadBalancerClient) {
            RibbonLoadBalancerClient client = (RibbonLoadBalancerClient) this.loadBalancer;
            Object attrValue = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
            Objects.requireNonNull(attrValue);
            String serviceId = ((URI) attrValue).getHost();
            // 这里使用session做为选择服务实例的key
            return client.choose(serviceId, value);
        }
        return super.choose(exchange);
    }
}
