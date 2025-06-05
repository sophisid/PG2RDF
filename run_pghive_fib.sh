#!/bin/bash

ROOT_DIR="/mnt/fast/sophisid"
PROJECT_DIR="/mnt/fast/sophisid/PG2RDF"
DATASETS_DIR="$PROJECT_DIR/datasets/FIB25"
PG2RDF_DIR="$PROJECT_DIR/pg2rdf"
NEO4J_TAR="neo4j-community.tar.gz"
NEO4J_VERSION="neo4j-community-4.4.0"
NEO4J_DIR="$NEO4J_VERSION"
OUTPUT_BASE_DIR="$PROJECT_DIR/output"
NEO4J_PORT=7687

mkdir -p "$OUTPUT_BASE_DIR"

    current_dataset_dir="$DATASETS_DIR/"

# Step 1: Remove the current Neo4j database directory
echo "Removing $NEO4J_VERSION directory..."
rm -rf "$NEO4J_DIR"

# Step 2: Extract Neo4j from the tar.gz
echo "Extracting Neo4j from $NEO4J_TAR..."
tar -xzvf "$NEO4J_TAR"

# Step 3: Import the CSV files into Neo4j
    echo "Importing data into Neo4j from $current_dataset_dir..."
"$NEO4J_DIR/bin/neo4j-admin" import --database=neo4j --delimiter=',' \
    --nodes=Meta="$current_dataset_dir/Neuprint_Meta_fib25.csv" \
    --nodes=Neuron="$current_dataset_dir/Neuprint_Neurons_fib25.csv" \
    --relationships=CONNECTS_TO="$current_dataset_dir/Neuprint_Neuron_Connections_fib25.csv" \
    --nodes=SynapseSet="$current_dataset_dir/Neuprint_SynapseSet_fib25.csv" \
    --relationships=CONNECTS_TO="$current_dataset_dir/Neuprint_SynapseSet_to_SynapseSet_fib25.csv" \
    --relationships=CONTAINS="$current_dataset_dir/Neuprint_Neuron_to_SynapseSet_fib25.csv" \
    --nodes=Synapse="$current_dataset_dir/Neuprint_Synapses_fib25.csv" \
    --relationships=SYNAPSES_TO="$current_dataset_dir/Neuprint_Synapse_Connections_fib25.csv" \
    --relationships=CONTAINS="$current_dataset_dir/Neuprint_SynapseSet_to_Synapses_fib25.csv"

    # Step 4: Start Neo4j
    echo "Starting Neo4j..."
    "$NEO4J_DIR/bin/neo4j" start

    # Wait for Neo4j to start
    echo "Waiting for Neo4j to start..."
    sleep 100  


    echo "Running PG2RDF..."
    cd "$PG2RDF_DIR"
    sbt run > "$OUTPUT_BASE_DIR/output_PG2RDF_${dataset}.txt"
    cd "$ROOT_DIR"

    # Step 7: Stop Neo4j
    echo "Stopping Neo4j..."
    "$NEO4J_DIR/bin/neo4j" stop
    sleep 100  

    # Step 8: Dynamically kill any process on port 7687
    echo "Checking and killing any process on port $NEO4J_PORT..."
    PID=$(lsof -t -i :$NEO4J_PORT)

    if [ -z "$PID" ]; then
        echo "No process found on port $NEO4J_PORT."
    else
        echo "Killing process with PID: $PID"
        kill -9 $PID
        if [ $? -eq 0 ]; then
            echo "Process $PID successfully killed."
        else
            echo "Failed to kill process $PID."
        fi
    fi

    echo "Finished processing dataset: $dataset"
    echo ""
done

echo "All datasets have been processed."
