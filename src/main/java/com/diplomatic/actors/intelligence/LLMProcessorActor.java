package com.diplomatic.actors.intelligence;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.diplomatic.messages.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class LLMProcessorActor extends AbstractBehavior<LLMRequestMessage> {

    private final Logger logger = LoggerFactory.getLogger(LLMProcessorActor.class);
    private final String apiKey;
    private final ObjectMapper objectMapper;
    private static final String MODEL = "claude-sonnet-4-20250514";

    public static Behavior<LLMRequestMessage> create(String apiKey, String apiProvider) {
        return Behaviors.setup(context -> new LLMProcessorActor(context, apiKey));
    }

    private LLMProcessorActor(ActorContext<LLMRequestMessage> context, String apiKey) {
        super(context);
        this.apiKey = apiKey;
        this.objectMapper = new ObjectMapper();
        logger.info("LLMProcessorActor initialized - Provider: CLAUDE, Model: {}", MODEL);
    }

    @Override
    public Receive<LLMRequestMessage> createReceive() {
        return newReceiveBuilder()
                .onMessage(LLMRequestMessage.class, this::onLLMRequest)
                .build();
    }

    private Behavior<LLMRequestMessage> onLLMRequest(LLMRequestMessage msg) {
        logger.info("Processing LLM request with Claude API");

        getContext().pipeToSelf(
                java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                    try {
                        return callClaudeAPI(msg.getPrompt());
                    } catch (Exception e) {
                        logger.error("Claude API call failed: {}", e.getMessage());
                        return null;
                    }
                }),
                (response, throwable) -> {
                    LLMResponseMessage llmResponse;
                    if (throwable != null || response == null) {
                        logger.error("Claude API error", throwable);
                        llmResponse = new LLMResponseMessage(
                                "I apologize, but I'm having trouble connecting to the AI service.",
                                false
                        );
                    } else {
                        logger.info("Claude API call successful");
                        llmResponse = new LLMResponseMessage(response, true);
                    }
                    msg.getReplyTo().tell(llmResponse);
                    return msg;
                }
        );

        return this;
    }

    private String callClaudeAPI(String prompt) throws Exception {
        logger.debug("Connecting to Claude API");
        URL url = new URL("https://api.anthropic.com/v1/messages");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("x-api-key", apiKey);
        conn.setRequestProperty("anthropic-version", "2023-06-01");
        conn.setDoOutput(true);

        String requestBody = String.format(
                "{\"model\":\"%s\",\"max_tokens\":1024,\"messages\":[{\"role\":\"user\",\"content\":\"%s\"}]}",
                MODEL,
                escapeJson(prompt)
        );

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();
        logger.debug("Claude API response code: {}", responseCode);

        if (responseCode == 200) {
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
            );
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            br.close();

            JsonNode jsonResponse = objectMapper.readTree(response.toString());
            String text = jsonResponse.get("content").get(0).get("text").asText();
            return text;
        } else {
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8)
            );
            StringBuilder errorResponse = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                errorResponse.append(line);
            }
            br.close();
            throw new Exception("Claude API error: " + responseCode + " - " + errorResponse.toString());
        }
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}