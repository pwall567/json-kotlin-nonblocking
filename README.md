# json-kotlin-nonblocking

Non-blocking JSON serialization for Kotlin

This library allows a JSON object to be serialized to a non-blocking stream, for example a `ByteWriteChannel`.
It is designed to be used in conjunction with the [co-pipelines](https://github.com/pwall567/co-pipelines.git) library,
as follows:
```kotlin
fun serializeToByteChannel(obj: Any?, channel: ByteWriteChannel) {
    val pipeline = CoEncoderFactory.getEncoder(Charsets.UTF_8, ByteChannelCoAcceptor(channel))
    pipeline.outputJSON(obj)
}
```
In this example, the `getEncoder()` function will create a non-blocking pipeline that takes characters (the stringified
JSON), converts them to a UTF-8 byte stream and pipes it to the channel.

This a very much a work in progress; stay tuned for more developments (and with any luck, more documentation).

## Dependency Specification

The latest version of the library is 0.4, and it may be obtained from the Maven Central repository.

### Maven
```xml
    <dependency>
      <groupId>net.pwall.json</groupId>
      <artifactId>json-kotlin-nonblocking</artifactId>
      <version>0.4</version>
    </dependency>
```
### Gradle
```groovy
    implementation 'net.pwall.json:json-kotlin-nonblocking:0.4'
```
### Gradle (kts)
```kotlin
    implementation("net.pwall.json:json-kotlin-nonblocking:0.4")
```

Peter Wall

2020-05-01
