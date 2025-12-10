package com.diplomatic.actors.infrastructure;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;
import akka.cluster.typed.Cluster;
import akka.cluster.typed.Subscribe;
import akka.cluster.ClusterEvent;
import com.diplomatic.actors.intelligence.IntelligenceNodeSupervisor;
import com.diplomatic.messages.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scala.collection.Iterator;

/**
 * Cluster-Aware Supervisor for Node 1 (Infrastructure)
 */
public class ClusterSupervisorActor extends AbstractBehavior<ClusterSupervisorActor.Command> {

    private final Logger logger = LoggerFactory.getLogger(ClusterSupervisorActor.class);
    private final Cluster cluster;
    private final ActorRef<SessionManagerActor.Command> sessionManager;
    private boolean clusterReady = false;

    private ActorRef<RouteToClassifierMessage> discoveredClassifier;
    private ActorRef<CulturalAnalysisRequestMessage> discoveredCultural;
    private ActorRef<DiplomaticPrimitiveRequestMessage> discoveredPrimitives;

    public static final ServiceKey<Command> SUPERVISOR_KEY =
            ServiceKey.create(Command.class, "cluster-supervisor");

    // ========================================================================
    // COMMAND MESSAGES
    // ========================================================================

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
        public final String sessionId;
        public final String query;
        public final ActorRef<String> replyTo;

        public RouteQuery(String sessionId, String query, ActorRef<String> replyTo) {
            this.sessionId = sessionId;
            this.query = query;
            this.replyTo = replyTo;
        }
    }

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

    private static final class ActorsRegistered implements Command {
        public final Receptionist.Listing listing;
        public ActorsRegistered(Receptionist.Listing listing) {
            this.listing = listing;
        }
    }

    // ========================================================================
    // CONSTRUCTOR
    // ========================================================================

    public static Behavior<Command> createInfrastructure() {
        return Behaviors.setup(ClusterSupervisorActor::new);
    }

    private ClusterSupervisorActor(ActorContext<Command> context) {
        super(context);
        this.cluster = Cluster.get(context.getSystem());

        context.getSystem().receptionist().tell(
                Receptionist.register(SUPERVISOR_KEY, context.getSelf())
        );

        this.sessionManager = context.spawn(
                SessionManagerActor.create(),
                "session-manager"
        );

        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║  NODE 1: Infrastructure Supervisor Started              ║");
        System.out.println("║  Actors: SessionManager, ConversationHistory             ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        logger.info("ClusterSupervisor registered with receptionist");
        logger.info("SessionManager spawned as regular actor");
    }

    // ========================================================================
    // MESSAGE HANDLERS
    // ========================================================================

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(MonitorCluster.class, this::onMonitorCluster)
                .onMessage(ActorsRegistered.class, this::onActorsRegistered)
                .onMessage(CreateSession.class, this::onCreateSession)
                .onMessage(RouteQuery.class, this::onRouteQuery)
                .onMessage(ClusterEventMessage.class, this::onClusterEvent)
                .onMessage(ClusterReachabilityChange.class, this::onReachabilityChange)
                .build();
    }

    private Behavior<Command> onMonitorCluster(MonitorCluster cmd) {
        ActorRef<ClusterEvent.MemberEvent> memberEventAdapter = getContext()
                .messageAdapter(ClusterEvent.MemberEvent.class, ClusterEventMessage::new);

        cluster.subscriptions().tell(Subscribe.create(memberEventAdapter, ClusterEvent.MemberEvent.class));

        ActorRef<ClusterEvent.ReachabilityEvent> reachabilityAdapter = getContext()
                .messageAdapter(ClusterEvent.ReachabilityEvent.class, ClusterReachabilityChange::new);

        cluster.subscriptions().tell(Subscribe.create(reachabilityAdapter, ClusterEvent.ReachabilityEvent.class));

        logger.info("Cluster monitoring activated");
        logger.info("Current cluster state: {}", cluster.state());
        logger.info("Current members in cluster:");

        Iterator<akka.cluster.Member> members = cluster.state().members().iterator();
        while (members.hasNext()) {
            akka.cluster.Member member = members.next();
            logger.info("   - {} [{}]", member.uniqueAddress(), member.roles());
        }

        return this;
    }

    private Behavior<Command> onActorsRegistered(ActorsRegistered msg) {
        if (msg.listing.isForKey(IntelligenceNodeSupervisor.CLASSIFIER_KEY)) {
            var instances = msg.listing.getServiceInstances(IntelligenceNodeSupervisor.CLASSIFIER_KEY);
            if (!instances.isEmpty()) {
                discoveredClassifier = instances.iterator().next();
                logger.info("✅ Classifier actor discovered: {}", discoveredClassifier);
            }
        } else if (msg.listing.isForKey(IntelligenceNodeSupervisor.CULTURAL_KEY)) {
            var instances = msg.listing.getServiceInstances(IntelligenceNodeSupervisor.CULTURAL_KEY);
            if (!instances.isEmpty()) {
                discoveredCultural = instances.iterator().next();
                logger.info("✅ Cultural actor discovered: {}", discoveredCultural);
            }
        } else if (msg.listing.isForKey(IntelligenceNodeSupervisor.PRIMITIVES_KEY)) {
            var instances = msg.listing.getServiceInstances(IntelligenceNodeSupervisor.PRIMITIVES_KEY);
            if (!instances.isEmpty()) {
                discoveredPrimitives = instances.iterator().next();
                logger.info("✅ Primitives actor discovered: {}", discoveredPrimitives);
            }
        }

        if (discoveredClassifier != null && discoveredCultural != null && discoveredPrimitives != null) {
            logger.info("🔗 All intelligence actors discovered - configuring SessionManager");
            sessionManager.tell(new SessionManagerActor.SetIntelligenceActors(
                    discoveredClassifier, discoveredCultural, discoveredPrimitives));
        }

        return this;
    }

    private Behavior<Command> onClusterEvent(ClusterEventMessage msg) {
        logger.info("Cluster event: {}", msg.event.getClass().getSimpleName());

        if (msg.event instanceof ClusterEvent.MemberUp) {
            ClusterEvent.MemberUp memberUp = (ClusterEvent.MemberUp) msg.event;
            logger.info("✅ Member UP: {} with roles {}",
                    memberUp.member().uniqueAddress(), memberUp.member().roles());

            int memberCount = cluster.state().members().size();
            logger.info("Total members in cluster: {}", memberCount);

            if (memberCount >= 2 && !clusterReady) {
                clusterReady = true;
                System.out.println("\n🎉 CLUSTER READY! " + memberCount + " nodes connected");
                logger.info("Infrastructure node operational");
                logger.info("Subscribing to actor registrations...");

                ActorRef<Receptionist.Listing> adapter = getContext()
                        .messageAdapter(Receptionist.Listing.class, ActorsRegistered::new);

                getContext().getSystem().receptionist().tell(
                        Receptionist.subscribe(IntelligenceNodeSupervisor.CLASSIFIER_KEY, adapter));
                getContext().getSystem().receptionist().tell(
                        Receptionist.subscribe(IntelligenceNodeSupervisor.CULTURAL_KEY, adapter));
                getContext().getSystem().receptionist().tell(
                        Receptionist.subscribe(IntelligenceNodeSupervisor.PRIMITIVES_KEY, adapter));
            }
        } else if (msg.event instanceof ClusterEvent.MemberJoined) {
            ClusterEvent.MemberJoined joined = (ClusterEvent.MemberJoined) msg.event;
            logger.info("Member JOINED: {}", joined.member().uniqueAddress());
        } else if (msg.event instanceof ClusterEvent.MemberRemoved) {
            ClusterEvent.MemberRemoved removed = (ClusterEvent.MemberRemoved) msg.event;
            logger.warn("Member REMOVED: {}", removed.member().uniqueAddress());
            clusterReady = false;
        }

        return this;
    }

    private Behavior<Command> onReachabilityChange(ClusterReachabilityChange msg) {
        if (msg.event instanceof ClusterEvent.UnreachableMember) {
            ClusterEvent.UnreachableMember unreachable = (ClusterEvent.UnreachableMember) msg.event;
            logger.warn("Node UNREACHABLE: {}", unreachable.member().uniqueAddress());
        } else if (msg.event instanceof ClusterEvent.ReachableMember) {
            ClusterEvent.ReachableMember reachable = (ClusterEvent.ReachableMember) msg.event;
            logger.info("Node REACHABLE: {}", reachable.member().uniqueAddress());
        }
        return this;
    }

    private Behavior<Command> onCreateSession(CreateSession cmd) {
        if (!clusterReady) {
            logger.warn("Cluster not ready yet for session creation");
            cmd.replyTo.tell(new SessionCreatedMessage("pending", cmd.userId));
            return this;
        }

        logger.info("Routing session creation to SessionManager for user: {}", cmd.userId);
        sessionManager.tell(new SessionManagerActor.CreateSession(cmd.userId, cmd.replyTo));
        return this;
    }

    private Behavior<Command> onRouteQuery(RouteQuery cmd) {
        if (!clusterReady) {
            logger.warn("Cluster not ready yet for query routing");
            cmd.replyTo.tell("System initializing, please wait...");
            return this;
        }

        logger.info("Routing query to SessionManager for session: {}", cmd.sessionId);

        sessionManager.tell(new SessionManagerActor.RouteToSession(
                cmd.sessionId,
                cmd.query,
                cmd.replyTo
        ));

        return this;
    }
}