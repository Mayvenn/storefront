FROM java:8u91-jre

COPY storefront.jar storefront.jar

USER nobody
CMD java -jar storefront.jar -Djava.awt.headless=true -Xmx384m -XX:+UseConcMarkSweepGC
