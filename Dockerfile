FROM gradle:jdk8

WORKDIR /home/gradle

ENV HOST_FTP_SERVER_NAME="localhost"
ADD --chown=gradle . .

ENTRYPOINT gradle test
