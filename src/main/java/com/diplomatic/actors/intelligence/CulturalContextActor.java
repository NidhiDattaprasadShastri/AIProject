package com.diplomatic.actors.intelligence;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.diplomatic.messages.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class CulturalContextActor extends AbstractBehavior<CulturalAnalysisRequestMessage> {

    private final Logger logger = LoggerFactory.getLogger(CulturalContextActor.class);
    private final ActorRef<LLMRequestMessage> llmActor;

    public static Behavior<CulturalAnalysisRequestMessage> create(ActorRef<LLMRequestMessage> llmActor) {
        return Behaviors.setup(context -> new CulturalContextActor(context, llmActor));
    }

    private CulturalContextActor(ActorContext<CulturalAnalysisRequestMessage> context,
                                 ActorRef<LLMRequestMessage> llmActor) {
        super(context);
        this.llmActor = llmActor;
        logger.info("CulturalContextActor initialized on Node 2");
    }

    @Override
    public Receive<CulturalAnalysisRequestMessage> createReceive() {
        return newReceiveBuilder()
                .onMessage(CulturalAnalysisRequestMessage.class, this::onAnalyzeRequest)
                .build();
    }

    private Behavior<CulturalAnalysisRequestMessage> onAnalyzeRequest(CulturalAnalysisRequestMessage msg) {
        // CRITICAL FIX: Ignore dummy messages from adapters to prevent infinite loop
        if (msg.getQuery() == null || msg.getQuery().trim().isEmpty()) {
            logger.debug("Ignoring empty query (adapter dummy message)");
            return this;
        }

        logger.info("Processing cultural analysis for country: {}", msg.getCountry());

        String culturalPrompt = buildCulturalPrompt(msg.getQuery(), msg.getCountry());

        Map<String, Object> context = new HashMap<>();
        context.put("country", msg.getCountry());
        context.put("scenario_type", "CULTURAL");
        context.put("query", msg.getQuery());

        // Store the original replyTo
        final ActorRef<CulturalAnalysisResponseMessage> originalReplyTo = msg.getReplyTo();

        ActorRef<LLMResponseMessage> adapter = getContext().messageAdapter(
                LLMResponseMessage.class,
                llmResponse -> {
                    String analysis;
                    if (llmResponse.isSuccess()) {
                        analysis = llmResponse.getResponse();
                    } else {
                        analysis = "I apologize, but I'm having trouble accessing cultural information.";
                    }

                    CulturalAnalysisResponseMessage response = new CulturalAnalysisResponseMessage(
                            analysis, context
                    );

                    originalReplyTo.tell(response);

                    // CRITICAL FIX: Return a dummy message with empty query to prevent re-processing
                    // The empty query will be caught by the guard clause above
                    return new CulturalAnalysisRequest("", "processed", originalReplyTo);
                }
        );

        LLMRequestMessage llmRequest = new LLMRequestMessage(culturalPrompt, context, adapter);
        llmActor.tell(llmRequest);

        logger.info("Cultural analysis request sent to LLM processor");

        return this;
    }

    private String buildCulturalPrompt(String query, String country) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a cross-cultural diplomatic advisor with expertise in international relations.\n\n");
        prompt.append("Context: Cultural guidance needed");
        if (!"General".equals(country)) {
            prompt.append(" for ").append(country);
        }
        prompt.append("\n\n");
        prompt.append("User Query: ").append(query).append("\n\n");
        prompt.append("Please provide:\n");
        prompt.append("1. Cultural Context: Key cultural considerations\n");
        prompt.append("2. Communication Approach: Appropriate style\n");
        prompt.append("3. Potential Pitfalls: Cultural mistakes to avoid\n");
        prompt.append("4. Practical Advice: Concrete recommendations\n\n");
        prompt.append("Keep response concise (under 250 words) and practical.");
        return prompt.toString();
    }
}