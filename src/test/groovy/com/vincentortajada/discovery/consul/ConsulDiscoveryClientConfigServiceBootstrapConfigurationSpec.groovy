package com.vincentortajada.discovery.consul

import com.ecwid.consul.v1.QueryParams
import com.ecwid.consul.v1.Response
import com.ecwid.consul.v1.catalog.model.CatalogService
import org.springframework.cloud.config.client.ConfigClientProperties
import spock.lang.Specification

/**
 * @author vincentortajada
 */
class ConsulDiscoveryClientConfigServiceBootstrapConfigurationSpec extends Specification {

    def "should create service url"(){
        setup:
            def ConfigClientProperties config = new ConfigClientProperties()
            config.getDiscovery().setServiceId("serviceName")
            CatalogService catalogService = new CatalogService()
            catalogService.serviceAddress = "10.0.0.1"
            catalogService.servicePort = 8080
            def List<CatalogService> list = Arrays.asList(catalogService)
            def Response<List<CatalogService>> response = new Response<List<CatalogService>>(list, 0, true, 0)
            def ConsulDiscoveryClientConfigServiceBootstrapConfiguration.ConsulClientAdapter consulClientMock =
                    Mock(ConsulDiscoveryClientConfigServiceBootstrapConfiguration.ConsulClientAdapter)

            def ConsulDiscoveryClientConfigServiceBootstrapConfiguration service =
                    new ConsulDiscoveryClientConfigServiceBootstrapConfiguration()
            service.@consulClientAdapter = consulClientMock
            service.@config = config
            service.@protocol = "http"
            service.@datacenter = ""
        when:
            service.refresh()
        then:
            1*consulClientMock.getCatalogService("serviceName", QueryParams.DEFAULT) >> response
            assert config.getUri() == "http://10.0.0.1:8080/"
    }

    def "should return an Exception when returned service is empty"(){
        setup:
            def ConfigClientProperties config = new ConfigClientProperties()
            config.getDiscovery().setServiceId("serviceName")
            CatalogService catalogService = new CatalogService()
            catalogService.serviceAddress = "10.0.0.1"
            catalogService.servicePort = 8080
            def List<CatalogService> list = []
            def Response<List<CatalogService>> response = new Response<List<CatalogService>>(list, 0, true, 0)
            def ConsulDiscoveryClientConfigServiceBootstrapConfiguration.ConsulClientAdapter consulClientMock =
                    Mock(ConsulDiscoveryClientConfigServiceBootstrapConfiguration.ConsulClientAdapter)

            def ConsulDiscoveryClientConfigServiceBootstrapConfiguration service =
                    new ConsulDiscoveryClientConfigServiceBootstrapConfiguration()
            service.@consulClientAdapter = consulClientMock
            service.@config = config
            service.@protocol = "http"
            service.@datacenter = ""
        when:
            service.refresh()
        then:
            1*consulClientMock.getCatalogService("serviceName", QueryParams.DEFAULT) >> response
            assert config.getUri() == "http://localhost:8888"
    }
}
