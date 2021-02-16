package org.example;

import org.example.server.boot.VertxBeans;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import static org.springframework.boot.Banner.Mode.OFF;

/**
 * Start up class for spring boot application.
 */
@SpringBootApplication
@Import(VertxBeans.class)
public class SimpleTCPApplication {

    protected SimpleTCPApplication() { }

    /**
     * Start up main method.
     *
     * @param args Any input args
     */
    public static void main(final String...args) {
        final SpringApplication application = new SpringApplication(SimpleTCPApplication.class);
        application.setBannerMode(OFF);
        application.run(args);
    }
}
