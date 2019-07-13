package io.evvo.island

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
import java.util.Calendar

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill}
import akka.event.{LoggingAdapter, LoggingReceive}
import akka.pattern.ask
import akka.util.Timeout
import io.evvo.agent._
import io.evvo.island.population._
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{Await, Future}

/** This component is used to do all the actual work of managing the island, without managing
  * or being tied to where the island is deployed to.
  */
private class EvvoIsland[Sol]
(
  creators: Vector[CreatorFunction[Sol]],
  mutators: Vector[ModifierFunction[Sol]],
  deletors: Vector[DeletorFunction[Sol]],
  fitnesses: Vector[Objective[Sol]],
  immigrationStrategy: ImmigrationStrategy,
  emigrationStrategy: EmigrationStrategy)
(implicit log: LoggingAdapter)
  extends EvolutionaryProcess[Sol] {

  /** Serialize and deserialize the given value, returning the deserialized data.
    * Roundtripping like this allows all Islands to catch (some) serialization bugs before
    * deploying remotely.
    *
    * @param t The value to deserialize
    * @tparam T The type of the value to deserialize
    * @return The value after the roundtrip
    */
  private def serializationRoundtrip[T](t: T): T = {
    val baos = new ByteArrayOutputStream()
    val outputStream = new ObjectOutputStream(baos)
    outputStream.writeObject(t)

    val bais = new ByteArrayInputStream(baos.toByteArray)
    val inputStream = new ObjectInputStream(bais)
    val deserializedObject = inputStream.readObject()
    val deserializedT = deserializedObject.asInstanceOf[T]

    deserializedT
  }

  private val pop: Population[Sol] = StandardPopulation(fitnesses.map(serializationRoundtrip))
  private val creatorAgents = creators.map(c => CreatorAgent(serializationRoundtrip(c), pop))
  private val mutatorAgents = mutators.map(m => ModifierAgent(serializationRoundtrip(m), pop))
  private val deletorAgents = deletors.map(d => DeletorAgent(serializationRoundtrip(d), pop))

  /** The list of all other islands, to send emigrating solutions to. */
  private var emigrationTargets: Seq[EvolutionaryProcess[Sol]] = Seq()
  /** The index of the current "target" that will receive the next emigration. */
  private var currentEmigrationTargetIndex: Int = 0

  override def runAsync(stopAfter: StopAfter): Future[Unit] = {
    Future {
      log.info(s"Island running with stopAfter=${stopAfter}")

      creatorAgents.foreach(_.start())
      mutatorAgents.foreach(_.start())
      deletorAgents.foreach(_.start())

      val startTime = Calendar.getInstance().toInstant.toEpochMilli
      while (startTime + stopAfter.time.toMillis >
        Calendar.getInstance().toInstant.toEpochMilli) {
        Thread.sleep(500)
        val pareto = pop.getParetoFrontier()
        this.emigrate()
        log.info(f"pareto = ${pareto}")
      }

      log.info(f"c=${startTime}, now=${Calendar.getInstance().toInstant.toEpochMilli}")
      stop()
    }
  }

  def runBlocking(stopAfter: StopAfter): Unit = {
    Await.result(this.runAsync(stopAfter), Duration.Inf)
  }

  override def currentParetoFrontier(): ParetoFrontier[Sol] = {
    pop.getParetoFrontier()
  }

  override def immigrate(solutions: Seq[Scored[Sol]]): Unit = {
    pop.addSolutions(immigrationStrategy.filter(solutions, pop).map(_.solution))
  }

  override def poisonPill(): Unit = {
    stop()
  }

  override def registerIslands(islands: Seq[EvolutionaryProcess[Sol]]): Unit = {
    emigrationTargets = emigrationTargets ++ islands
  }

  private def stop(): Unit = {
    creatorAgents.foreach(_.stop())
    mutatorAgents.foreach(_.stop())
    deletorAgents.foreach(_.stop())
  }

  private def emigrate(): Unit = {
    if (emigrationTargets.isEmpty) {
      log.info("Trying to emigrate without any emigration targets")
    } else {
      val emigrants = emigrationStrategy.chooseSolutions(this.pop)

      // Hardcoded to round robin for now - will be updated when we add network topology.
      this.emigrationTargets(currentEmigrationTargetIndex).immigrate(emigrants)
      currentEmigrationTargetIndex = (currentEmigrationTargetIndex + 1) % emigrationTargets.length
    }
  }
}

object EvvoIsland {
  /** @tparam Sol the type of solutions processed by this island.
    * @return A builder for an EvvoIsland.
    */
  def builder[Sol](): UnfinishedEvvoIslandBuilder[Sol, _, _, _, _] = EvvoIslandBuilder[Sol]()
}


// =================================================================================================
// Local EvvoIsland wrapper

/** An island that can be run locally. Does not connect to any other networked island, but is good
  * for testing agent functions without having to spin up a cluster.
  */
class LocalEvvoIsland[Sol]
(
  creators: Vector[CreatorFunction[Sol]],
  mutators: Vector[ModifierFunction[Sol]],
  deletors: Vector[DeletorFunction[Sol]],
  objectives: Vector[Objective[Sol]],
  immigrationStrategy: ImmigrationStrategy,
  emigrationStrategy: EmigrationStrategy
)(
  implicit val log: LoggingAdapter = LocalLogger
) extends EvolutionaryProcess[Sol] {
  private val island = new EvvoIsland(
    creators,
    mutators,
    deletors,
    objectives,
    immigrationStrategy,
    emigrationStrategy)

  override def runBlocking(stopAfter: StopAfter): Unit = {
    island.runBlocking(stopAfter)
  }

  override def runAsync(stopAfter: StopAfter): Future[Unit] = {
    island.runAsync(stopAfter)
  }

  override def currentParetoFrontier(): ParetoFrontier[Sol] = {
    island.currentParetoFrontier()
  }

  override def immigrate(solutions: Seq[Scored[Sol]]): Unit = {
    island.immigrate(solutions)
  }

  override def poisonPill(): Unit = {
    island.poisonPill()
  }

  override def registerIslands(islands: Seq[EvolutionaryProcess[Sol]]): Unit = {
    island.registerIslands(islands)
  }
}

object LocalLogger extends LoggingAdapter {
  private val logger = LoggerFactory.getLogger("LocalEvvoIsland")

  override def isErrorEnabled: Boolean = true

  override def isWarningEnabled: Boolean = true

  override def isInfoEnabled: Boolean = true

  override def isDebugEnabled: Boolean = false

  override protected def notifyError(message: String): Unit = {
    logger.error(message)
  }

  override protected def notifyError(cause: Throwable, message: String): Unit = {
    logger.error(message, cause)
  }

  override protected def notifyWarning(message: String): Unit = {
    logger.warn(message)
  }

  override protected def notifyInfo(message: String): Unit = {
    logger.info(message)
  }

  override protected def notifyDebug(message: String): Unit = {
  }
}

// =================================================================================================
// Remote EvvoIsland

/** A single-island evolutionary system, which will run on one computer (although on multiple
  * threads). Because it is an Akka actor, generally people will use SingleIslandEvvo.Wrapped
  * to use it in a type-safe way, instead of throwing messages.
  */
class RemoteEvvoIsland[Sol]
(
  creators: Vector[CreatorFunction[Sol]],
  mutators: Vector[ModifierFunction[Sol]],
  deletors: Vector[DeletorFunction[Sol]],
  objectives: Vector[Objective[Sol]],
  immigrationStrategy: ImmigrationStrategy,
  emigrationStrategy: EmigrationStrategy
)
  extends Actor with EvolutionaryProcess[Sol] with ActorLogging {
  // for messages, which are case classes defined within RemoteEvvoIsland's companion objeect
  import io.evvo.island.RemoteEvvoIsland._ // scalastyle:ignore import.grouping

  implicit val logger: LoggingAdapter = log

  private val island = new EvvoIsland(
    creators,
    mutators,
    deletors,
    objectives,
    immigrationStrategy,
    emigrationStrategy)

  override def receive: Receive = LoggingReceive({
    case Run(t) => sender ! this.runBlocking(t)
    case GetParetoFrontier => sender ! this.currentParetoFrontier()
    case Immigrate(solutions: Seq[Scored[Sol]]) => this.immigrate(solutions)
    case RegisterIslands(islands: Seq[EvolutionaryProcess[Sol]]) => this.registerIslands(islands)
  })


  override def runBlocking(stopAfter: StopAfter): Unit = {
    island.runBlocking(stopAfter)
  }

  override def runAsync(stopAfter: StopAfter): Future[Unit] = {
    island.runAsync(stopAfter)
  }

  override def currentParetoFrontier(): ParetoFrontier[Sol] = {
    island.currentParetoFrontier()
  }

  override def immigrate(solutions: Seq[Scored[Sol]]): Unit = {
    island.immigrate(solutions)
  }

  override def poisonPill(): Unit = {
    self ! PoisonPill
  }

  override def registerIslands(islands: Seq[EvolutionaryProcess[Sol]]): Unit = {
    this.island.registerIslands(islands)
  }
}

object RemoteEvvoIsland {
  /** This is a wrapper for ActorRefs of SingleIslandEvvo actors, serving as an
    * adapter to the EvolutionaryProcess interface. This allows strongly-typed code to
    * still use Akka for async message passing.
    *
    * @param ref The reference to wrap
    */
  case class Wrapper[Sol](ref: ActorRef) extends EvolutionaryProcess[Sol] {
    implicit val timeout: Timeout = Timeout(5.days)

    override def runBlocking(stopAfter: StopAfter): Unit = {
      Await.result(this.runAsync(stopAfter), Duration.Inf)
    }

    override def runAsync(stopAfter: StopAfter): Future[Unit] = {
      (ref ? Run(stopAfter)).asInstanceOf[Future[Unit]]
    }

    override def currentParetoFrontier(): ParetoFrontier[Sol] = {
      Await.result(ref ? GetParetoFrontier, Duration.Inf).asInstanceOf[ParetoFrontier[Sol]]
    }

    override def immigrate(solutions: Seq[Scored[Sol]]): Unit = {
      ref ! Immigrate(solutions)
    }

    override def poisonPill(): Unit = {
      ref ! PoisonPill
    }

    override def registerIslands(islands: Seq[EvolutionaryProcess[Sol]]): Unit = {
      ref ! RegisterIslands[Sol](islands)
    }
  }


  // All of these are meant to be used as Akka messages.
  private case class Run(stopAfter: StopAfter)

  private case object GetParetoFrontier

  private case class Immigrate[Sol](solutions: Seq[Scored[Sol]])

  private case class RegisterIslands[Sol](islands: Seq[EvolutionaryProcess[Sol]])
}
