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
        logger.info("CulturalContextActor initialized");
    }

    @Override
    public Receive<CulturalAnalysisRequestMessage> createReceive() {
        return newReceiveBuilder()
                .onMessage(CulturalAnalysisRequestMessage.class, this::onAnalyzeRequest)
                .build();
    }

    private Behavior<CulturalAnalysisRequestMessage> onAnalyzeRequest(CulturalAnalysisRequestMessage msg) {
        logger.info("Processing cultural analysis for country: {}, query: {}",
                msg.getCountry(), msg.getQuery());

        String culturalPrompt = buildCulturalPrompt(msg.getQuery(), msg.getCountry());

        Map<String, Object> context = new HashMap<>();
        context.put("country", msg.getCountry());
        context.put("scenario_type", "CULTURAL");
        context.put("query", msg.getQuery());

        ActorRef<LLMResponseMessage> adapter = getContext().messageAdapter(
                LLMResponseMessage.class,
                llmResponse -> {
                    String analysis;
                    if (llmResponse.isSuccess()) {
                        analysis = llmResponse.getResponse();
                    } else {
                        analysis = "I apologize, but I'm having trouble accessing cultural information at the moment. " +
                                "Please try again or consult with a cultural expert for guidance on " +
                                msg.getCountry() + ".";
                    }

                    CulturalAnalysisResponseMessage response = new CulturalAnalysisResponseMessage(
                            analysis,
                            context
                    );

                    msg.getReplyTo().tell(response);
                    return msg;
                }
        );

        LLMRequestMessage llmRequest = new LLMRequestMessage(culturalPrompt, context, adapter);
        llmActor.tell(llmRequest);

        logger.info("Sent cultural analysis request to LLM actor");

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
        prompt.append("2. Communication Approach: Appropriate style (direct/indirect, formal/informal)\n");
        prompt.append("3. Potential Pitfalls: Cultural mistakes to avoid\n");
        prompt.append("4. Practical Advice: Concrete, actionable recommendations\n\n");
        prompt.append("Keep response concise (under 250 words) and practical.");

        return prompt.toString();
    }
}