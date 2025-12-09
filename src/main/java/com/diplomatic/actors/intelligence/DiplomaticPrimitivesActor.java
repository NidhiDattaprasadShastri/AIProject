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

public class DiplomaticPrimitivesActor extends AbstractBehavior<DiplomaticPrimitiveRequestMessage> {

    private final Logger logger = LoggerFactory.getLogger(DiplomaticPrimitivesActor.class);
    private final ActorRef<LLMRequestMessage> llmActor;

    public static Behavior<DiplomaticPrimitiveRequestMessage> create(ActorRef<LLMRequestMessage> llmActor) {
        return Behaviors.setup(context -> new DiplomaticPrimitivesActor(context, llmActor));
    }

    private DiplomaticPrimitivesActor(ActorContext<DiplomaticPrimitiveRequestMessage> context,
                                      ActorRef<LLMRequestMessage> llmActor) {
        super(context);
        this.llmActor = llmActor;
        logger.info("DiplomaticPrimitivesActor initialized");
    }

    @Override
    public Receive<DiplomaticPrimitiveRequestMessage> createReceive() {
        return newReceiveBuilder()
                .onMessage(DiplomaticPrimitiveRequestMessage.class, this::onProcessRequest)
                .build();
    }

    private Behavior<DiplomaticPrimitiveRequestMessage> onProcessRequest(DiplomaticPrimitiveRequestMessage msg) {
        String primitive = msg.getPrimitive();
        logger.info("Processing diplomatic primitive: {}, query: {}", primitive, msg.getQuery());

        String primitivePrompt = buildPrimitivePrompt(msg.getQuery(), primitive);

        Map<String, Object> context = new HashMap<>();
        context.put("primitive", primitive);
        context.put("scenario_type", "DIPLOMATIC_PRIMITIVE");
        context.put("query", msg.getQuery());

        ActorRef<LLMResponseMessage> adapter = getContext().messageAdapter(
                LLMResponseMessage.class,
                llmResponse -> {
                    String result;
                    if (llmResponse.isSuccess()) {
                        result = llmResponse.getResponse();
                    } else {
                        result = "I apologize, but I'm having trouble accessing diplomatic guidance at the moment. " +
                                "Please try again or consult with a diplomatic expert regarding the " +
                                primitive + " primitive.";
                    }

                    DiplomaticPrimitiveResponseMessage response = new DiplomaticPrimitiveResponseMessage(
                            primitive,
                            result
                    );

                    msg.getReplyTo().tell(response);
                    return msg;
                }
        );

        LLMRequestMessage llmRequest = new LLMRequestMessage(primitivePrompt, context, adapter);
        llmActor.tell(llmRequest);

        logger.info("Sent primitive analysis request to LLM actor for primitive: {}", primitive);

        return this;
    }

    private String buildPrimitivePrompt(String query, String primitive) {
        return "You are a diplomatic negotiation advisor using the IDEA Framework.\n\n" +
                "IDEA Framework Primitive: " + primitive + "\n" +
                getPrimitiveDefinition(primitive) + "\n\n" +
                "User Query: " + query + "\n\n" +
                "Please provide:\n" +
                "1. Strategy: How to effectively apply the " + primitive + " primitive\n" +
                "2. Key Actions: Specific steps to take\n" +
                "3. Expected Outcomes: What to anticipate\n" +
                "4. Next Steps: Follow-up actions\n\n" +
                "Keep response concise (under 250 words) and action-oriented.";
    }

    private String getPrimitiveDefinition(String primitive) {
        switch (primitive) {
            case "PROPOSE":
                return "PROPOSE: Present new ideas, terms, or solutions to advance negotiations";
            case "CLARIFY":
                return "CLARIFY: Seek or provide understanding of positions, intentions, or terms";
            case "CONSTRAIN":
                return "CONSTRAIN: Define boundaries, limitations, or requirements";
            case "REVISE":
                return "REVISE: Modify existing proposals based on feedback";
            case "AGREE":
                return "AGREE: Reach consensus, accept terms, or establish mutual understanding";
            case "ESCALATE":
                return "ESCALATE: Elevate unresolved issues to higher authority";
            case "DEFER":
                return "DEFER: Postpone decisions to allow for more information";
            default:
                return "GENERAL: Diplomatic negotiation and relationship building";
        }
    }
}