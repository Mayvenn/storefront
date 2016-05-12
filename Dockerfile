FROM java:8u66-jre

COPY storefront.jar storefront.jar

USER nobody
CMD java -jar storefront.jar -Djava.awt.headless=true -Xmx768m -XX:+UseConcMarkSweepGC
