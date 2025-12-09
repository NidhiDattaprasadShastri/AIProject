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

# Check if API key is set (optional for Node 1)
if [ -z "$LLM_API_KEY" ]; then
    echo "⚠️  Warning: LLM_API_KEY not set"
    echo "Set it with: export LLM_API_KEY='your-key'"
    echo ""
fi

# Compile if needed
echo "📦 Compiling project..."
mvn compile -q

# Run Node 1
echo "🚀 Starting Node 1..."
mvn exec:java \
    -Dexec.mainClass="com.diplomatic.Node1App" \
    -Dconfig.file=src/main/resources/application-node1.conf \
    -Dexec.cleanupDaemonThreads=false

echo ""
echo "Node 1 stopped."