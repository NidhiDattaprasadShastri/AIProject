#!/bin/bash

#######################################################################
# Start Node 1 - Infrastructure/Frontend Node
# Port: 2551
# Roles: infrastructure, frontend
#######################################################################

echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║       STARTING NODE 1 (Infrastructure)                        ║"
echo "║       Port: 2551                                              ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo ""

# Compile if needed
echo "📦 Compiling project..."
mvn compile -q

# Run Node 1
echo "🚀 Starting Node 1..."
echo ""
echo "⚠️  NOTE: Node 1 will run continuously."
echo "         Start Node 2 in another terminal, then use a third terminal for queries."
echo ""

mvn exec:java \
    -Dexec.mainClass="com.diplomatic.Node1App" \
    -Dexec.cleanupDaemonThreads=false

echo ""
echo "Node 1 stopped."