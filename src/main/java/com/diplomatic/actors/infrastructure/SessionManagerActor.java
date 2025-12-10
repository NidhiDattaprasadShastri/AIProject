package com.diplomatic.actors.infrastructure;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import com.diplomatic.messages.*;
import java.util.*;

public class SessionManagerActor extends AbstractBehavior<SessionManagerActor.Command> {

    private final Map<String, ActorRef<DiplomaticSessionActor.Command>> activeSessions;
    private final ActorRef<ConversationHistoryActor.Command> historyActor;
    private ActorRef<RouteToClassifierMessage> classifierActor;
    private ActorRef<CulturalAnalysisRequestMessage> culturalActor;
    private ActorRef<DiplomaticPrimitiveRequestMessage> primitivesActor;
    private boolean intelligenceActorsReady = false;

    public interface Command {}

    public static class CreateSession implements Command {
        public final String userId;
        public final ActorRef<SessionCreatedMessage> replyTo;
        public CreateSession(String userId, ActorRef<SessionCreatedMessage> replyTo) {
            this.userId = userId;
            this.replyTo = replyTo;
        }
    }

    public static class RouteToSession implements Command {
        public final String sessionId;
        public final String query;
        public final ActorRef<String> replyTo;
        public RouteToSession(String sessionId, String query, ActorRef<String> replyTo) {
            this.sessionId = sessionId;
            this.query = query;
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

    private SessionManagerActor(ActorContext<Command> context) {
        super(context);
        this.activeSessions = new HashMap<>();
        this.historyActor = context.spawn(ConversationHistoryActor.create(), "conversation-history");
        System.out.println("✅ SessionManagerActor initialized");
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(SessionManagerActor::new);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(SetIntelligenceActors.class, this::onSetIntelligenceActors)
                .onMessage(CreateSession.class, this::onCreateSession)
                .onMessage(RouteToSession.class, this::onRouteToSession)
                .build();
    }

    private Behavior<Command> onSetIntelligenceActors(SetIntelligenceActors cmd) {
        this.classifierActor = cmd.classifierActor;
        this.culturalActor = cmd.culturalActor;
        this.primitivesActor = cmd.primitivesActor;
        this.intelligenceActorsReady = true;

        System.out.println("✅ SessionManager received intelligence actors!");

        for (ActorRef<DiplomaticSessionActor.Command> sessionActor : activeSessions.values()) {
            sessionActor.tell(new DiplomaticSessionActor.SetIntelligenceActors(
                    classifierActor, culturalActor, primitivesActor));
        }
        return this;
    }

    private Behavior<Command> onCreateSession(CreateSession cmd) {
        String sessionId = UUID.randomUUID().toString();
        System.out.println("🆕 Creating session: " + sessionId);

        ActorRef<DiplomaticSessionActor.Command> sessionActor = getContext().spawn(
                DiplomaticSessionActor.create(sessionId, cmd.userId, historyActor),
                "session-" + sessionId
        );

        activeSessions.put(sessionId, sessionActor);

        if (intelligenceActorsReady) {
            sessionActor.tell(new DiplomaticSessionActor.SetIntelligenceActors(
                    classifierActor, culturalActor, primitivesActor));
        }

        cmd.replyTo.tell(new SessionCreatedMessage(sessionId, cmd.userId));
        System.out.println("✅ Session created: " + sessionId);
        return this;
    }

    private Behavior<Command> onRouteToSession(RouteToSession cmd) {
        System.out.println("📨 Routing query to session: " + cmd.sessionId);

        ActorRef<DiplomaticSessionActor.Command> sessionActor = activeSessions.get(cmd.sessionId);

        if (sessionActor == null) {
            System.out.println("❌ Session not found!");
            cmd.replyTo.tell("Error: Session not found");
            return this;
        }

        // First, set the response handler
        sessionActor.tell(new DiplomaticSessionActor.SetResponseHandler(cmd.replyTo));

        // Then send the query
        sessionActor.tell(new DiplomaticSessionActor.ProcessQuery(cmd.query));

        System.out.println("✅ Query and response handler sent to DiplomaticSessionActor!");
        return this;
    }
}
