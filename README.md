# PG2RDF: A Framework for Converting Property Graphs to RDF Using X3ML

**PG2RDF** is a framework designed for the transformation of property graphs, stored in Neo4j, into RDF representations using the X3ML mapping language. The pipeline supports graph datasets and enables structured data export in compliance with semantic web standards.

## Overview

The PG2RDF pipeline follows a multi-step process:

1. Data Ingestion: Load property graph datasets from Neo4j.
2. Schema and Data Extraction: Extract PG schema in XSD.
3. Populate XML data from the discovered schema.
4. Transformation to RDF: Convert the XML representation into RDF triples using the X3ML Engine.
5. Output: RDF triples are exported in `.trig` format for use in knowledge graph applications or semantic integration.

## Requirements

- Java 11+
- Scala 2.12+
- Apache Spark (optional, for parallel processing)
- Neo4j Community Edition 4.4.0
- X3ML Engine (`x3ml-engine.jar`)

## Dataset Preparation

Datasets must first be imported into Neo4j. Three datasets are currently supported:

- FIB25 (NeuPrint)
- MB6 (NeuPrint)
- LDBC SNB (Social Network Benchmark)

Example shell commands for dataset loading are included in:

- `run_pg2rdf_fib.sh`
- `run_pg2rdf_mb6.sh`
- `run_pg2rdf_ldbc.sh`

Each script utilizes the `neo4j-admin import` tool. Below is an indicative example for FIB25 (please replace the dataset path with your own):

## Dataset Preparation

The project includes **evaluation datasets** (FIB25, LDBC, MB6) that need to be unzipped and loaded into Neo4j.

### 1. Unzip the Datasets 

```
cd datasets
unzip FIB25/preprocessed_fib25_neo4j_inputs.zip
unzip LDBC/ldbc_neo4j_inputs1.zip
unzip LDBC/ldbc_neo4j_inputs2.zip
unzip LDBC/ldbc_neo4j_inputs3.zip
unzip LDBC/ldbc_neo4j_inputs4.zip
unzip MB6/preprocessed_mb6_neo4j_inputs.zip
```

### 2. Load Datasets into Neo4j

Before importing, ensure that:

1. Neo4j is stopped if currently running.
2. `NEO4J_DIR` is set to your Neo4j installation directory.
3. `current_dataset_dir` is set to the path containing the dataset CSV files.

**General Preparation Steps:**

```
cd $NEO4J_DIR
bin/neo4j stop   # Stop Neo4j if running
rm -rf $NEO4J_DIR/data/databases/neo4j  # Delete old database if needed

export current_dataset_dir=<path-to-datasets>
```

---

### **LDBC Dataset Import (using '|' delimiter)**

```
$NEO4J_DIR/bin/neo4j-admin import --database=neo4j --delimiter='|' \
    --nodes=Forum="$current_dataset_dir/forum_0_0.csv" \
    --nodes=Person="$current_dataset_dir/person_0_0.csv" \
    --nodes=Post="$current_dataset_dir/post_0_0.csv" \
    --nodes=Place="$current_dataset_dir/place_0_0.csv" \
    --nodes=Organisation="$current_dataset_dir/organisation_0_0.csv" \
    --nodes=TagClass="$current_dataset_dir/tagclass_0_0.csv" \
    --nodes=Tag="$current_dataset_dir/tag_0_0.csv" \
    --relationships=CONTAINER_OF="$current_dataset_dir/forum_containerOf_post_0_0.csv" \
    --relationships=HAS_MEMBER="$current_dataset_dir/forum_hasMember_person_0_0.csv" \
    --relationships=HAS_MODERATOR="$current_dataset_dir/forum_hasModerator_person_0_0.csv" \
    --relationships=HAS_TAG="$current_dataset_dir/forum_hasTag_tag_0_0.csv" \
    --relationships=HAS_INTEREST="$current_dataset_dir/person_hasInterest_tag_0_0.csv" \
    --relationships=IS_LOCATED_IN="$current_dataset_dir/person_isLocatedIn_place_0_0.csv" \
    --relationships=KNOWS="$current_dataset_dir/person_knows_person_0_0.csv" \
    --relationships=LIKES="$current_dataset_dir/person_likes_post_0_0.csv" \
    --relationships=STUDIES_AT="$current_dataset_dir/person_studyAt_organisation_0_0.csv" \
    --relationships=WORKS_AT="$current_dataset_dir/person_workAt_organisation_0_0.csv" \
    --relationships=HAS_CREATOR="$current_dataset_dir/post_hasCreator_person_0_0.csv" \
    --relationships=HAS_TAG="$current_dataset_dir/post_hasTag_tag_0_0.csv" \
    --relationships=IS_LOCATED_IN="$current_dataset_dir/post_isLocatedIn_place_0_0.csv" \
    --relationships=IS_LOCATED_IN="$current_dataset_dir/organisation_isLocatedIn_place_0_0.csv" \
    --relationships=IS_PART_OF="$current_dataset_dir/place_isPartOf_place_0_0.csv" \
    --relationships=HAS_TYPE="$current_dataset_dir/tag_hasType_tagclass_0_0.csv" \
    --relationships=IS_SUBCLASS_OF="$current_dataset_dir/tagclass_isSubclassOf_tagclass_0_0.csv"
```

---

### **MB6 Dataset Import (using ',' delimiter)**

```
$NEO4J_DIR/bin/neo4j-admin import --database=neo4j --delimiter=',' \
    --nodes=Meta="$current_dataset_dir/Neuprint_Meta_mb6.csv" \
    --nodes=Neuron="$current_dataset_dir/Neuprint_Neurons_mb6.csv" \
    --relationships=CONNECTS_TO="$current_dataset_dir/Neuprint_Neuron_Connections_mb6.csv" \
    --nodes=SynapseSet="$current_dataset_dir/Neuprint_SynapseSet_mb6.csv" \
    --relationships=CONNECTS_TO="$current_dataset_dir/Neuprint_SynapseSet_to_SynapseSet_mb6.csv" \
    --relationships=CONTAINS="$current_dataset_dir/Neuprint_Neuron_to_SynapseSet_mb6.csv" \
    --nodes=Synapse="$current_dataset_dir/Neuprint_Synapses_mb6.csv" \
    --relationships=SYNAPSES_TO="$current_dataset_dir/Neuprint_Synapse_Connections_mb6.csv" \
    --relationships=CONTAINS="$current_dataset_dir/Neuprint_SynapseSet_to_Synapses_mb6.csv"
```

---

### **FIB25 Dataset Import (using ',' delimiter)**

```
$NEO4J_DIR/bin/neo4j-admin import --database=neo4j --delimiter=',' \
    --nodes=Meta="$current_dataset_dir/Neuprint_Meta_fib25.csv" \
    --nodes=Neuron="$current_dataset_dir/Neuprint_Neurons_fib25.csv" \
    --relationships=CONNECTS_TO="$current_dataset_dir/Neuprint_Neuron_Connections_fib25.csv" \
    --nodes=SynapseSet="$current_dataset_dir/Neuprint_SynapseSet_fib25.csv" \
    --relationships=CONNECTS_TO="$current_dataset_dir/Neuprint_SynapseSet_to_SynapseSet_fib25.csv" \
    --relationships=CONTAINS="$current_dataset_dir/Neuprint_Neuron_to_SynapseSet_fib25.csv" \
    --nodes=Synapse="$current_dataset_dir/Neuprint_Synapses_fib25.csv" \
    --relationships=SYNAPSES_TO="$current_dataset_dir/Neuprint_Synapse_Connections_fib25.csv" \
    --relationships=CONTAINS="$current_dataset_dir/Neuprint_SynapseSet_to_Synapses_fib25.csv"
```

---

## Post-Import Steps

1. **Verify the Import**:
   ```
   $NEO4J_DIR/bin/neo4j-admin check-consistency --database=neo4j
   ```

2. **Start the Neo4j Server**:
   ```
   cd $NEO4J_DIR
   bin/neo4j start
   ```
   
3. **Access the Neo4j Browser**:  
   Open [http://localhost:7474](http://localhost:7474) to visualize the graph and run queries.

---

## Notes

1. **Delimiters**:  
   - Use `|` for **LDBC** dataset.  
   - Use `,` for **MB6** and **FIB25** datasets.

2. **CSV Format Requirements**:  
   - Nodes must have a unique `id` field.
   - Relationships must have `:START_ID`, `:END_ID`, and `:TYPE` columns.
   
3. **Reloading Data**:  
   If you need to re-import, remove the database first:
   ```
   rm -rf $NEO4J_DIR/data/databases/neo4j
   ```

---

## Neo4j Configuration

The default connection port in the Scala-based `Dataloader` is set to `7687`, the standard Neo4j Bolt port. Users wishing to modify this can adjust the configuration accordingly within the source code.

## Transformation to RDF

You can download X3ML engine .jar from [here](https://github.com/isl/x3ml/releases)
Ensure that the path to `x3ml-engine.jar` is valid. If necessary, modify the relative path in the main class accordingly.

```scala
X3MLBatchRunner.runX3MLBatch(
  inputFolder     = "output_xml",
  x3mlMapping     = "output_mappings.x3ml",
  policyFile      = "generator-policies.xml",
  x3mlEngineJar   = "../../x3ml-engine.jar",
  outputFolder    = "output_trig"
)
```

## Project Structure

This repository contains the full pipeline for converting Property Graph datasets from Neo4j to RDF using X3ML. Below is an overview of the main directories and files included in the project.

```
PG2RDF/
├── datasets/                          # Input CSV datasets to be imported into Neo4j
├── output/                            # Output folders for XML and RDF results
├── pg2rdf/                            # Internal project structure (can contain build artifacts)
│   └── .bloop/                        # Bloop build server configuration (if used)
├── project/                           # SBT project configuration
├── output_trig/                       # RDF output (default folder)
├── output_trig_fib/                   # RDF output for FIB25 dataset
├── output_trig_ldbc/                  # RDF output for LDBC dataset
├── output_trig_mb6/                   # RDF output for MB6 dataset
├── output_xml/                        # Intermediate XML generated from Neo4j
├── src/
│   └── main/
│       └── scala/                            # Scala source code for PG2RDF
│           ├── DataLoader.scala              # Loads data from Neo4j from PG-HIVE
│           ├── Evaluation.scala              # Evaluates the extracted schemas from PG-HIVE
│           ├── InferTypes.scala              # Infers types from data from PG-HIVE
│           ├── LSHClustering.scala           # Clustering based on LSH from PG-HIVE
│           ├── Main.scala                    # Main
│           ├── PatternPreprocessing.scala    # Preprocessing steps for patterns from PG-HIVE
│           ├── PGSchemaExporterLoose.scala   # Schema export with loose type rules from PG-HIVE
│           ├── PGSchemaExporterStrict.scala  # Schema export with strict type rules from PG-HIVE
│           ├── PostProcessing.scala          # Post-processing from PG-HIVE
│           ├── X3MLBatchRunner.scala         # Wrapper for running X3ML Engine
│           ├── XSD2X3MLGenerator.scala       # Converts XSD to X3ML mapping rules
│           ├── XSDExporter.scala             # Exports XSD schema from graph structure
│           └── XSDToXMLGenerator.scala       # Exports XML based on schema and data
├── target/                            # SBT build output
├── build.sbt                          # SBT build definition
├── generator-policies.xml           # default X3ML policy file
├── output_mappings.x3ml             # Autogenerated X3ML mapping rules
├── pg_schema_output_loose.txt       # Exported loose schema summary
├── pg_schema_output_strict.txt      # Exported strict schema summary
├── schema_output.xsd                # Autogenerated XSD schema file
├── .gitignore                       # Git ignored files
├── README.md                        # Project README
├── run_pg2rdf_fib.sh                # Load and process FIB25 dataset
├── run_pg2rdf_ldbc.sh               # Load and process LDBC dataset
└── run_pg2rdf_mb6.sh                # Load and process MB6 dataset
```

## Installation

To install and set up **PG2RDF**, follow these steps:

### 1. Clone the Repository from here https://github.com/sophisid/PG2RDF.git

### 2. Build the Project

Navigate to the **`pg2rdf`** directory and build the project using `sbt` (Scala Build Tool):

```
cd pg2rdf
sbt compile
```

## Usage

Once the setup is complete and the datasets are loaded, you can run **PG2RDF** to perform schema discovery.

**PG2RDF** can be executed in multiple ways:

### Option 1: Using the Automated Script

Scripts (e.g., `run_pg2rdf_ldbc.sh`, `run_pg2rdf_fib.sh`, `run_pg2rdf_mb6.sh`) are provided to automate the entire process:

1. Set environment variables and directories inside these scripts.
2. Make them executable:
   ```
   chmod +x run_pg2rdf_ldbc.sh
   chmod +x run_pg2rdf_fib.sh
   chmod +x run_pg2rdf_mb6.sh
   ```
3. Run the script for the desired dataset:
   ```
   ./run_pg2rdf_ldbc.sh
   ./run_pg2rdf_fib.sh
   ./run_pg2rdf_mb6.sh
   ```

The scripts handle:
- Removing and re-extracting Neo4j.
- Importing data.
- Create various test cases, with 0%-50%-100% usage of labels.
- Running schema discovery (LSH clustering).
- Stopping Neo4j and cleaning up.
(Note: that you need to change the folder paths in the forst lines of the scripts)

### Option 2: Manual Execution

If you prefer more control, follow the manual steps:
1. Stop Neo4j, remove old database, re-extract Neo4j.
2. Import the desired dataset with `neo4j-admin import`.
3. Start Neo4j.
4. Run:
   ```
   cd pg2rdf
   sbt run  
   ```
5. Stop Neo4j and clean up if needed.

#### ⚠️ Testing locally 
If you are testing PG2RDF on a local machine or with a large dataset, make sure to add a LIMIT clause in the loadAllNodes and loadAllRelationships methods inside DataLoader.scala to avoid excessive memory consumption or long execution times.

## Notes

- The framework supports intermediate nodes to represent relationships with properties.
- All components are modular and can be replaced or extended for other domains or datasets.

---

## License

This project is licensed under the Apache License 2.0. See the [LICENSE](./LICENSE) file for details.

---
