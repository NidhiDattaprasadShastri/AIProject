package com.diplomatic.actors.infrastructure;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.diplomatic.actors.intelligence.IntelligenceNodeSupervisor;
import com.diplomatic.messages.*;

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

    private static class HandleCulturalResponse implements Command {
        public final CulturalAnalysisResponseMessage response;
        public final String originalQuery;

        public HandleCulturalResponse(CulturalAnalysisResponseMessage response, String originalQuery) {
            this.response = response;
            this.originalQuery = originalQuery;
        }
    }

    private static class HandleDiplomaticResponse implements Command {
        public final DiplomaticPrimitiveResponseMessage response;
        public final String originalQuery;

        public HandleDiplomaticResponse(DiplomaticPrimitiveResponseMessage response, String originalQuery) {
            this.response = response;
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
        System.out.println("✅ DiplomaticSessionActor created: " + sessionId);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(SetIntelligenceActors.class, this::onSetIntelligenceActors)
                .onMessage(SetResponseHandler.class, this::onSetResponseHandler)
                .onMessage(ProcessQuery.class, msg -> {
                    System.out.println("🔥🔥🔥 RECEIVED ProcessQuery!");
                    System.out.println("    Query: " + msg.query);
                    return onProcessQuery(msg);
                })
                .onMessage(HandleClassification.class, this::onHandleClassification)
                .onMessage(HandleCulturalResponse.class, this::onHandleCulturalResponse)
                .onMessage(HandleDiplomaticResponse.class, this::onHandleDiplomaticResponse)
                .build();
    }

    private Behavior<Command> onSetIntelligenceActors(SetIntelligenceActors cmd) {
        this.classifierActor = cmd.classifierActor;
        this.culturalActor = cmd.culturalActor;
        this.primitivesActor = cmd.primitivesActor;
        System.out.println("✅ Intelligence actors set for session: " + sessionId);
        return this;
    }

    private Behavior<Command> onSetResponseHandler(SetResponseHandler cmd) {
        this.currentReplyTo = cmd.replyTo;
        System.out.println("✅ Response handler set");
        return this;
    }

    private Behavior<Command> onProcessQuery(ProcessQuery cmd) {
        System.out.println("🔥 Processing query: " + cmd.query);

        if (classifierActor == null) {
            System.out.println("❌ Intelligence actors NOT configured!");
            if (currentReplyTo != null) {
                currentReplyTo.tell("System initializing, please try again...");
            }
            return this;
        }

        if (currentReplyTo == null) {
            System.out.println("❌ No reply handler!");
            return this;
        }

        System.out.println("✅ Sending to classifier on Node 2");

        ActorRef<ClassificationResultMessage> adapter = getContext().messageAdapter(
                ClassificationResultMessage.class,
                result -> new HandleClassification(result, cmd.query)
        );

        classifierActor.tell(new RouteToClassifierMessage(sessionId, cmd.query, adapter));
        System.out.println("➡️  TELL: Sent to classifier on Node 2");

        return this;
    }

    private Behavior<Command> onHandleClassification(HandleClassification cmd) {
        System.out.println("📊 Classification: " + cmd.result.getScenario());

        if ("CULTURAL".equals(cmd.result.getScenario())) {
            ActorRef<CulturalAnalysisResponseMessage> adapter = getContext().messageAdapter(
                    CulturalAnalysisResponseMessage.class,
                    response -> new HandleCulturalResponse(response, cmd.originalQuery)
            );

            culturalActor.tell(new CulturalAnalysisRequest(
                    cmd.originalQuery, cmd.result.getDetectedCountry(), adapter));
            System.out.println("➡️  FORWARD: To CulturalContextActor");

        } else {
            ActorRef<DiplomaticPrimitiveResponseMessage> adapter = getContext().messageAdapter(
                    DiplomaticPrimitiveResponseMessage.class,
                    response -> new HandleDiplomaticResponse(response, cmd.originalQuery)
            );

            primitivesActor.tell(new DiplomaticPrimitiveRequestMessage(
                    cmd.result.getDetectedPrimitive(), cmd.originalQuery, adapter));
            System.out.println("➡️  FORWARD: To PrimitivesActor");
        }

        return this;
    }

    private Behavior<Command> onHandleCulturalResponse(HandleCulturalResponse cmd) {
        System.out.println("📩 Cultural response from Node 2!");
        currentReplyTo.tell(cmd.response.getAnalysis());

        historyManager.tell(new ConversationHistoryActor.SaveConversation(
                new SaveConversationMessage(sessionId, cmd.originalQuery, cmd.response.getAnalysis())));
        System.out.println("➡️  TELL: Saved to history");

        return this;
    }

    private Behavior<Command> onHandleDiplomaticResponse(HandleDiplomaticResponse cmd) {
        System.out.println("📩 Diplomatic response from Node 2!");
        String formatted = cmd.response.getResult() + "\n\n[Primitive: " + cmd.response.getPrimitive() + "]";
        currentReplyTo.tell(formatted);

        historyManager.tell(new ConversationHistoryActor.SaveConversation(
                new SaveConversationMessage(sessionId, cmd.originalQuery, formatted)));
        System.out.println("➡️  TELL: Saved to history");

        return this;
    }
}