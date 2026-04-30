package com.shopsphere.orderservice.config;

import feign.RequestInterceptor;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
/*
 * What:
 * Propagates the current Micrometer trace context into outgoing Feign requests.
 *
 * Why:
 * Without this, Feign calls from orderservice to catalogservice and paymentservice
 * start a new trace instead of continuing the existing one. Zipkin would show them
 * as separate unlinked spans rather than one end-to-end trace, making it impossible
 * to follow a request across service boundaries.
 *
 * How:
 * A Feign RequestInterceptor runs before every outgoing HTTP call.
 * It reads the current active span from the Tracer, then uses the Propagator
 * to inject the trace/span IDs as HTTP headers (e.g. X-B3-TraceId, X-B3-SpanId).
 * The downstream service picks up those headers and continues the same trace.
 */
public class FeignTracingConfig {

    private final Tracer tracer;
    private final Propagator propagator;

    /*
     * What:
     * Feign interceptor that injects trace context headers into every outgoing request.
     *
     * Why:
     * Keeps orderservice → catalogservice and orderservice → paymentservice calls
     * linked under the same Zipkin trace as the original inbound request.
     *
     * How:
     * 1) Read the current span from the Tracer.
     * 2) If no active span exists, skip injection (nothing to propagate).
     * 3) Use Propagator.inject() to write trace headers onto the Feign RequestTemplate.
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
