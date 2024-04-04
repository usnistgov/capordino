## Cybersecurity And Privacy Open Reference Datasets IN Oscal (CAPORDINO)

Development of tooling to allow conversion of datasets managed by the [Cybersecurity and Privacy Reference Tool (CPRT)](https://csrc.nist.gov/projects/cprt) into [OSCAL](https://www.nist.gov/OSCAL) formats. 

Currently, the command line tool supports creating CSF 2.0 catalog. 

### Building
The tool requires [Apache Maven](https://maven.apache.org/) version 3.8.4 or greater.

#### Instructions on installing Maven

1. Ensure you have JDK 8 or above (requirement for Maven 3.9+)
```bash
java --version
```
2. If not, download the latest [JDK] (https://www.oracle.com/java/technologies/downloads/) for your operating system 
Then, follow these [instructions] (https://maven.apache.org/install.html) to install Maven 

#### Instructions on installing Capordino tool
1. Clone the git repository
```bash
git clone https://github.com/usnistgov/capordino.git
cd ./capordino/
```

2. Maven to install dependencies and build
```bash
mvn install
```

3. Make capordino.sh executable
A shell script, capordino.sh, is provided to simplify running the Capordino tool.
```bash
chmod u+x capordino.sh
```

### Running Capordino CLI
Use --help to see the available options.

`./capordino.sh --help`
Usage: capordino [-hV] <framework version identifier> [COMMAND]
      <framework version identifier>

  -h, --help      Show this help message and exit.
  -V, --version   Print version information and exit.
Commands:
  gui  Runs capordino GUI instead of CLI


#### Template
./capordino.sh <arguments>

Arguments:
<framework version identifier> - Framework version to build a catalog for, written as a string

```bash
./capordino.sh "CSF_2_0_0"
```


Output:
Catalog is written to "src/test/resources"