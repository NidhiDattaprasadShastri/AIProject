#!/bin/bash

#######################################################################
# Start Node 2 - Intelligence/Backend Node
# Port: 2552
# Roles: intelligence, backend
#######################################################################

echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║       STARTING NODE 2 (Intelligence)                          ║"
echo "║       Port: 2552                                              ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo ""

# Check if API key is set (REQUIRED for Node 2)
if [ -z "$LLM_API_KEY" ]; then
    echo "❌ ERROR: LLM_API_KEY environment variable not set"
    echo "Node 2 requires an API key for Claude integration"
    echo ""
    echo "Set it with:"
    echo "  export LLM_API_KEY='your-anthropic-api-key'"
    echo ""
    exit 1
fi

echo "✓ LLM_API_KEY configured"
echo "✓ Provider: CLAUDE"
echo ""

# Compile if needed
echo "📦 Compiling project..."
mvn compile -q

# Run Node 2
echo "🚀 Starting Node 2..."
mvn exec:java \
    -Dexec.mainClass="com.diplomatic.Node2App" \
    -Dconfig.file=src/main/resources/application-node2.conf \
    -Dexec.cleanupDaemonThreads=false

echo ""
echo "Node 2 stopped."