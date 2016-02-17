package com.vincentortajada.discovery.consul;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.catalog.model.CatalogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.cloud.consul.ConsulAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.config.client.ConfigServicePropertySourceLocator;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import lombok.extern.apachecommons.CommonsLog;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * Bootstrap configuration for a config client that wants to lookup the config server via
 * discovery when using Consul.io.
 * @author vincentortajada
 */
@ConditionalOnClass({ ConsulClient.class, ConfigServicePropertySourceLocator.class })
@ConditionalOnProperty(value = "spring.cloud.config.discovery.enabled", matchIfMissing = false)
@Configuration
@Import(ConsulAutoConfiguration.class)
@CommonsLog
public class ConsulDiscoveryClientConfigServiceBootstrapConfiguration {

    @Value("${spring.cloud.consul.protocol:http}")
    private String protocol;

    @Autowired
    private ConsulClient consulClient;

    @Autowired
    private ConfigClientProperties config;

    @Value("${spring.cloud.consul.datacenter:}")
    private String datacenter;

    @EventListener(ContextRefreshedEvent.class)
    public void onApplicationEvent(ContextRefreshedEvent event) {
        refresh();
    }

    private ConsulClientAdapter consulClientAdapter;

    @PostConstruct
    public void init(){
        consulClientAdapter = new ConsulClientAdapter(consulClient);
    }

    protected void refresh() {
        try {
            log.debug("Locating configserver via discovery");
            QueryParams queryParams = (datacenter.isEmpty())? QueryParams.DEFAULT:new QueryParams(datacenter);
            Response<List<CatalogService>> catalogServices = this.consulClientAdapter.getCatalogService(this.config.getDiscovery().getServiceId(), queryParams);
            String url = getHomePage(catalogServices);
            this.config.setUri(url);
        }
        catch (Exception ex) {
            log.warn("Could not locate configserver via discovery", ex);
        }
    }

    private String getHomePage(Response<List<CatalogService>> catalogServices) throws Exception {
        List<CatalogService> services = catalogServices.getValue();
        for(CatalogService service: services){
            return protocol+"://"+service.getServiceAddress()+":"+service.getServicePort()+"/";
        }
        throw new Exception("no services match the config server serviceName");
    }

    /**
     * For unit test mocking since Consulclient is final
     */
    protected static class ConsulClientAdapter{

        private ConsulClient consulClient;

        public ConsulClientAdapter(ConsulClient consulClient){
            this.consulClient = consulClient;
        }

        public Response<List<CatalogService>> getCatalogService(String serviceName, QueryParams queryParams) {
            return consulClient.getCatalogService(serviceName, queryParams);
        }
    }
}
