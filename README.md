Simple TCP Server
-----------------------------------
##Pre-requisites to build code

1. JDK 11
2. Maven 3.6.x

##Basic info of the folder structure

    - bin               # Executables used when building docker image
    - checkstyle        # Checkstly code linting rules
    - src/main          # All the source code
    - src/tests         # All unit tests
    - src/it            # All integration tests
    - Dockerfile        # Used to build docker image
    - pom.xml           # Maven pom file
    - README.md         # Info about the project
    - start-local.sh    # Shell script to start the server locally

## Build TCP Server

The below command will run unit tests and integration tests.
Checkstyle rules are applied to both source and test cases.

    mvn clean install

### Build by skipping all tests
If you are in a hurry and want to skip all tests -

    mvn clean install -DskipTests

### Build by skipping integration tests
If you want to skip only the integration tests -

    mvn clean install -DskipITs 

### Debugging integration Test cases
if you are using Intellij, go any integration test file, scroll to intended test method. Right click on that test method, to
run or debug.

## Running the TCP server
Assuming you are in the root folder of the project, simply run the below command.
On success, both the application log (simple-tcp-server.log) and "numbers.log" to the same directory.

If you want to alter the location of "numbers.log",
change the value of the variable "NUMBERS_LOG_FILE" in "start-locally.sh" file.

    ./start-locally.sh

#### Running the docker container
As given below, the commands will build the image and only then you can run.

Note: Since the container writes into numbers.log, the below command is asking you to mount a directory which the log
files can be written. It also forwards port 4000 to the host.

On success, both the application log (simple-tcp-server.log) and "numbers.log" will be written to the mounted directory.

    docker build -t example:1.0 .
    docker run -p 4000:4000 -v <my-directoryt-to-be-mounted>:/var/log/example example:1.0

#### Running via maven or IDE

    1. Via intellij IDE, go to plugins -> spring-boot->spring-boot:run
    2. Via command client, run command "mvn install spring-boot:run"
    
## To get unit and integration test coverage

The service uses Jacoco to track coverage. A consolidated reported is generated and its ready for access.

    1. The unit test report is available at "<my-project>/target/site/jacoco/jacoco-ut/index.html"
    2. The integration test report is available at "<my-project>/target/site/jacoco/jacoco-it/index.html"

