package org.example.server.boot;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.vertx.core.VertxOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.eventbus.EventBus;
import io.vertx.reactivex.core.file.FileSystem;
import io.vertx.reactivex.core.shareddata.SharedData;

/**
 * Sets up all vertx instances as spring beans.
 */
@Configuration
public class VertxBeans extends VertxBeansBase {

    /**
     * Sets up vertx bean.
     *
     * @param options VertxOptions
     * @return Vertx
     */
    @Bean
    public Vertx vertx(final VertxOptions options) {
        return Vertx.vertx(options);
    }

    /**
     * Sets up event bus bean.
     *
     * @param vertx Vertx
     * @return EventBus
     */
    @Bean
    public EventBus eventBus(final Vertx vertx) {
        return vertx.eventBus();
    }

    /**
     * Sets up file system bean.
     *
     * @param vertx Vertx
     * @return FileSystem
     */
    @Bean
    public FileSystem fileSystem(final Vertx vertx) {
        return vertx.fileSystem();
    }

    /**
     * Sets up SharedData bean.
     *
     * @param vertx Vertx
     * @return SharedData
     */
    @Bean
    public SharedData sharedData(final Vertx vertx) {
        return vertx.sharedData();
    }

    /**
     * Sets up context runner.
     *
     * @param vertx Vertx
     * @return ContextRunnerRx
     */
    @Bean
    public ContextRunnerRx contextRunner(final Vertx vertx) {
        return new ContextRunnerRxImpl(new ContextRunnerImpl((io.vertx.core.Vertx)vertx.getDelegate()));
    }
}
