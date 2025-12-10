package com.diplomatic.actors.intelligence;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.diplomatic.messages.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScenarioClassifierActor extends AbstractBehavior<RouteToClassifierMessage> {

    private final Logger logger = LoggerFactory.getLogger(ScenarioClassifierActor.class);

    public static Behavior<RouteToClassifierMessage> create() {
        return Behaviors.setup(ScenarioClassifierActor::new);
    }

    private ScenarioClassifierActor(ActorContext<RouteToClassifierMessage> context) {
        super(context);
        logger.info("ScenarioClassifierActor initialized");
        System.out.println("✅ ScenarioClassifierActor ready on Node 2");
    }

    @Override
    public Receive<RouteToClassifierMessage> createReceive() {
        return newReceiveBuilder()
                .onMessage(RouteToClassifierMessage.class, msg -> {
                    System.out.println("\n🎯🎯🎯 CLASSIFIER RECEIVED MESSAGE ON NODE 2!");
                    System.out.println("    SessionId: " + msg.getSessionId());
                    System.out.println("    Query: " + msg.getQuery());
                    return onClassify(msg);
                })
                .build();
    }

    private Behavior<RouteToClassifierMessage> onClassify(RouteToClassifierMessage msg) {
        String query = msg.getQuery().toLowerCase();
        logger.info("Classifying query for session {}: {}", msg.getSessionId(), query);
        System.out.println("🔍 Classifying: " + query);

        String scenario;
        String targetActor;
        String detectedCountry = detectCountry(query);
        String detectedPrimitive = detectPrimitive(query);
        double confidence;

        if (isCulturalQuery(query)) {
            scenario = "CULTURAL";
            targetActor = "CulturalContextActor";
            confidence = 0.85;
            System.out.println("📋 Classified as CULTURAL - Country: " + detectedCountry);
        } else if (isDiplomaticPrimitiveQuery(query)) {
            scenario = "PRIMITIVE";
            targetActor = "DiplomaticPrimitivesActor";
            confidence = 0.90;
            System.out.println("📋 Classified as PRIMITIVE - Primitive: " + detectedPrimitive);
        } else {
            scenario = "GENERAL";
            targetActor = "DiplomaticPrimitivesActor";
            confidence = 0.60;
            System.out.println("📋 Classified as GENERAL - using PRIMITIVE");
        }

        ClassificationResultMessage result = new ClassificationResultMessage(
                scenario, targetActor, confidence, detectedCountry, detectedPrimitive
        );

        System.out.println("📤 Sending classification result back to Node 1");
        msg.getReplyTo().tell(result);
        System.out.println("✅ Classification sent!\n");

        return this;
    }

    private boolean isCulturalQuery(String query) {
        String[] culturalKeywords = {
                "culture", "cultural", "tradition", "custom", "etiquette", "greeting",
                "gift", "hierarchy", "formality", "dress code", "body language",
                "communication style", "direct", "indirect", "religious", "festival"
        };
        for (String keyword : culturalKeywords) {
            if (query.contains(keyword)) return true;
        }
        return false;
    }

    private boolean isDiplomaticPrimitiveQuery(String query) {
        String[] primitiveKeywords = {
                "propose", "proposal", "negotiate", "negotiation",
                "clarify", "clarification", "understand",
                "constrain", "constraint", "limit", "deadline",
                "revise", "revision", "modify", "change",
                "agree", "agreement", "consensus",
                "escalate", "escalation", "elevate",
                "defer", "postpone", "delay"
        };
        for (String keyword : primitiveKeywords) {
            if (query.contains(keyword)) return true;
        }
        return false;
    }

    private String detectCountry(String query) {
        String[] countries = {
                "japan", "japanese", "kuwait", "kuwaiti", "morocco", "moroccan",
                "canada", "canadian", "turkey", "turkish", "mauritania", "mauritanian",
                "china", "chinese", "india", "indian", "germany", "german",
                "france", "french", "arab", "arabic", "iraq", "iraqi"
        };
        for (String country : countries) {
            if (query.contains(country)) {
                return capitalize(country);
            }
        }
        return "General";
    }

    private String detectPrimitive(String query) {
        if (query.contains("propose") || query.contains("proposal")) return "PROPOSE";
        if (query.contains("clarify") || query.contains("clarification")) return "CLARIFY";
        if (query.contains("constrain") || query.contains("constraint")) return "CONSTRAIN";
        if (query.contains("revise") || query.contains("revision")) return "REVISE";
        if (query.contains("agree") || query.contains("agreement")) return "AGREE";
        if (query.contains("escalate") || query.contains("escalation")) return "ESCALATE";
        if (query.contains("defer") || query.contains("postpone")) return "DEFER";
        return "GENERAL";
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}