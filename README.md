vmwareConDiag
=============

Diagnose VMware connection for OpenNMS VMware Integration

1. git checkout
2. mvn clean install assembly:assembly
3. Create config.properties with following content

host="vcenter-ip"
user="vcenter-user"
pass="vcenter-pass"

4. The config.properties has to be in the same folder like the runnable jar you can find it in the target directory
5. Run the programm with 
  java -jar vmwareConDiag-1.0-SNAPSHOT-jar-with-dependencies.jar
