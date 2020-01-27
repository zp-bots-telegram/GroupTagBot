FROM java:8-alpine
MAINTAINER Zack Pollard <zackpollard@ymail.com>

VOLUME /java/data/
WORKDIR /java/data/

ADD target/bot.jar /java/app.jar

CMD java -jar ../app.jar