# PG2RDF: Schema-Guided Transformation of Property Graphs to RDF

**PG2RDF** is a framework for transforming Property Graphs (PG), stored in Neo4j, into RDF representations using a schema-guided approach.

It integrates:
- PG-HIVE for schema discovery
- X3ML Engine and RMLMapper for RDF transformation

enabling automated, structured, and semantically consistent conversion of graph data into Knowledge Graphs.

---

## Requirements

- Java 11+
- Scala 2.12+
- Apache Spark
- Neo4j (Community Edition recommended)
- X3ML Engine (`x3ml-engine.jar`)
- RMLMapper (`rmlmapper.jar`)

---

## Installation

### 1. Clone the Repository

```bash
git clone https://github.com/sophisid/PG2RDF.git
cd PG2RDF
````

### 2. Build the Project

```bash
cd pg2rdf
sbt compile
```

---

## Usage

After setting up Neo4j and ensuring your dataset is loaded:

```bash
cd pg2rdf
sbt run
```

The pipeline will:

* extract schema from the graph
* generate mappings
* produce XML
* convert to RDF
* store results in the output directories

---

## Testing & Benchmark

To evaluate **PG2RDF**, we recommend using the datasets in **PG-SB benchmark**.

* Repository: [https://github.com/sophisid/PG-SB](https://github.com/sophisid/PG-SB)
* DOI: [https://doi.org/10.5281/zenodo.17801335](https://doi.org/10.5281/zenodo.17801335)

---

## Transformation to RDF

### Using X3ML

```scala
X3MLBatchRunner.runX3MLBatch(
  inputFolder     = "output_xml",
  x3mlMapping     = "output_mappings.x3ml",
  policyFile      = "generator-policies.xml",
  x3mlEngineJar   = "path/to/x3ml-engine.jar",
  outputFolder    = "output_trig"
)
```

### Using RML

```scala
RMLBatchRunner.runRMLBatch(
   spark,
   inputFolder = "output_xml",
   rmlMapping = "output_mappings_rml.ttl",
   rmlMapperJar = "path/to/rmlmapper.jar",
   outputFolder = "output_trig_rml",
   numPartitions = 2
)
```

---

## Citation

To cite our work, please use:

```bibtex
@inproceedings{DBLP:conf/jist/SideriEPK25,
  author       = {Sophia Sideri and
                  Vasilis Efthymiou and
                  Dimitris Plexousakis and
                  Haridimos Kondylakis},
  title        = {{PG2RDF:} Schema-Guided Transformation of Property Graphs to {RDF}},
  booktitle    = {Knowledge Graphs - 14th International Joint Conference, {IJCKG} 2025,
                  Heraklion, Crete, Greece, October 15-17, 2025, Proceedings},
  series       = {Lecture Notes in Computer Science},
  pages        = {358--373},
  publisher    = {Springer},
  year         = {2025},
  url          = {https://doi.org/10.1007/978-981-95-5009-8\_24},
  doi          = {10.1007/978-981-95-5009-8\_24},
  biburl       = {https://dblp.org/rec/conf/jist/SideriEPK25.bib}
}
```
