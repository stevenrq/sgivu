package com.sgivu.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import brave.Span;
import brave.Tracer;
import brave.propagation.TraceContext;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ZipkinTracingGlobalFilterTests {

  @Test
  void shouldAddTraceHeaderAndFinishSpanOnSuccess() {
    Tracer tracer = mock(Tracer.class);
    Span span = mockSpan();
    when(tracer.nextSpan()).thenReturn(span);
    ZipkinTracingGlobalFilter filter = new ZipkinTracingGlobalFilter(tracer);

    MockServerHttpRequest request = MockServerHttpRequest.get("http://localhost/v1/users").build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);

    GatewayFilterChain chain =
        ex -> {
          ex.getResponse().setStatusCode(HttpStatus.OK);
          return Mono.empty();
        };

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

    assertThat(exchange.getResponse().getHeaders().getFirst("X-Trace-Id")).isEqualTo("abc123");
    verify(tracer).nextSpan();
    verify(span).start();
    verify(span).finish();
    verify(span, atLeastOnce()).tag(anyString(), anyString());
  }

  @Test
  void shouldSkipActuatorEndpoints() {
    Tracer tracer = mock(Tracer.class);
    ZipkinTracingGlobalFilter filter = new ZipkinTracingGlobalFilter(tracer);

    MockServerHttpRequest request =
        MockServerHttpRequest.get("http://localhost/actuator/health").build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);
    GatewayFilterChain chain = ex -> Mono.empty();

    StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

    assertThat(exchange.getResponse().getHeaders().containsKey("X-Trace-Id")).isFalse();
    verifyNoInteractions(tracer);
  }

  @Test
  void shouldTagErrorWhenChainFails() {
    Tracer tracer = mock(Tracer.class);
    Span span = mockSpan();
    when(tracer.nextSpan()).thenReturn(span);
    ZipkinTracingGlobalFilter filter = new ZipkinTracingGlobalFilter(tracer);

    MockServerHttpRequest request = MockServerHttpRequest.get("http://localhost/v1/users").build();
    MockServerWebExchange exchange = MockServerWebExchange.from(request);
    RuntimeException boom = new RuntimeException("boom");

    StepVerifier.create(filter.filter(exchange, ex -> Mono.error(boom)))
        .expectErrorSatisfies(th -> assertThat(th).isSameAs(boom))
        .verify();

    assertThat(exchange.getResponse().getHeaders().getFirst("X-Trace-Id")).isEqualTo("abc123");
    verify(span).error(boom);
    verify(span).tag(eq("error"), contains("boom"));
    verify(span).finish();
  }

  private Span mockSpan() {
    Span span = mock(Span.class);
    TraceContext traceContext = mock(TraceContext.class);
    when(traceContext.traceIdString()).thenReturn("abc123");
    when(span.context()).thenReturn(traceContext);
    when(span.name(anyString())).thenReturn(span);
    when(span.tag(anyString(), anyString())).thenReturn(span);
    when(span.error(any())).thenReturn(span);
    when(span.start()).thenReturn(span);
    return span;
  }
}
