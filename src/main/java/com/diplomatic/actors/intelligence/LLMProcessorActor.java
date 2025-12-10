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
    private final String apiProvider;
    private final String model;
    private final ObjectMapper objectMapper;

    public static Behavior<LLMRequestMessage> create(String apiKey, String apiProvider) {
        return Behaviors.setup(context -> new LLMProcessorActor(context, apiKey, apiProvider));
    }

    private LLMProcessorActor(ActorContext<LLMRequestMessage> context, String apiKey, String apiProvider) {
        super(context);
        this.apiKey = apiKey;
        this.apiProvider = apiProvider != null ? apiProvider.toUpperCase() : "CLAUDE";
        this.objectMapper = new ObjectMapper();

        if (this.apiProvider.equals("OPENAI")) {
            this.model = "gpt-4";
        } else {
            this.model = "claude-sonnet-4-20250514";
        }

        logger.info("LLMProcessorActor initialized with provider: {} and model: {}", this.apiProvider, this.model);
        System.out.println("✅ LLMProcessorActor ready - Provider: " + this.apiProvider);
    }

    @Override
    public Receive<LLMRequestMessage> createReceive() {
        return newReceiveBuilder()
                .onMessage(LLMRequestMessage.class, msg -> {
                    System.out.println("\n🤖🤖🤖 LLM PROCESSOR RECEIVED REQUEST!");
                    System.out.println("    Prompt length: " + msg.getPrompt().length() + " chars");
                    System.out.println("    Provider: " + apiProvider);
                    return onLLMRequest(msg);
                })
                .build();
    }

    private Behavior<LLMRequestMessage> onLLMRequest(LLMRequestMessage msg) {
        logger.info("Processing LLM request with provider: {}", apiProvider);
        System.out.println("📡 Making API call to " + apiProvider + "...");

        getContext().pipeToSelf(
                java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                    try {
                        System.out.println("🌐 Calling " + apiProvider + " API NOW...");
                        String response = callLLMAPI(msg.getPrompt());
                        System.out.println("✅ API call successful! Response length: " + response.length());
                        return response;
                    } catch (Exception e) {
                        System.out.println("❌ API call failed: " + e.getMessage());
                        logger.error("LLM API call failed", e);
                        e.printStackTrace();
                        return null;
                    }
                }),
                (response, throwable) -> {
                    LLMResponseMessage llmResponse;
                    if (throwable != null || response == null) {
                        logger.error("LLM API error", throwable);
                        System.out.println("❌ Sending error response back");
                        llmResponse = new LLMResponseMessage(
                                "I apologize, but I'm having trouble connecting to the AI service.",
                                false
                        );
                    } else {
                        System.out.println("✅ Sending successful response back");
                        llmResponse = new LLMResponseMessage(response, true);
                    }
                    msg.getReplyTo().tell(llmResponse);
                    return msg;
                }
        );

        return this;
    }

    private String callLLMAPI(String prompt) throws Exception {
        if (apiProvider.equals("OPENAI")) {
            return callOpenAIAPI(prompt);
        } else {
            return callClaudeAPI(prompt);
        }
    }

    private String callClaudeAPI(String prompt) throws Exception {
        System.out.println("🔗 Connecting to Claude API...");
        System.out.println("   Endpoint: https://api.anthropic.com/v1/messages");
        System.out.println("   Model: " + model);

        URL url = new URL("https://api.anthropic.com/v1/messages");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("x-api-key", apiKey);
        conn.setRequestProperty("anthropic-version", "2023-06-01");
        conn.setDoOutput(true);

        System.out.println("✓ Headers set, API key length: " + apiKey.length());

        String requestBody = String.format(
                "{\"model\":\"%s\",\"max_tokens\":1024,\"messages\":[{\"role\":\"user\",\"content\":\"%s\"}]}",
                model,
                escapeJson(prompt)
        );

        System.out.println("✓ Request body created, length: " + requestBody.length());
        System.out.println("📤 Sending request to Claude...");

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        System.out.println("✓ Request sent, waiting for response...");
        int responseCode = conn.getResponseCode();
        System.out.println("📨 Response code: " + responseCode);

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

            System.out.println("✅ Claude API SUCCESS! Response received");
            JsonNode jsonResponse = objectMapper.readTree(response.toString());
            String text = jsonResponse.get("content").get(0).get("text").asText();
            System.out.println("✅ Extracted text, length: " + text.length());
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
            System.out.println("❌ Claude API ERROR: " + responseCode);
            System.out.println("   Error body: " + errorResponse.toString());
            throw new Exception("Claude API error: " + responseCode + " - " + errorResponse.toString());
        }
    }

    private String callOpenAIAPI(String prompt) throws Exception {
        System.out.println("🔗 Connecting to OpenAI API...");
        URL url = new URL("https://api.openai.com/v1/chat/completions");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);

        String requestBody = String.format(
                "{\"model\":\"%s\",\"max_tokens\":1024,\"messages\":[{\"role\":\"user\",\"content\":\"%s\"}]}",
                model,
                escapeJson(prompt)
        );

        System.out.println("📤 Sending request to OpenAI...");

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();
        System.out.println("📨 Response code: " + responseCode);

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

            System.out.println("✅ OpenAI API SUCCESS!");
            JsonNode jsonResponse = objectMapper.readTree(response.toString());
            return jsonResponse.get("choices").get(0).get("message").get("content").asText();
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
            System.out.println("❌ OpenAI API ERROR: " + responseCode);
            throw new Exception("OpenAI API error: " + responseCode + " - " + errorResponse.toString());
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