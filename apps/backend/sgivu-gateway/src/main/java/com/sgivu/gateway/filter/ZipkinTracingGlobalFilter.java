package com.sgivu.gateway.filter;

import brave.Span;
import brave.Tracer;
import com.sgivu.gateway.exception.TracingException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

/**
 * Filtro global para Spring Cloud Gateway que crea y finaliza spans de Zipkin (Brave) para las
 * peticiones entrantes. Añade la cabecera de trazado `X-Trace-Id`, registra información básica de
 * entrada y salida, y etiqueta los spans con información HTTP (código, texto) y duración. También
 * etiqueta los errores producidos durante el procesamiento de la petición.
 */
@Component
public class ZipkinTracingGlobalFilter implements GlobalFilter, Ordered {

  private static final Logger logger = LoggerFactory.getLogger(ZipkinTracingGlobalFilter.class);
  private static final String ATTR_SPAN = "ZipkinTracingGlobalFilter.span";
  private static final String ATTR_START = "ZipkinTracingGlobalFilter.startTime";
  private static final String TRACE_HEADER = "X-Trace-Id";

  private final Tracer tracer;

  private static final String UNKNOWN = "unknown";

  public ZipkinTracingGlobalFilter(Tracer tracer) {
    this.tracer = Objects.requireNonNull(tracer, "Tracer must not be null");
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    final String path = safe(() -> exchange.getRequest().getPath().value(), "unknown-path");
    final String method = safe(() -> exchange.getRequest().getMethod().name(), "UNKNOWN");
    final String host = safe(() -> exchange.getRequest().getURI().getHost(), "unknown-host");

    if (shouldSkip(path)) {
      return chain.filter(exchange);
    }

    final Span span = createSpan(method, path, host);
    exchange.getAttributes().put(ATTR_SPAN, span);
    exchange.getAttributes().put(ATTR_START, System.nanoTime());

    return chain
        .filter(exchange)
        .doFirst(() -> startSpan(span, exchange, method, path))
        .doOnError(throwable -> handleError(span, throwable))
        .doFinally(signal -> finalizeSpan(span, exchange, method, path, signal));
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }

  private boolean shouldSkip(String path) {
    if (path == null) return false;
    return path.contains("/actuator/health")
        || path.contains("/actuator/info")
        || path.contains("/actuator/prometheus");
  }

  private Span createSpan(String method, String path, String host) {
    Span s = tracer.nextSpan().name("gateway-" + method);
    safeTag(s, "path", path);
    safeTag(s, "method", method);
    safeTag(s, "host", host);
    return s;
  }

  private void startSpan(Span span, ServerWebExchange exchange, String method, String path) {
    try {
      span.start();
    } catch (Exception e) {
      throw new TracingException("Error starting span", e);
    }

    try {
      String traceId = (span.context() != null) ? span.context().traceIdString() : "no-trace";
      exchange.getResponse().getHeaders().add(TRACE_HEADER, traceId);
      MDC.put("traceId", traceId);
      logger.info("Entering gateway request (traceId={}): {} {}", traceId, method, path);
    } catch (Exception e) {
      throw new TracingException("Error setting traceId header or MDC", e);
    }
  }

  private void handleError(Span span, Throwable throwable) {
    if (span == null) return;
    try {
      String errorMsg =
          throwable == null
              ? UNKNOWN
              : (throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
      span.tag("error", errorMsg);
      span.error(throwable);
    } catch (Exception e) {
      logger.debug("Could not tag span with error", e);
    }
  }

  private void finalizeSpan(
      Span span, ServerWebExchange exchange, String method, String path, SignalType signalType) {
    if (span == null) return;

    try {
      HttpStatusCode statusCode = exchange.getResponse().getStatusCode();
      int statusValue = (statusCode != null) ? statusCode.value() : -1;
      String statusText = UNKNOWN;
      if (statusCode != null) {
        HttpStatus resolved = HttpStatus.resolve(statusCode.value());
        statusText = (resolved != null) ? resolved.getReasonPhrase() : "N/A";
      }

      safeTag(span, "http.status_code", String.valueOf(statusValue == -1 ? UNKNOWN : statusValue));
      safeTag(span, "http.status_text", statusText);

      Long start = (Long) exchange.getAttributes().get(ATTR_START);
      if (start != null) {
        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        safeTag(span, "duration_ms", String.valueOf(durationMs));
      }

      String traceId = span.context() != null ? span.context().traceIdString() : null;
      logger.info(
          "Leaving gateway request (traceId={}): {} {} -> status={} (signal={})",
          traceId,
          method,
          path,
          (statusValue == -1 ? UNKNOWN : statusValue),
          signalType);

    } catch (Exception e) {
      logger.warn("Error finalizing span", e);
    } finally {
      try {
        span.finish();
      } catch (Exception e) {
        logger.debug("Could not finalize span", e);
      }
      MDC.remove("traceId");
    }
  }

  private static void safeTag(Span span, String key, String value) {
    if (span == null || key == null || value == null) return;
    try {
      span.tag(key, value);
    } catch (Exception e) {
      throw new TracingException("Error tagging span " + key + "=" + value, e);
    }
  }

  private static <T> T safe(SupplierThrows<T> supplier, T fallback) {
    try {
      T v = supplier.get();
      return v == null ? fallback : v;
    } catch (TracingException e) {
      LoggerFactory.getLogger(ZipkinTracingGlobalFilter.class)
          .debug("Safe execution failed, returning default value={}", fallback, e);
      return fallback;
    }
  }

  @FunctionalInterface
  private interface SupplierThrows<T> {
    T get() throws TracingException;
  }
}
