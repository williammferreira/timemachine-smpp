package com.timemachine.smppclient;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudhopper.smpp.SmppServerConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppServer;
import com.cloudhopper.smpp.type.SmppChannelException;

/**
 * @author William Ferreira (@link http://www.github.com/williammferreira)
 */
public class ServerMain {
    public static final Logger logger = LoggerFactory.getLogger(ServerMain.class);

    private static final int PORT = 2775;

    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();

    ScheduledThreadPoolExecutor monitorExecutor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);

    DefaultSmppServer server = new DefaultSmppServer(serverConfiguration(), new ServerHandler(), executor,
            monitorExecutor);

    /**
     * Set the server configuration.
     * 
     * @return The server configuration.
     */
    public SmppServerConfiguration serverConfiguration() {
        SmppServerConfiguration configuration = new SmppServerConfiguration();

        configuration.setPort(PORT);
        configuration.setMaxConnectionSize(10);
        configuration.setNonBlockingSocketsEnabled(true);
        configuration.setDefaultRequestExpiryTimeout(30000);
        configuration.setDefaultWindowMonitorInterval(15000);
        configuration.setDefaultWindowSize(5);
        configuration.setDefaultWindowWaitTimeout(configuration.getDefaultRequestExpiryTimeout());
        configuration.setDefaultSessionCountersEnabled(true);
        configuration.setJmxEnabled(true);

        return configuration;
    }

    /**
     * Start the Server
     */
    public void start() {
        boolean success = false;

        while (!success) {
            System.out.println("Starting server...");

            try {
                this.server.start();
                success = true;
                System.out.println("Server started on port " + serverConfiguration().getPort() + ".");
            } catch (SmppChannelException e) {
                System.err.println("Failed to start server. Retrying.");
            }
        }
    }

    /**
     * Stop the Server.
     */
    public void shutdown() {
        this.shutdown(true);
    }

    /**
     * Stop the Server.
     * 
     * @param exit Whether to stop execution.
     */
    public void shutdown(boolean exit) {
        System.out.println("Shutting down server...");

        this.server.stop();
        System.out.println("Server stopped.");

        this.server.destroy();
        System.out.println("Server destroyed.");

        if (exit) {
            System.out.println("Exiting...");
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        ServerMain server = new ServerMain();

        server.start();
    }
}
