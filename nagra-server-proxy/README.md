Nagra Proxy Server
==================

Copy the file from the resources folder to /data/config/nagra-server/nagra-games-lookup.txt

How to Build & Run
==================
mvn clean package
cd target
nohup java -Xmx1g -Xms1g -Dnagra-games-file=/data/config/nagra-server/nagra-games-lookup.txt -jar nagra-server.jar localhost 5001 &