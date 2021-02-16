package org.example.server.boot;

import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBusOptions;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.core.spi.cluster.ClusterManager;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VertxBeansBase {

    @Autowired(required = false)
    private ClusterManager clusterManager;

    @Autowired(required = false)
    private EventBusOptions eventBusOptions;

    @Autowired(required = false)
    private MetricsOptions metricsOptions;

    @Autowired
    private Environment env;

    /**
     * Sets up all the vertx bean properties.
     *
     * @return An instance of {@link VertxOptions}.
     */
    @Bean
    protected VertxOptions vertxOptions() {
        final VertxOptions options = new VertxOptions();

        setParameter(env.getProperty("vertx.warning-exception-time", Long.class), options::setWarningExceptionTime);
        setParameter(env.getProperty("vertx.event-loop-pool-size", Integer.class), options::setEventLoopPoolSize);
        setParameter(env.getProperty("vertx.max-event-loop-execution-time", Long.class), options::setMaxEventLoopExecuteTime);
        setParameter(env.getProperty("vertx.worker-pool-size", Integer.class), options::setWorkerPoolSize);
        setParameter(env.getProperty("vertx.max-worker-execution-time", Long.class), options::setMaxWorkerExecuteTime);
        setParameter(env.getProperty("vertx.blocked-thread-check-interval", Long.class), options::setBlockedThreadCheckInterval);
        setParameter(env.getProperty("vertx.internal-blocking-pool-size", Integer.class), options::setInternalBlockingPoolSize);
        options.setHAEnabled(env.getProperty("vertx.ha-enabled", Boolean.class, false));
        setParameter(env.getProperty("vertx.ha-group", ""), options::setHAGroup);
        setParameter(env.getProperty("vertx.quorum-size", Integer.class), options::setQuorumSize);
        setParameter(clusterManager, options::setClusterManager);
        setParameter(metricsOptions, options::setMetricsOptions);
        setParameter(eventBusOptions, options::setEventBusOptions);

        return options;
    }

    private <T> void setParameter(final T param, final Consumer<T> setter) {
        if (param != null) {
            setter.accept(param);
        }
    }
}
