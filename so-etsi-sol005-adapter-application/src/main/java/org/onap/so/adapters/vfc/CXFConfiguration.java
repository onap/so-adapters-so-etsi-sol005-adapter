/*-
 * ============LICENSE_START=======================================================
 * ONAP - SO
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.onap.so.adapters.vfc;

import java.util.Arrays;
import java.util.HashSet;
import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.LoggingFeature;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.openapi.OpenApiFeature;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.onap.so.adapters.vfc.rest.VfcAdapterRest;
import org.onap.so.logging.jaxrs.filter.SOAuditLogContainerFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;


@Configuration
public class CXFConfiguration {

    @Autowired
    private Bus bus;

    @Autowired
    private VfcAdapterRest vfcAdapterRest;

    @Autowired
    private SOAuditLogContainerFilter soAuditLogContainerFilter;

    @Autowired
    private ObjectMapper mapper;

    @Bean
    public ServletRegistrationBean cxfServlet() {
        return new ServletRegistrationBean(new CXFServlet(), "/services/*");
    }

    @Bean
    public Server rsServer() {
        JAXRSServerFactoryBean endpoint = new JAXRSServerFactoryBean();
        endpoint.setBus(bus);
        endpoint.setServiceBeans(Arrays.<Object>asList(vfcAdapterRest));
        endpoint.setAddress("/");
        endpoint.setFeatures(Arrays.asList(createSwaggerFeature(), new LoggingFeature()));
        endpoint.setProviders(Arrays.asList(new JacksonJsonProvider(mapper), soAuditLogContainerFilter));
        return endpoint.create();
    }

    @Bean
    public OpenApiFeature createSwaggerFeature() {
        OpenApiFeature openApiFeature = new OpenApiFeature();
        openApiFeature.setPrettyPrint(true);
        openApiFeature.setTitle("SO VFC Adapter");
        openApiFeature.setContactName("The ONAP SO team");
        openApiFeature.setDescription("This project is the SO Orchestration Engine");
        openApiFeature.setVersion("1.0.0");
        openApiFeature.setResourcePackages(new HashSet<String>(Arrays.asList("org.onap.so.adapters.vfc.rest")));
        openApiFeature.setScan(true);
        return openApiFeature;
    }

}
