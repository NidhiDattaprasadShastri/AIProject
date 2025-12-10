package com.diplomatic.actors.infrastructure;

import akka.cluster.typed.Subscribe;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;
import akka.cluster.typed.Cluster;
import akka.cluster.ClusterEvent;
import com.diplomatic.actors.intelligence.IntelligenceNodeSupervisor;
import com.diplomatic.messages.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scala.collection.Iterator;

/**
 * Cluster-Aware Supervisor for Node 1 (Infrastructure)
 * CRITICAL FIX: Removed ClusterSingleton - it was causing serialization issues!
 */
public class ClusterSupervisorActor extends AbstractBehavior<ClusterSupervisorActor.Command> {

    private final Logger logger = LoggerFactory.getLogger(ClusterSupervisorActor.class);
    private final Cluster cluster;
    private final ActorRef<SessionManagerActor.Command> sessionManager;
    private boolean clusterReady = false;

    // Discovered intelligence actors
    private ActorRef<RouteToClassifierMessage> discoveredClassifier;
    private ActorRef<CulturalAnalysisRequestMessage> discoveredCultural;
    private ActorRef<DiplomaticPrimitiveRequestMessage> discoveredPrimitives;

    // Service key for client discovery
    public static final ServiceKey<Command> SUPERVISOR_KEY =
            ServiceKey.create(Command.class, "cluster-supervisor");

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

    public static Behavior<Command> createInfrastructure() {
        return Behaviors.setup(ClusterSupervisorActor::new);
    }

    private ClusterSupervisorActor(ActorContext<Command> context) {
        super(context);
        this.cluster = Cluster.get(context.getSystem());

        // Register with receptionist
        context.getSystem().receptionist().tell(
                Receptionist.register(SUPERVISOR_KEY, context.getSelf())
        );

        // CRITICAL FIX: Spawn SessionManager as regular child actor, NOT a singleton!
        this.sessionManager = context.spawn(
                SessionManagerActor.create(),
                "session-manager"
        );

        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║  NODE 1: Infrastructure Supervisor Started                ║");
        System.out.println("║  Actors: SessionManager (NO SINGLETON), ConversationHistory║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println("📡 Registered ClusterSupervisor with receptionist");
        System.out.println("✅ SessionManager spawned as regular actor (NO serialization boundary)");
    }

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

        System.out.println("📡 Cluster monitoring activated");
        System.out.println("🔍 Current cluster state: " + cluster.state());

        System.out.println("📊 Current members in cluster:");
        Iterator<akka.cluster.Member> members = cluster.state().members().iterator();
        while (members.hasNext()) {
            akka.cluster.Member member = members.next();
            System.out.println("   - " + member.uniqueAddress() + " [" + member.roles() + "]");
        }

        return this;
    }

    private Behavior<Command> onActorsRegistered(ActorsRegistered msg) {
        if (msg.listing.isForKey(IntelligenceNodeSupervisor.CLASSIFIER_KEY)) {
            var instances = msg.listing.getServiceInstances(IntelligenceNodeSupervisor.CLASSIFIER_KEY);
            if (!instances.isEmpty()) {
                discoveredClassifier = instances.iterator().next();
                System.out.println("✅ VERIFIED: Classifier actor registered in cluster receptionist");
                System.out.println("   Location: " + discoveredClassifier);
            }
        } else if (msg.listing.isForKey(IntelligenceNodeSupervisor.CULTURAL_KEY)) {
            var instances = msg.listing.getServiceInstances(IntelligenceNodeSupervisor.CULTURAL_KEY);
            if (!instances.isEmpty()) {
                discoveredCultural = instances.iterator().next();
                System.out.println("✅ VERIFIED: Cultural actor registered in cluster receptionist");
                System.out.println("   Location: " + discoveredCultural);
            }
        } else if (msg.listing.isForKey(IntelligenceNodeSupervisor.PRIMITIVES_KEY)) {
            var instances = msg.listing.getServiceInstances(IntelligenceNodeSupervisor.PRIMITIVES_KEY);
            if (!instances.isEmpty()) {
                discoveredPrimitives = instances.iterator().next();
                System.out.println("✅ VERIFIED: Primitives actor registered in cluster receptionist");
                System.out.println("   Location: " + discoveredPrimitives);
            }
        }

        // Check if all three are discovered and configure SessionManager
        if (discoveredClassifier != null && discoveredCultural != null && discoveredPrimitives != null) {
            System.out.println("🔗 All intelligence actors discovered, configuring SessionManager...");
            sessionManager.tell(new SessionManagerActor.SetIntelligenceActors(
                    discoveredClassifier,
                    discoveredCultural,
                    discoveredPrimitives
            ));
            System.out.println("✅ Intelligence actors sent to SessionManager");
        }

        return this;
    }

    private Behavior<Command> onClusterEvent(ClusterEventMessage msg) {
        System.out.println("🔔 Received cluster event: " + msg.event.getClass().getSimpleName());

        if (msg.event instanceof ClusterEvent.MemberUp) {
            ClusterEvent.MemberUp memberUp = (ClusterEvent.MemberUp) msg.event;
            System.out.println("✅ Member UP: " + memberUp.member().uniqueAddress() + " with roles " + memberUp.member().roles());

            int memberCount = cluster.state().members().size();

            System.out.println("🔍 Total members in cluster: " + memberCount);

            if (memberCount >= 2 && !clusterReady) {
                clusterReady = true;
                System.out.println("🎉 CLUSTER READY! " + memberCount + " nodes connected");
                System.out.println("✓ Infrastructure node operational");
                System.out.println("🔍 Subscribing to actor registrations...");

                ActorRef<Receptionist.Listing> receptionistAdapter = getContext()
                        .messageAdapter(Receptionist.Listing.class, ActorsRegistered::new);

                getContext().getSystem().receptionist().tell(
                        Receptionist.subscribe(IntelligenceNodeSupervisor.CLASSIFIER_KEY, receptionistAdapter)
                );
                getContext().getSystem().receptionist().tell(
                        Receptionist.subscribe(IntelligenceNodeSupervisor.CULTURAL_KEY, receptionistAdapter)
                );
                getContext().getSystem().receptionist().tell(
                        Receptionist.subscribe(IntelligenceNodeSupervisor.PRIMITIVES_KEY, receptionistAdapter)
                );
            }

        } else if (msg.event instanceof ClusterEvent.MemberRemoved) {
            ClusterEvent.MemberRemoved removed = (ClusterEvent.MemberRemoved) msg.event;
            System.out.println("⚠️  Member REMOVED: " + removed.member().uniqueAddress());
            clusterReady = false;
        } else if (msg.event instanceof ClusterEvent.MemberJoined) {
            ClusterEvent.MemberJoined joined = (ClusterEvent.MemberJoined) msg.event;
            System.out.println("👋 Member JOINED: " + joined.member().uniqueAddress());
        } else {
            System.out.println("ℹ️  Other cluster event: " + msg.event);
        }

        return this;
    }

    private Behavior<Command> onReachabilityChange(ClusterReachabilityChange msg) {
        if (msg.event instanceof ClusterEvent.UnreachableMember) {
            ClusterEvent.UnreachableMember unreachable = (ClusterEvent.UnreachableMember) msg.event;
            System.out.println("❌ Node UNREACHABLE: " + unreachable.member().uniqueAddress());
        } else if (msg.event instanceof ClusterEvent.ReachableMember) {
            ClusterEvent.ReachableMember reachable = (ClusterEvent.ReachableMember) msg.event;
            System.out.println("✅ Node REACHABLE again: " + reachable.member().uniqueAddress());
        }

        return this;
    }

    private Behavior<Command> onCreateSession(CreateSession cmd) {
        if (!clusterReady) {
            System.out.println("⏳ Cluster not ready yet, session creation delayed");
            cmd.replyTo.tell(new SessionCreatedMessage("pending", cmd.userId));
            return this;
        }

        System.out.println("➡️  Routing session creation to SessionManager: " + cmd.userId);
        sessionManager.tell(new SessionManagerActor.CreateSession(cmd.userId, cmd.replyTo));
        return this;
    }

    private Behavior<Command> onRouteQuery(RouteQuery cmd) {
        if (!clusterReady) {
            System.out.println("⏳ Cluster not ready, query rejected");
            cmd.queryMessage.getReplyTo().tell("System initializing, please wait...");
            return this;
        }

        System.out.println("➡️  Routing query to SessionManager for session: " + cmd.queryMessage.getSessionId());
        sessionManager.tell(new SessionManagerActor.RouteToSession(cmd.queryMessage));
        return this;
    }
}