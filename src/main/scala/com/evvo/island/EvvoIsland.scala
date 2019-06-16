package com.evvo.island

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
import java.util.Calendar

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill, Props}
import akka.event.{LoggingAdapter, LoggingReceive}
import akka.pattern.ask
import akka.util.Timeout
import com.evvo.agent._
import com.evvo.island.population._
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{Await, Future}


class EvvoIsland[Sol]
(
  creators: Vector[CreatorFunction[Sol]],
  mutators: Vector[MutatorFunction[Sol]],
  deletors: Vector[DeletorFunction[Sol]],
  fitnesses: Vector[Objective[Sol]])
(implicit log: LoggingAdapter)
  extends EvolutionaryProcess[Sol] {

  def serializationRoundtrip[T](t: T): T = {
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
  private val mutatorAgents = mutators.map(m => MutatorAgent(serializationRoundtrip(m), pop))
  private val deletorAgents = deletors.map(d => DeletorAgent(serializationRoundtrip(d), pop))

  private var emigrationTargets: Seq[EvolutionaryProcess[Sol]] = Seq()
  private var currentEmigrationTargetIndex: Int = 0

  override def runAsync(stopAfter: StopAfter)
  : Future[Unit] = {
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

  override def immigrate(solutions: Seq[Sol]): Unit = {
    pop.addSolutions(solutions)
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
      val emigrants = this.pop.getSolutions(4).map(_.solution)
      this.emigrationTargets(currentEmigrationTargetIndex).immigrate(emigrants)
      currentEmigrationTargetIndex = (currentEmigrationTargetIndex + 1) % emigrationTargets.length
    }
  }
}

object EvvoIsland {
  def builder[Sol](): EvvoIslandBuilder[Sol] = EvvoIslandBuilder[Sol]()
}


/**
  * @param creators   the functions to be used for creating new solutions.
  * @param mutators   the functions to be used for creating new solutions from current solutions.
  * @param deletors   the functions to be used for deciding which solutions to delete.
  * @param objectives the objective functions to maximize.
  */
case class EvvoIslandBuilder[Sol]
(
  creators: Set[CreatorFunction[Sol]] = Set[CreatorFunction[Sol]](),
  mutators: Set[MutatorFunction[Sol]] = Set[MutatorFunction[Sol]](),
  deletors: Set[DeletorFunction[Sol]] = Set[DeletorFunction[Sol]](),
  objectives: Set[Objective[Sol]] = Set[Objective[Sol]]()
) {
  def addCreator(creatorFunc: CreatorFunction[Sol]): EvvoIslandBuilder[Sol] = {
    this.copy(creators = creators + creatorFunc)
  }

  def addMutator(mutatorFunc: MutatorFunction[Sol]): EvvoIslandBuilder[Sol] = {
    this.copy(mutators = mutators + mutatorFunc)
  }

  def addDeletor(deletorFunc: DeletorFunction[Sol]): EvvoIslandBuilder[Sol] = {
    this.copy(deletors = deletors + deletorFunc)
  }

  def addObjective(objective: Objective[Sol])
  : EvvoIslandBuilder[Sol] = {
    this.copy(objectives = objectives + objective)
  }

  def toProps()(implicit system: ActorSystem): Props = {
    Props(new RemoteEvvoIsland[Sol](
      creators.toVector,
      mutators.toVector,
      deletors.toVector,
      objectives.toVector))
  }

  def buildLocalEvvo(): EvolutionaryProcess[Sol] = {
    new LocalEvvoIsland[Sol](
      creators.toVector,
      mutators.toVector,
      deletors.toVector,
      objectives.toVector)
  }
}

// =================================================================================================
// Local EvvoIsland wrapper

/**
  * An island that can be run locally. Does not connect to any other networked island, but is good
  * for testing agent functions without having to spin up a cluster.
  */
class LocalEvvoIsland[Sol]
(
  creators: Vector[CreatorFunction[Sol]],
  mutators: Vector[MutatorFunction[Sol]],
  deletors: Vector[DeletorFunction[Sol]],
  objectives: Vector[Objective[Sol]]
)(
  implicit val log: LoggingAdapter = LocalLogger
) extends EvolutionaryProcess[Sol] {
  private val island = new EvvoIsland(
    creators,
    mutators,
    deletors,
    objectives)

  override def runBlocking(stopAfter: StopAfter): Unit = {
    island.runBlocking(stopAfter)
  }

  override def runAsync(stopAfter: StopAfter): Future[Unit] = {
    island.runAsync(stopAfter)
  }

  override def currentParetoFrontier(): ParetoFrontier[Sol] = {
    island.currentParetoFrontier()
  }

  override def immigrate(solutions: Seq[Sol]): Unit = {
    island.immigrate(solutions)
  }

  override def poisonPill(): Unit = {
    island.poisonPill()
  }

  override def registerIslands(islands: Seq[EvolutionaryProcess[Sol]]): Unit = {
    island.registerIslands(islands)
  }
}


object LocalEvvoIsland {
  def builder[Sol](): EvvoIslandBuilder[Sol] = EvvoIslandBuilder[Sol]()
}


object LocalLogger extends LoggingAdapter {
  private val logger = LoggerFactory.getLogger("LocalEvvoIsland")

  override def isErrorEnabled: Boolean = true

  override def isWarningEnabled: Boolean = true

  override def isInfoEnabled: Boolean = true

  override def isDebugEnabled: Boolean = true

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
    logger.debug(message)
  }
}

// =================================================================================================
// Remote EvvoIsland

/**
  * A single-island evolutionary system, which will run on one computer (although on multiple
  * CPU cores). Because it is an Akka actor, generally people will use SingleIslandEvvo.Wrapped
  * to use it in a type-safe way, instead of throwing messages.
  */
class RemoteEvvoIsland[Sol]
(
  creators: Vector[CreatorFunction[Sol]],
  mutators: Vector[MutatorFunction[Sol]],
  deletors: Vector[DeletorFunction[Sol]],
  objectives: Vector[Objective[Sol]]
)
  extends Actor with EvolutionaryProcess[Sol] with ActorLogging {
  // for messages
  import com.evvo.island.RemoteEvvoIsland._ // scalastyle:ignore import.grouping

  implicit val logger: LoggingAdapter = log

  private val island = new EvvoIsland(
    creators,
    mutators,
    deletors,
    objectives)

  override def receive: Receive = LoggingReceive({
    case Run(t) => sender ! this.runBlocking(t)
    case GetParetoFrontier => sender ! this.currentParetoFrontier()
    case Immigrate(solutions: Seq[Sol]) => this.immigrate(solutions)
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

  override def immigrate(solutions: Seq[Sol]): Unit = {
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

  def builder[Sol](): EvvoIslandBuilder[Sol] = EvvoIslandBuilder[Sol]()

  /**
    * This is a wrapper for ActorRefs containing SingleIslandEvvo actors, serving as an
    * adapter to the TIsland interface
    *
    * @param ref the reference to wrap
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

    override def immigrate(solutions: Seq[Sol]): Unit = {
      ref ! Immigrate(solutions)
    }

    override def poisonPill(): Unit = {
      ref ! PoisonPill
    }

    override def registerIslands(islands: Seq[EvolutionaryProcess[Sol]]): Unit = {
      ref ! RegisterIslands[Sol](islands)
    }
  }

  case class Run(stopAfter: StopAfter)

  case object GetParetoFrontier

  case class Immigrate[Sol](solutions: Seq[Sol])

  case class RegisterIslands[Sol](islands: Seq[EvolutionaryProcess[Sol]])
}
