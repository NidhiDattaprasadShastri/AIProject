package com.diplomatic.messages;

/**
 * Marker interface for Jackson CBOR serialization in Akka Cluster
 * All messages passed between cluster nodes must implement this interface
 */
public interface CborSerializable {
}