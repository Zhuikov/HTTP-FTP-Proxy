FROM gradle:jdk8

WORKDIR /home/gradle

ENV hostFtpServerName="ftp-server"
ADD --chown=gradle . .

ENTRYPOINT gradle test
