FROM openjdk:8-jre

RUN wget https://github.com/jmxtrans/jmxtrans-agent/releases/download/jmxtrans-agent-1.2.4/jmxtrans-agent-1.2.4.jar
COPY storefront.jar storefront.jar
COPY container_files /

USER nobody
CMD exec java -javaagent:/jmxtrans-agent-1.2.4.jar=/jmxtrans-agent.xml -server -Xmx1024m -XX:MaxMetaspaceSize=128m -Xss1m -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -jar storefront.jar
