## Cybersecurity And Privacy Open Reference Datasets IN Oscal (CAPORDINO)

Development of tooling to allow conversion of datasets managed by the [Cybersecurity and Privacy Reference Tool (CPRT)](https://csrc.nist.gov/projects/cprt) into [OSCAL](https://www.nist.gov/OSCAL) formats. 

Currently, the command line tool supports generating catalogs for: 
- CSF v2.0
- SP 800-171 Rev 3
- SP 800-218 (SSDF) Version 1.1
- SP 800-66 Rev 2
- SP 800-172 and 172A 1.0.0 (combined into one catalog)
- SP 800-172 and 172A Rev 3 (combined into one catalog)
- AI RMF 1.0

### Building
#### Clone the git repository
```bash
git clone https://github.com/usnistgov/capordino.git
cd capordino/
```

#### Container environment
If you would like to develop/run this tool in a Docker container (easier management of dependencies), follow these steps. Otherwise, skip to [Installing Maven](#installing-maven).
* Install [Docker Desktop](https://www.docker.com/products/docker-desktop/)
* Install [Visual Studio Code](https://code.visualstudio.com/) and its Dev Containers extension 
* Press F1 to open Command Palette - Dev Containers: Open Folder in Container
* Choose your cloned capordino repository
* Open a new Terminal within Visual Studio Code
* All subsequent commands should be run in this terminal


#### Installing Maven
The tool requires [Apache Maven](https://maven.apache.org/) version 3.9 or greater.
1. Check if JDK 8 or above is installed (requirement for Maven 3.9+)
```bash
java --version
```
2. If necessary, download the latest [JDK](https://www.oracle.com/java/technologies/downloads/) for your operating system.

3. Follow these [instructions](https://maven.apache.org/install.html) to install Maven.

#### Installing Capordino
1. Use Maven to install dependencies and build
```bash
mvn install
```

2. A shell script `capordino.sh` is provided to simplify running the Capordino tool. Change file permissions to make it executable.
```bash
chmod u+x capordino.sh
```

### Running Capordino CLI

#### Basic template to run capordino.sh
Use --help to see the available options.
`./capordino.sh --help`
```
Usage: capordino [-hV] [-f=<filepath>] [-o=<output_directory>] <framework 
                 version identifier>
      <framework version identifier>
                  REQUIRED: framework version identifier to build catalog for
                  Implemented: CSF_2_0_0, SP_800_171_3_0_0, SP_800_218_1_1_0,
                    SP800_66_2_0_0, SP_800_172_1_0_0, SP_800_172_3_0_0,
                    AI_100_1_0_0
  -f, --file-path=<filepath>
                  File path for framework json, if already downloaded from CPRT
  -h, --help      Show this help message and exit.
  -o, --output-directory=<output_directory>
                  Directory for capordino tool output (built catalog), default
                    is "./catalogs/"
  -V, --version   Print version information and exit.
```

#### Default usage
```shell
./capordino.sh -o "catalogs/nist.gov/CSF/" "CSF_2_0_0"
```

#### Output
The built catalog is written to specified directory or "catalogs/" by default.

#### Specify output directory

```shell
./capordino.sh -o "catalogs/nist.gov/CSF/" "CSF_2_0_0"
```