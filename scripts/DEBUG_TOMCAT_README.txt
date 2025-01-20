root : [ 20:54 ] # systemctl status tomcat | grep config | cut -d "-" -f4
Djava.util.logging.config.file=/opt/tomcat/latest/conf/logging.properties

which tells me the installed tomcat has /oot/tomcat as it's base

Go there

root : [ 20:54 ] # cd /opt/tomcat/latest/bin/
root : [ 20:55 ] # vi setenv.sh

and then add

CATALINA_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n"

exit and save and then

root : [ 20:55 ] systemctl restart tomcat

and now from your IDE you can remote debug on port 8000.




