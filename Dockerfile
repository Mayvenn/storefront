FROM java:8u91-jre

COPY storefront.jar storefront.jar

USER nobody
CMD java -server -Xmx768m -XX:MaxMetaspaceSize=64m -Xss1m -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -jar storefront.jar
