package com.sarat.dsavisualizer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the DSA Visualizer backend.
 * Serves the step-trace REST API the React frontend plays back.
 */
@SpringBootApplication
public class DsavisualizerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DsavisualizerApplication.class, args);
    }
}
