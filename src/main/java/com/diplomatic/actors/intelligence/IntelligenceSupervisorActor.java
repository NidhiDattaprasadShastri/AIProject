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

/**
 * Intelligence Supervisor Actor - Part B
 * Spawns and manages all intelligence actors
 */
public class IntelligenceSupervisorActor extends AbstractBehavior<IntelligenceSupervisorActor.Command> {

    private final Logger logger = LoggerFactory.getLogger(IntelligenceSupervisorActor.class);

    public interface Command {}

    public static class Initialize implements Command {
        public final String apiKey;
        public final String apiProvider;

        public Initialize(String apiKey, String apiProvider) {
            this.apiKey = apiKey;
            this.apiProvider = apiProvider;
        }
    }

    public static class IntelligenceActorsReady implements Command {
        public final ActorRef<RouteToClassifierMessage> classifierActor;
        public final ActorRef<CulturalAnalysisRequestMessage> culturalActor;
        public final ActorRef<DiplomaticPrimitiveRequestMessage> primitivesActor;
        public final ActorRef<LLMRequestMessage> llmActor;

        public IntelligenceActorsReady(
                ActorRef<RouteToClassifierMessage> classifierActor,
                ActorRef<CulturalAnalysisRequestMessage> culturalActor,
                ActorRef<DiplomaticPrimitiveRequestMessage> primitivesActor,
                ActorRef<LLMRequestMessage> llmActor) {
            this.classifierActor = classifierActor;
            this.culturalActor = culturalActor;
            this.primitivesActor = primitivesActor;
            this.llmActor = llmActor;
        }
    }

    private ActorRef<RouteToClassifierMessage> classifierActor;
    private ActorRef<CulturalAnalysisRequestMessage> culturalActor;
    private ActorRef<DiplomaticPrimitiveRequestMessage> primitivesActor;
    private ActorRef<LLMRequestMessage> llmActor;

    public static Behavior<Command> create() {
        return Behaviors.setup(IntelligenceSupervisorActor::new);
    }

    private IntelligenceSupervisorActor(ActorContext<Command> context) {
        super(context);
        logger.info("IntelligenceSupervisorActor started");
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(Initialize.class, this::onInitialize)
                .build();
    }

    private Behavior<Command> onInitialize(Initialize msg) {
        logger.info("Initializing intelligence actors with provider: {}", msg.apiProvider);

        try {
            // 1. Spawn LLM Actor first (needed by other actors)
            this.llmActor = getContext().spawn(
                    LLMProcessorActor.create(msg.apiKey, msg.apiProvider),
                    "llm-processor"
            );
            logger.info("✓ LLMProcessorActor spawned");

            // 2. Spawn Scenario Classifier Actor
            this.classifierActor = getContext().spawn(
                    ScenarioClassifierActor.create(),
                    "scenario-classifier"
            );
            logger.info("✓ ScenarioClassifierActor spawned");

            // 3. Spawn Cultural Context Actor (depends on LLM)
            this.culturalActor = getContext().spawn(
                    CulturalContextActor.create(llmActor),
                    "cultural-context"
            );
            logger.info("✓ CulturalContextActor spawned");

            // 4. Spawn Diplomatic Primitives Actor (depends on LLM)
            this.primitivesActor = getContext().spawn(
                    DiplomaticPrimitivesActor.create(llmActor),
                    "diplomatic-primitives"
            );
            logger.info("✓ DiplomaticPrimitivesActor spawned");

            logger.info("All intelligence actors initialized successfully!");

            // Actors are ready - they can now be accessed via getters
            return this;

        } catch (Exception e) {
            logger.error("Failed to initialize intelligence actors", e);
            throw new RuntimeException("Intelligence actor initialization failed", e);
        }
    }

    // Getters for actor references
    public ActorRef<RouteToClassifierMessage> getClassifierActor() {
        return classifierActor;
    }

    public ActorRef<CulturalAnalysisRequestMessage> getCulturalActor() {
        return culturalActor;
    }

    public ActorRef<DiplomaticPrimitiveRequestMessage> getPrimitivesActor() {
        return primitivesActor;
    }

    public ActorRef<LLMRequestMessage> getLlmActor() {
        return llmActor;
    }
}