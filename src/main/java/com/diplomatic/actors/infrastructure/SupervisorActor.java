package com.diplomatic.actors.infrastructure;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.SupervisorStrategy;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.diplomatic.messages.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;

public class SupervisorActor extends AbstractBehavior<SupervisorActor.Command> {

    private final Logger logger = LoggerFactory.getLogger(SupervisorActor.class);
    private final ActorRef<SessionManagerActor.Command> sessionManager;
    public interface Command {}

    public static final class CreateSession implements Command {
        public final String userId;
        public final ActorRef<SessionCreatedMessage> replyTo;
        public CreateSession(String userId, ActorRef<SessionCreatedMessage> replyTo) {
            this.userId = userId;
            this.replyTo = replyTo;
        }
    }

    public static final class RouteQuery implements Command {
        public final UserQueryMessage queryMessage;
        public RouteQuery(UserQueryMessage queryMessage) {
            this.queryMessage = queryMessage;
        }
    }

    public static final class Shutdown implements Command {
        public static final Shutdown INSTANCE = new Shutdown();
        private Shutdown() {}
    }

    private SupervisorActor(ActorContext<Command> context) {
        super(context);
        this.sessionManager = context.spawn(
                Behaviors.supervise(SessionManagerActor.create()).onFailure(SupervisorStrategy.restart()
                                        .withLimit(3, Duration.ofMinutes(1))),
                "session-manager");
        logger.info("SupervisorActor initialized with SessionManager");
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(SupervisorActor::new);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(CreateSession.class, this::onCreateSession)
                .onMessage(RouteQuery.class, this::onRouteQuery)
                .onMessage(Shutdown.class, this::onShutdown)
                .build();
    }

    private Behavior<Command> onCreateSession(CreateSession cmd) {
        logger.info("Received request to create session for user: {}", cmd.userId);
        sessionManager.tell(new SessionManagerActor.CreateSession(cmd.userId, cmd.replyTo));
        return this;
    }
    private Behavior<Command> onRouteQuery(RouteQuery cmd) {
        logger.info("Routing query for session: {}", cmd.queryMessage.getSessionId());
        sessionManager.tell(new SessionManagerActor.RouteToSession(cmd.queryMessage));
        return this;
    }
    private Behavior<Command> onShutdown(Shutdown cmd) {
        logger.info("Shutting down SupervisorActor and all children");
        return Behaviors.stopped();
    }
}
