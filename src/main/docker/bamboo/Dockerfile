FROM atlassian/bamboo-server:7.1.2

LABEL description="Bamboo pre-configured for Artemis"

USER root
ENV DEBIAN_FRONTEND noninteractive

RUN apt-get update
RUN apt-get install -y maven
RUN apt-get install -y software-properties-common apt-utils
RUN add-apt-repository -y ppa:linuxuprising/java
RUN echo debconf shared/accepted-oracle-license-v1-2 select true | debconf-set-selections && echo debconf shared/accepted-oracle-license-v1-2 seen true | debconf-set-selections
RUN apt-get install -y oracle-java15-installer
RUN update-alternatives --install /usr/bin/java java /usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java 1
RUN update-alternatives --set java /usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java
# Add file in /artemis/bin/mvn that uses the correct java version and passes all arguments to mvn
RUN mkdir /artemis && mkdir /artemis/bin && printf '#!/bin/bash\nJAVA_HOME=/usr/lib/jvm/java-15-oracle /usr/bin/mvn "$@"\n' > /artemis/bin/mvn && chmod 777 /artemis/bin/mvn

USER ${BAMBOO_USER}
