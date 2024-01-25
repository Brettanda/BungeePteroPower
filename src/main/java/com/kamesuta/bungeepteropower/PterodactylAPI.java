package com.kamesuta.bungeepteropower;

import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

import static com.kamesuta.bungeepteropower.BungeePteroPower.logger;

/**
 * Pterodactyl API client.
 */
public class PterodactylAPI {
    private final URI pterodactylUrl;
    private final String pterodactylToken;
    private final Map<String, String> serverIdMap;

    /**
     * Create a new Pterodactyl API client.
     *
     * @param pterodactylUrl   The Pterodactyl server url
     * @param pterodactylToken The Pterodactyl API token
     * @param serverIdMap     The map of Bungeecord server name to Pterodactyl server ID
     */
    public PterodactylAPI(URI pterodactylUrl, String pterodactylToken, Map<String, String> serverIdMap) {
        this.pterodactylUrl = pterodactylUrl;
        this.pterodactylToken = pterodactylToken;
        this.serverIdMap = serverIdMap;
    }

    /**
     * Get the Pterodactyl server ID from the Bungeecord server name.
     * @param serverName The Bungeecord server name
     * @return The Pterodactyl server ID
     */
    public @Nullable String getServerId(String serverName) {
        return serverIdMap.get(serverName);
    }

    /**
     * Send a power signal to the Pterodactyl server.
     *
     * @param serverName          The name of the server to start
     * @param pterodactylServerId The Pterodactyl server ID
     * @param signal              The power signal to send
     * @return A future that completes when the request is finished
     */
    public CompletableFuture<Void> sendPowerSignal(String serverName, String pterodactylServerId, PowerSignal signal) {
        logger.info(String.format("Starting server: %s (Pterodactyl server ID: %s)", serverName, pterodactylServerId));

        // Create a path
        String path = "/api/client/servers/" + pterodactylServerId + "/power";

        OkHttpClient client = new OkHttpClient();

        // Create a form body to send power signal
        FormBody.Builder formBuilder = new FormBody.Builder();
        formBuilder.add("signal", signal.getSignal());
        RequestBody formBody = formBuilder.build();

        // Create a request
        Request request = new Request.Builder()
                .url(pterodactylUrl.resolve(path).toString())
                .post(formBody)
                .addHeader("Authorization", "Bearer " + pterodactylToken)
                .build();

        // Execute request and register a callback
        CompletableFuture<Void> future = new CompletableFuture<>();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                logger.log(Level.WARNING, "Failed to start server: " + serverName, e);
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                if (response.isSuccessful()) {
                    logger.info("Successfully started server: " + serverName);
                    future.complete(null);
                } else {
                    logger.warning("Failed to start server: " + serverName + ". Response: " + response);
                    future.completeExceptionally(new RuntimeException("Failed to start server: " + serverName + ". Response: " + response));
                }
                response.close();
            }
        });
        return future;
    }

    /**
     * Power signal type.
     */
    public enum PowerSignal {
        START("start"),
        STOP("stop"),
        RESTART("restart"),
        KILL("kill"),
        ;

        private final String signal;

        PowerSignal(String signal) {
            this.signal = signal;
        }

        public String getSignal() {
            return signal;
        }
    }
}