FROM clojure:openjdk-8-lein

WORKDIR /app
ADD project.clj /app/project.clj
ARG AWS_ACCESS_KEY_ID
ARG AWS_SECRET_ACCESS_KEY
RUN lein deps

ADD . /app
CMD exec lein trampoline test
