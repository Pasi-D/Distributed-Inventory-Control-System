# Mini Distributed Inventory Control System 

This is an inventory system built using apache zookeeper to demonstrate a mini distributed system with two phase commit.

### Pre-Requisites

- [Oracle Java 8](https://www.oracle.com/java/technologies/javase/javase8-archive-downloads.html)
- [Maven](https://maven.apache.org/)
- [Apache Zookeeper 3.6.2](https://zookeeper.apache.org/doc/r3.6.2/releasenotes.html)

### Demonstrating the mini distributed system

1. Make sure whether zookeeper is running.

   In Zookeeper directory you can,
   ```bash
   ./bin/zkServer.sh start conf/zoo_sample.cfg
   ```
   To check whether Zookeeper is running (On windows)

   ```bash
   netstat -ano | findStr "2181"
   ```
   To stop the zookeeper, in zookeeper directory,
   
   ```bash
   ./bin/zkServer.sh stop conf/zoo_sample.cfg
   ```
2. To run each node, in `inventory-control-server` directory.

   ```bash
   java -jar target/inventory-control-server-1.0.0-jar-with-dependencies.jar <port>
   ```
   
3. Then you can invoke client commands through `inventory-control-client` directory.

   ```bash
   java -jar target/inventory-control-client-1.0.0-jar-with-dependencies.jar <host> <port> <command>
   ```

