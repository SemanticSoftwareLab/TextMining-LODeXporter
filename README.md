[![Build Status](http://assistant.cs.concordia.ca:8080/job/TextMining-LODeXporter/badge/icon)](http://assistant.cs.concordia.ca:8080/job/TextMining-LODeXporter/)  [![License: LGPL v3](https://img.shields.io/badge/License-LGPL%20v3-blue.svg)](https://www.gnu.org/licenses/lgpl-3.0)

# TextMining-LODeXporter

The LODeXporter is a [GATE](https://gate.ac.uk/ "General Architecture for Text Engineering (GATE)") component that can export NLP annotations directly to a triplestore, with configurable vocabularies, for use in LOD applications. 

## Compiling

Configure the location of your GATE installation in `build.properties`. The default `ant` builds the plugin and Javadoc documentation, `ant test` runs the unit tests.

## Running

Add LODeXporter to your pipeline. By default, it reads the mapping rules from a file (see `resources/mapping.rdf` for a basic example) and exports the generated RDF triples in N-Quads format into the output file.

For more details on working with GATE PRs, please refer to the [GATE User Guide](https://gate.ac.uk/sale/tao/split.html).