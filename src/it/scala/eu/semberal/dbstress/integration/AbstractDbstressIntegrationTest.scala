package eu.semberal.dbstress.integration

import java.io.{File, FilenameFilter, InputStreamReader}
import java.lang.System._
import java.nio.file.Files._

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestKitBase}
import eu.semberal.dbstress.Orchestrator
import eu.semberal.dbstress.config.ConfigParser._
import eu.semberal.dbstress.util.{CsvResultsExport, JsonResultsExport, ResultsExport}
import org.scalatest.{BeforeAndAfterAll, Suite}

import scala.concurrent.duration.DurationLong

trait AbstractDbstressIntegrationTest extends TestKitBase with BeforeAndAfterAll  {
  this: Suite =>

  override implicit lazy val system: ActorSystem = ActorSystem()

  protected val jsonFilter = new FilenameFilter {
    override def accept(dir: File, name: String): Boolean = name.endsWith(".json")
  }

  protected val csvFilter = new FilenameFilter {
    override def accept(dir: File, name: String): Boolean = name.endsWith(".csv")
  }

  override protected def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  protected def executeTest(configFile: String): File = {
    val tmpDir = createTempDirectory(s"dbstress_OrchestratorTest_${currentTimeMillis()}_").toFile
    val reader = new InputStreamReader(getClass.getClassLoader.getResourceAsStream(configFile))
    val config = parseConfigurationYaml(reader, Some("")).right.get
    val exports: List[ResultsExport] = new JsonResultsExport(tmpDir) :: new CsvResultsExport(tmpDir) :: Nil
    new Orchestrator(exports).run(config, system)
    system.awaitTermination(20.seconds)
    tmpDir
  }
}
