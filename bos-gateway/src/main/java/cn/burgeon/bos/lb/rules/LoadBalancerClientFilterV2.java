package cn.burgeon.bos.lb.rules;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.gateway.config.LoadBalancerProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.LoadBalancerClientFilter;
import org.springframework.cloud.gateway.support.DelegatingServiceInstance;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.TomcatHttpHandlerAdapter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * <p></p >
 *
 * @author zxl
 * @email xiaoliang.zhang@payby.com
 * @date BosLoadBalancerClientFilterV2.java v1.0  2020/7/29 10:27 AM
 */
@Deprecated
@Component
public class LoadBalancerClientFilterV2 implements GlobalFilter, Ordered {

    public static final int LOAD_BALANCER_CLIENT_FILTER_ORDER = 10100;
    private static final Log log = LogFactory.getLog(LoadBalancerClientFilter.class);
    protected final LoadBalancerClient loadBalancer;
    private LoadBalancerProperties properties;

    public LoadBalancerClientFilterV2(LoadBalancerClient loadBalancer, LoadBalancerProperties properties) {
        this.loadBalancer = loadBalancer;
        this.properties = properties;
    }

    @Override
    public int getOrder() {
        return 10100;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServletRequestAttributes requestAttributes = new ServletRequestAttributes(((TomcatHttpHandlerAdapter.TomcatServerHttpRequest)exchange.getRequest()).getRequest());
        RequestContextHolder.setRequestAttributes(requestAttributes);

        URI url = (URI)exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
        String schemePrefix = (String)exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_SCHEME_PREFIX_ATTR);
        if (url != null && ("lb".equals(url.getScheme()) || "lb".equals(schemePrefix))) {
            ServerWebExchangeUtils.addOriginalRequestUrl(exchange, url);
            if (log.isTraceEnabled()) {
                log.trace("LoadBalancerClientFilter url before: " + url);
            }

            ServiceInstance instance = this.choose(exchange);
            if (instance == null) {
                throw NotFoundException.create(this.properties.isUse404(), "Unable to find instance for " + url.getHost());
            } else {
                URI uri = exchange.getRequest().getURI();
                String overrideScheme = instance.isSecure() ? "https" : "http";
                if (schemePrefix != null) {
                    overrideScheme = url.getScheme();
                }

                URI requestUrl = this.loadBalancer.reconstructURI(new DelegatingServiceInstance(instance, overrideScheme), uri);
                if (log.isTraceEnabled()) {
                    log.trace("LoadBalancerClientFilter url chosen: " + requestUrl);
                }

                exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, requestUrl);
                return chain.filter(exchange);
            }
        } else {
            return chain.filter(exchange);
        }
    }

    protected ServiceInstance choose(ServerWebExchange exchange) {
        return this.loadBalancer.choose(((URI)exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR)).getHost());
    }
}
