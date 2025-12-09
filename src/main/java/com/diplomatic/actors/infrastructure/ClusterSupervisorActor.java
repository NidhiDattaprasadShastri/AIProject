package com.diplomatic.actors.infrastructure;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.cluster.typed.Cluster;
import akka.cluster.typed.ClusterSingleton;
import akka.cluster.typed.SingletonActor;
import akka.cluster.typed.Subscribe;
import akka.cluster.ClusterEvent;
import com.diplomatic.messages.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cluster-Aware Supervisor for Node 1 (Infrastructure)
 *
 * PROJECT REQUIREMENT: Akka Cluster supervisor managing distributed actors
 *
 * Demonstrates:
 * - Cluster membership monitoring
 * - Actor deployment on specific cluster nodes
 * - Cluster-aware message routing
 */
public class ClusterSupervisorActor extends AbstractBehavior<ClusterSupervisorActor.Command> {

    private final Logger logger = LoggerFactory.getLogger(ClusterSupervisorActor.class);
    private final Cluster cluster;
    private final ActorRef<SessionManagerActor.Command> sessionManager;
    private boolean clusterReady = false;

    public interface Command {}

    public static final class MonitorCluster implements Command {}

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

    // Internal message for cluster events
    private static final class ClusterEventMessage implements Command {
        public final ClusterEvent.MemberEvent event;
        public ClusterEventMessage(ClusterEvent.MemberEvent event) {
            this.event = event;
        }
    }

    private static final class ClusterReachabilityChange implements Command {
        public final ClusterEvent.ReachabilityEvent event;
        public ClusterReachabilityChange(ClusterEvent.ReachabilityEvent event) {
            this.event = event;
        }
    }

    public static Behavior<Command> createInfrastructure() {
        return Behaviors.setup(ClusterSupervisorActor::new);
    }

    private ClusterSupervisorActor(ActorContext<Command> context) {
        super(context);
        this.cluster = Cluster.get(context.getSystem());

        // Spawn SessionManager as cluster singleton (only one across cluster)
        ClusterSingleton singleton = ClusterSingleton.get(context.getSystem());
        this.sessionManager = singleton.init(
                SingletonActor.of(SessionManagerActor.create(), "session-manager")
                        .withStopMessage(SessionManagerActor.Shutdown.INSTANCE)  // ✅ FIXED: Use singleton instance
        );

        logger.info("╔════════════════════════════════════════════════════════════╗");
        logger.info("║  NODE 1: Infrastructure Supervisor Started                ║");
        logger.info("║  Actors: SessionManager, ConversationHistory               ║");
        logger.info("╚════════════════════════════════════════════════════════════╝");
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(MonitorCluster.class, this::onMonitorCluster)
                .onMessage(CreateSession.class, this::onCreateSession)
                .onMessage(RouteQuery.class, this::onRouteQuery)
                .onMessage(ClusterEventMessage.class, this::onClusterEvent)
                .onMessage(ClusterReachabilityChange.class, this::onReachabilityChange)
                .build();
    }

    private Behavior<Command> onMonitorCluster(MonitorCluster cmd) {
        // Subscribe to cluster membership events
        ActorRef<ClusterEvent.MemberEvent> memberEventAdapter = getContext()
                .messageAdapter(ClusterEvent.MemberEvent.class, ClusterEventMessage::new);

        cluster.subscriptions().tell(Subscribe.create(memberEventAdapter, ClusterEvent.MemberEvent.class));

        // Subscribe to reachability events
        ActorRef<ClusterEvent.ReachabilityEvent> reachabilityAdapter = getContext()
                .messageAdapter(ClusterEvent.ReachabilityEvent.class, ClusterReachabilityChange::new);

        cluster.subscriptions().tell(Subscribe.create(reachabilityAdapter, ClusterEvent.ReachabilityEvent.class));

        logger.info("📡 Cluster monitoring activated");
        logger.info("🔍 Current cluster state: {}", cluster.state());

        return this;
    }

    private Behavior<Command> onClusterEvent(ClusterEventMessage msg) {
        if (msg.event instanceof ClusterEvent.MemberUp) {
            ClusterEvent.MemberUp memberUp = (ClusterEvent.MemberUp) msg.event;
            logger.info("✅ Member UP: {} with roles {}",
                    memberUp.member().address(),
                    memberUp.member().roles());

            // Check if we have both nodes up
            long memberCount = cluster.state().members().size();
            if (memberCount >= 2 && !clusterReady) {
                clusterReady = true;
                logger.info("🎉 CLUSTER READY! {} nodes connected", memberCount);
                logger.info("✓ Infrastructure node operational");
            }

        } else if (msg.event instanceof ClusterEvent.MemberRemoved) {
            ClusterEvent.MemberRemoved removed = (ClusterEvent.MemberRemoved) msg.event;
            logger.warn("⚠️  Member REMOVED: {}", removed.member().address());
            clusterReady = false;
        }

        return this;
    }

    private Behavior<Command> onReachabilityChange(ClusterReachabilityChange msg) {
        if (msg.event instanceof ClusterEvent.UnreachableMember) {
            ClusterEvent.UnreachableMember unreachable = (ClusterEvent.UnreachableMember) msg.event;
            logger.error("❌ Node UNREACHABLE: {}", unreachable.member().address());
        } else if (msg.event instanceof ClusterEvent.ReachableMember) {
            ClusterEvent.ReachableMember reachable = (ClusterEvent.ReachableMember) msg.event;
            logger.info("✅ Node REACHABLE again: {}", reachable.member().address());
        }

        return this;
    }

    private Behavior<Command> onCreateSession(CreateSession cmd) {
        if (!clusterReady) {
            logger.warn("⏳ Cluster not ready yet, session creation delayed");
            cmd.replyTo.tell(new SessionCreatedMessage("pending", cmd.userId));
            return this;
        }

        logger.info("➡️  Routing session creation to SessionManager: {}", cmd.userId);
        sessionManager.tell(new SessionManagerActor.CreateSession(cmd.userId, cmd.replyTo));
        return this;
    }

    private Behavior<Command> onRouteQuery(RouteQuery cmd) {
        if (!clusterReady) {
            logger.warn("⏳ Cluster not ready, query rejected");
            cmd.queryMessage.getReplyTo().tell("System initializing, please wait...");
            return this;
        }

        logger.info("➡️  Routing query to SessionManager for session: {}",
                cmd.queryMessage.getSessionId());
        sessionManager.tell(new SessionManagerActor.RouteToSession(cmd.queryMessage));
        return this;
    }
}