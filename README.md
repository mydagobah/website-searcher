# Website Searcher
A simple web crawler written in pure Java 8

### Description
Given a list of urls and a regex search term, decide whether each url content matches the regex.
1. Regex should be case insensitive;
2. Concurrent searching but http connections should be limited;
3. Results are writtenout to a file `results.txt`.

### How to run
```bash
./gradlew jar

java -jar build/libs/website_searcher-1.0.jar
```

### Things to improve
1. Use logging tools like [Slf4j](https://www.slf4j.org/) to replace standard output and support different log levels
2. Use dependency injection like [Google Guice](https://github.com/google/guice/wiki/Motivation) to replace `new` usages;
3. Support more search terms
4. Support more encoding types
5. Better handling of transient issues like timeouts via retries
