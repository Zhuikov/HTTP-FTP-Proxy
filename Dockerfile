FROM gradle:jdk8

WORKDIR /Proxy_HTTP_FTP

ADD --chown=gradle . .

ENTRYPOINT gradle test
