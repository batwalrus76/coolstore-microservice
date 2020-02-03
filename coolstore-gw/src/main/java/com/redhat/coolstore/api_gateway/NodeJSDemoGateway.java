/**
 * JBoss, Home of Professional Open Source
 * Copyright 2016, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.redhat.coolstore.api_gateway;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.Response;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.component.jackson.ListJacksonDataFormat;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.redhat.coolstore.api_gateway.model.PageCount;

@Component
public class NodeJSDemoGateway extends RouteBuilder {
	private static final Logger LOG = LoggerFactory.getLogger(ProductGateway.class);
	
	
	@Value("${hystrix.executionTimeout}")
	private int hystrixExecutionTimeout;
	
	@Value("${hystrix.groupKey}")
	private String hystrixGroupKey;
	
	@Value("${hystrix.circuitBreakerEnabled}")
	private boolean hystrixCircuitBreakerEnabled;
	
	@Autowired
	private Environment env;
	
    @Override
    public void configure() throws Exception {
    	try {
    		getContext().setTracing(Boolean.parseBoolean(env.getProperty("ENABLE_TRACER", "false")));	
		} catch (Exception e) {
			LOG.error("Failed to parse the ENABLE_TRACER value: {}", env.getProperty("ENABLE_TRACER", "false"));
		}
    	
        
        JacksonDataFormat pageCountFormatter = new ListJacksonDataFormat();
	    pageCountFormatter.setUnmarshalType(PageCount.class);

       

        rest("/pageCount/").description("PageCount Service")
            .produces(MediaType.APPLICATION_JSON_VALUE)

            
            
        // Handle CORS Pre-flight requests
        .options("/")
            .route().id("pageCountOptions").end()
        .endRest()

        .get("/").description("Get Page Count").outType(PageCount.class)
            .route().id("pageCountRoute")
                .hystrix().id("Page Count Service")
                	.hystrixConfiguration()
		    			.executionTimeoutInMilliseconds(hystrixExecutionTimeout)
		    			.groupKey(hystrixGroupKey)
		    			.circuitBreakerEnabled(hystrixCircuitBreakerEnabled)
		    		.end()
                	.setBody(simple("null"))
                	.removeHeaders("CamelHttp*")
                	.recipientList(simple("http4://{{env:PAGE_COUNT_ENDPOINT:catalog:8080}}/pageCount")).end()
                .onFallback()
                	.setHeader(Exchange.HTTP_RESPONSE_CODE, constant(Response.Status.SERVICE_UNAVAILABLE.getStatusCode()))
                    .to("direct:pageCountFallback")
                    .stop()
                .end()
                .choice()
                	.when(body().isNull())
                		.to("direct:pageCountFallback")
                	.end()
                	.unmarshal(productFormatter)
	            .end()
	            
        .endRest();
        
        

        from("direct:pageCountFallback")
                .id("PageCountFallbackRoute")
                .transform()
                .constant(Collections.singletonList(new PageCount(5)))
                .marshal().json(JsonLibrary.Jackson, PageCount.class);
    }

}
