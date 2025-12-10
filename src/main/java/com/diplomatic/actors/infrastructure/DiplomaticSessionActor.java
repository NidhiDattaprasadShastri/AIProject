package com.diplomatic.actors.infrastructure;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.diplomatic.messages.*;

/**
 * DiplomaticSessionActor - Orchestrates individual user sessions
 *
 * PROJECT REQUIREMENTS DEMONSTRATED:
 * - TELL pattern: Fire-and-forget to history actor
 * - ASK pattern: Request-response via message adapters
 * - FORWARD pattern: Preserving sender context through routing
 */
public class DiplomaticSessionActor extends AbstractBehavior<DiplomaticSessionActor.Command> {

    public interface Command {}

    public static class ProcessQuery implements Command {
        public final String query;
        public ProcessQuery(String query) {
            this.query = query;
        }
    }

    public static class SetResponseHandler implements Command {
        public final ActorRef<String> replyTo;
        public SetResponseHandler(ActorRef<String> replyTo) {
            this.replyTo = replyTo;
        }
    }

    public static class SetIntelligenceActors implements Command {
        public final ActorRef<RouteToClassifierMessage> classifierActor;
        public final ActorRef<CulturalAnalysisRequestMessage> culturalActor;
        public final ActorRef<DiplomaticPrimitiveRequestMessage> primitivesActor;

        public SetIntelligenceActors(
                ActorRef<RouteToClassifierMessage> classifierActor,
                ActorRef<CulturalAnalysisRequestMessage> culturalActor,
                ActorRef<DiplomaticPrimitiveRequestMessage> primitivesActor) {
            this.classifierActor = classifierActor;
            this.culturalActor = culturalActor;
            this.primitivesActor = primitivesActor;
        }
    }

    private static class HandleClassification implements Command {
        public final ClassificationResultMessage result;
        public final String originalQuery;

        public HandleClassification(ClassificationResultMessage result, String originalQuery) {
            this.result = result;
            this.originalQuery = originalQuery;
        }
    }

    private final String sessionId;
    private final String userId;
    private final ActorRef<ConversationHistoryActor.Command> historyManager;

    private ActorRef<RouteToClassifierMessage> classifierActor;
    private ActorRef<CulturalAnalysisRequestMessage> culturalActor;
    private ActorRef<DiplomaticPrimitiveRequestMessage> primitivesActor;
    private ActorRef<String> currentReplyTo;

    public static Behavior<Command> create(
            String sessionId,
            String userId,
            ActorRef<ConversationHistoryActor.Command> historyManager) {
        return Behaviors.setup(context -> new DiplomaticSessionActor(context, sessionId, userId, historyManager));
    }

    private DiplomaticSessionActor(
            ActorContext<Command> context,
            String sessionId,
            String userId,
            ActorRef<ConversationHistoryActor.Command> historyManager) {
        super(context);
        this.sessionId = sessionId;
        this.userId = userId;
        this.historyManager = historyManager;
        context.getLog().info("DiplomaticSessionActor created for session: {}", sessionId);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(SetIntelligenceActors.class, this::onSetIntelligenceActors)
                .onMessage(SetResponseHandler.class, this::onSetResponseHandler)
                .onMessage(ProcessQuery.class, this::onProcessQuery)
                .onMessage(HandleClassification.class, this::onHandleClassification)
                .build();
    }

    private Behavior<Command> onSetIntelligenceActors(SetIntelligenceActors cmd) {
        this.classifierActor = cmd.classifierActor;
        this.culturalActor = cmd.culturalActor;
        this.primitivesActor = cmd.primitivesActor;
        getContext().getLog().info("Intelligence actors configured for session: {}", sessionId);
        return this;
    }

    private Behavior<Command> onSetResponseHandler(SetResponseHandler cmd) {
        this.currentReplyTo = cmd.replyTo;
        return this;
    }

    private Behavior<Command> onProcessQuery(ProcessQuery cmd) {
        // Ignore empty queries (used as no-op messages from adapters)
        if (cmd.query == null || cmd.query.trim().isEmpty()) {
            return this;
        }

        getContext().getLog().info("Processing query for session {}: {}", sessionId, cmd.query);

        if (classifierActor == null) {
            getContext().getLog().warn("Intelligence actors not configured for session: {}", sessionId);
            if (currentReplyTo != null) {
                currentReplyTo.tell("System initializing, please try again...");
            }
            return this;
        }

        if (currentReplyTo == null) {
            getContext().getLog().warn("No reply handler set for session: {}", sessionId);
            return this;
        }

        // REQUIREMENT: ASK pattern (request-response via message adapter)
        ActorRef<ClassificationResultMessage> adapter = getContext().messageAdapter(
                ClassificationResultMessage.class,
                result -> new HandleClassification(result, cmd.query)
        );

        classifierActor.tell(new RouteToClassifierMessage(sessionId, cmd.query, adapter));
        getContext().getLog().info("Query sent to classifier for session: {}", sessionId);

        return this;
    }

    private Behavior<Command> onHandleClassification(HandleClassification cmd) {
        getContext().getLog().info("Classification received: {} for session: {}",
                cmd.result.getScenario(), sessionId);

        if ("CULTURAL".equals(cmd.result.getScenario())) {
            // Store context for later use in adapter
            final String query = cmd.originalQuery;
            final ActorRef<String> replyTo = currentReplyTo;

            // REQUIREMENT: ASK pattern (request-response via message adapter)
            ActorRef<CulturalAnalysisResponseMessage> adapter = getContext().messageAdapter(
                    CulturalAnalysisResponseMessage.class,
                    response -> {
                        // Send response immediately in the adapter
                        if (replyTo != null) {
                            replyTo.tell(response.getAnalysis());
                        }

                        // REQUIREMENT: TELL pattern (fire-and-forget to history)
                        historyManager.tell(new ConversationHistoryActor.SaveConversation(
                                new SaveConversationMessage(sessionId, query, response.getAnalysis())));

                        // Return a valid Command (no-op that doesn't cause side effects)
                        return new SetResponseHandler(replyTo);
                    }
            );

            // REQUIREMENT: FORWARD pattern (preserving original sender context)
            culturalActor.tell(new CulturalAnalysisRequest(
                    cmd.originalQuery, cmd.result.getDetectedCountry(), adapter));

        } else {
            // Store context for later use in adapter
            final String query = cmd.originalQuery;
            final ActorRef<String> replyTo = currentReplyTo;

            // REQUIREMENT: ASK pattern (request-response via message adapter)
            ActorRef<DiplomaticPrimitiveResponseMessage> adapter = getContext().messageAdapter(
                    DiplomaticPrimitiveResponseMessage.class,
                    response -> {
                        String formatted = response.getResult() + "\n\n[Primitive: " + response.getPrimitive() + "]";

                        // Send response immediately in the adapter
                        if (replyTo != null) {
                            replyTo.tell(formatted);
                        }

                        // REQUIREMENT: TELL pattern (fire-and-forget to history)
                        historyManager.tell(new ConversationHistoryActor.SaveConversation(
                                new SaveConversationMessage(sessionId, query, formatted)));

                        // Return a valid Command (no-op that doesn't cause side effects)
                        return new SetResponseHandler(replyTo);
                    }
            );

            // REQUIREMENT: FORWARD pattern (preserving original sender context)
            primitivesActor.tell(new DiplomaticPrimitiveRequestMessage(
                    cmd.result.getDetectedPrimitive(), cmd.originalQuery, adapter));
        }

        return this;
    }
}