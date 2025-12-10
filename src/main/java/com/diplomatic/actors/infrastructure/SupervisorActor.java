package com.diplomatic.actors.infrastructure;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.SupervisorStrategy;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.diplomatic.actors.intelligence.IntelligenceSupervisorActor;
import com.diplomatic.messages.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * CORRECTED SupervisorActor - Updated to match new RouteQuery signature
 * NOTE: This file is NOT used by Node1App (which uses ClusterSupervisorActor instead)
 * But it's corrected here to prevent compilation errors
 */
public class SupervisorActor extends AbstractBehavior<SupervisorActor.Command> {

    private final Logger logger = LoggerFactory.getLogger(SupervisorActor.class);
    private final ActorRef<SessionManagerActor.Command> sessionManager;
    private ActorRef<IntelligenceSupervisorActor.Command> intelligenceSupervisor;

    private ActorRef<RouteToClassifierMessage> classifierActor;
    private ActorRef<CulturalAnalysisRequestMessage> culturalActor;
    private ActorRef<DiplomaticPrimitiveRequestMessage> primitivesActor;

    public interface Command {}

    public static final class CreateSession implements Command {
        public final String userId;
        public final ActorRef<SessionCreatedMessage> replyTo;
        public CreateSession(String userId, ActorRef<SessionCreatedMessage> replyTo) {
            this.userId = userId;
            this.replyTo = replyTo;
        }
    }

    // CORRECTED: RouteQuery with separate fields
    public static final class RouteQuery implements Command {
        public final String sessionId;
        public final String query;
        public final ActorRef<String> replyTo;

        public RouteQuery(String sessionId, String query, ActorRef<String> replyTo) {
            this.sessionId = sessionId;
            this.query = query;
            this.replyTo = replyTo;
        }
    }

    public static final class InitializeIntelligence implements Command {
        public final String apiKey;
        public final String apiProvider;

        public InitializeIntelligence(String apiKey, String apiProvider) {
            this.apiKey = apiKey;
            this.apiProvider = apiProvider;
        }
    }

    public static final class Shutdown implements Command {
        public static final Shutdown INSTANCE = new Shutdown();
        private Shutdown() {}
    }

    private SupervisorActor(ActorContext<Command> context) {
        super(context);
        this.sessionManager = context.spawn(
                Behaviors.supervise(SessionManagerActor.create()).onFailure(
                        SupervisorStrategy.restart().withLimit(3, Duration.ofMinutes(1))
                ),
                "session-manager"
        );
        logger.info("SupervisorActor initialized with SessionManager");
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(SupervisorActor::new);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(InitializeIntelligence.class, this::onInitializeIntelligence)
                .onMessage(CreateSession.class, this::onCreateSession)
                .onMessage(RouteQuery.class, this::onRouteQuery)
                .onMessage(Shutdown.class, this::onShutdown)
                .build();
    }

    private Behavior<Command> onInitializeIntelligence(InitializeIntelligence msg) {
        logger.info("Initializing intelligence actors...");

        try {
            this.intelligenceSupervisor = getContext().spawn(
                    IntelligenceSupervisorActor.create(),
                    "intelligence-supervisor"
            );

            intelligenceSupervisor.tell(new IntelligenceSupervisorActor.Initialize(
                    msg.apiKey,
                    msg.apiProvider
            ));

            Thread.sleep(500);
            logger.info("Intelligence actors initialization complete");

        } catch (Exception e) {
            logger.error("Failed to initialize intelligence actors", e);
        }

        return this;
    }

    private Behavior<Command> onCreateSession(CreateSession cmd) {
        logger.info("Received request to create session for user: {}", cmd.userId);
        sessionManager.tell(new SessionManagerActor.CreateSession(cmd.userId, cmd.replyTo));
        return this;
    }

    // CORRECTED: onRouteQuery with new signature
    private Behavior<Command> onRouteQuery(RouteQuery cmd) {
        logger.info("Routing query for session: {}", cmd.sessionId);

        // Pass 3 separate parameters to RouteToSession
        sessionManager.tell(new SessionManagerActor.RouteToSession(
                cmd.sessionId,
                cmd.query,
                cmd.replyTo
        ));

        return this;
    }

    private Behavior<Command> onShutdown(Shutdown cmd) {
        logger.info("Shutting down SupervisorActor and all children");
        return Behaviors.stopped();
    }

    public ActorRef<IntelligenceSupervisorActor.Command> getIntelligenceSupervisor() {
        return intelligenceSupervisor;
    }
}