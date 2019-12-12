package io.rml.framework

import java.io.File
import java.util.Properties

import io.rml.framework.util.server.TestData
import kafka.server.{KafkaConfig, KafkaServerStartable}
import org.apache.curator.test.TestingServer
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

import scala.concurrent.ExecutionContextExecutor


/**
  * <p>Copyright 2019 IDLab (Ghent University - imec)</p>
  *
  * @author Gerald Haesendonck
  */
class KafkaStreamTestSync extends StreamTestSync {
  // initialise zookeeper, but do not start yet.
  var zookeeper: TestingServer = _
  var kafka: KafkaServerStartable = _
  var admin: AdminClient = _
  var client:  KafkaProducer[String, String] = _


  override def testFolder: String = "stream/kafka"

  override def passingTests: Array[(String, String)] = Array(
    (testFolder, "noopt"),                // standard streaming tests
    ("stream/kafka_json_ld", "json-ld")   // test with json-ld as output
  )

  override def setup(): Unit = {
    super.setup()
    logInfo("Starting Zookeeper...")
    zookeeper = new TestingServer(
      getZkPort(),
      new File(getTempDir(), "kafkazookeeper")
    )
    logInfo("Zookeeper started.")

    logInfo("Starting Kafka...")
    val config = new KafkaConfig(serverProperties)
    kafka = new KafkaServerStartable(config)
    kafka.startup()
    logInfo("Kafka started.")

    logInfo("Creating Kafka admin client...")
    admin = AdminClient.create(serverProperties)
    logInfo("Kafka admin client created.")

    logInfo("Creating Kafka client...")
    client = new KafkaProducer[String, String](producerProps)
    logInfo("Creating Kafka client created.")
  }


  override def beforeTestCase(): Unit = {
    // topics seem to be created automatically...

    /*logInfo("Creating Kafka input topic...")
    val topicName = "demo"
    val numPartitions = 1
    val replicationFactor = 1
    val topic = new NewTopic(topicName, numPartitions, replicationFactor.toShort)
    val createTopicsResult = admin.createTopics(ArrayBuffer(topic).asJava)
    createTopicsResult.all().get()  // wait for completion of creating topics
    Thread.sleep(2000)
    logInfo("Creating Kafka input topic done.") */
  }

  override def afterTestCase(): Unit = {
    logInfo("Deleting Kafka input topic(s)...")
    val topics = admin.listTopics().names().get()
    val deleteResults = admin.deleteTopics(topics)
    deleteResults.all().get()
    Thread.sleep(2000)  // wait a bit for the topic to be *really* deleted!
    logInfo("Kafka input topic(s) deleted.")
  }

  override def teardown(): Unit = {
    logInfo("Stopping Kafka admin client...")
    if (admin != null) {
      admin.close()
    }
    logInfo("Kafka admin client stopped.")

    logInfo("Shutting down Kafka...")
    if (kafka != null) {
      kafka.shutdown()
      kafka.awaitShutdown()
    }
    logInfo("Kafka stopped.")

    logInfo("Shutting down Zookeeper")
    if (zookeeper != null) {
      zookeeper.close()
    }
    logInfo("Zookeeper stopped.")
  }


  override def writeData(input: List[TestData])(implicit executur: ExecutionContextExecutor): Unit = {
    for (batch <- input) {

      val topic = if (input.size > 1)
        batch.filename.replaceAll("\\..*", "")
      else
        "demo"

      for (in <- batch.data) {
        logInfo(s"Writing record to Kafka: [${in}]")
        val result = client.send(new ProducerRecord[String, String](topic, in))
        result.get()  // wait until data is sent.
      }
    }
  }

  /////////////////////////
  // some helper methods //
  /////////////////////////
  private def serverProperties: Properties = {
    val props = new Properties()
    props.put("zookeeper.connect", "localhost:2181")
    props.put("bootstrap.servers", "localhost:9092")
    props.put("broker.id", "1")
    props.put("port", "9092")
    props.put("log.dir", new File(getTempDir(), "kafka").getAbsolutePath)
    props.put("host.name", "localhost")
    props.put("delete.topic.enable", "true")
    props.put("offsets.topic.replication.factor", "1")  // by default it will expect 3
    props.put("offsets.topic.num.partitions", "1")  // by default it will expect 3
    props
  }

  private def producerProps: Properties = {
    val props = new Properties()
    props.put("bootstrap.servers", "localhost:9092")
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("acks", "1")
    props
  }

  private def getZkPort: Int = {
    val inetAddress = serverProperties.getProperty("zookeeper.connect")
    if (inetAddress == null || inetAddress.isEmpty || !inetAddress.contains(":")) {
      throw new IllegalArgumentException("The given connection address in string is invalid.")
    }
    inetAddress.split(":")(1).toInt
  }
}

object KafkaStreamTestSync {

}
