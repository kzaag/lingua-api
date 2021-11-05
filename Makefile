
all: build-jar run-jar

libs:
	wget https://github.com/pemistahl/lingua/releases/download/v1.1.0/lingua-1.1.0-with-dependencies.jar -O lib/lingua.jar
	wget https://search.maven.org/remotecontent?filepath=org/json/json/20210307/json-20210307.jar -O lib/json.jar

build:
	javac -cp lib/lingua.jar:lib/json.jar  Program.java

build-jar: build
	jar cvfm lingua-api.jar Manifest *.class

run-jar:
	java -jar lingua-api.jar

podman-image: build-jar
	podman build -t lingua-api .
docker-image: build-jar
	docker build -t lingua-api .
