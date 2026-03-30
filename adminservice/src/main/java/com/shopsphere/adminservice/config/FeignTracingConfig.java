package com.shopsphere.adminservice.config;

import feign.RequestInterceptor;
import io.micrometer.tracing.propagation.Propagator;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class FeignTracingConfig {

    private final Tracer tracer;
    private final Propagator propagator;

    /*
     * This interceptor forwards the current trace context to Feign downstream calls.
     * It helps Zipkin keep adminservice and orderservice spans in one linked trace.
     */
    @Bean
    public RequestInterceptor tracingRequestInterceptor() {
        return requestTemplate -> {
            Span currentSpan = tracer.currentSpan();
            if (currentSpan == null) {
                return;
            }
            propagator.inject(currentSpan.context(), requestTemplate,
                    (carrier, key, value) -> carrier.header(key, value));
        };
    }
}
