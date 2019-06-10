package com.evvo.island

import java.util.Calendar

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill, Props}
import akka.event.{LoggingAdapter, LoggingReceive}
import akka.pattern.ask
import akka.util.Timeout
import com.evvo.agent._
import com.evvo.island.population._
import com.evvo.{CreatorFunctionType, DeletorFunctionType, MutatorFunctionType, ObjectiveFunctionType}
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

  private val pop: Population[Sol] = StandardPopulation(fitnesses)
  private val creatorAgents = creators.map(c => CreatorAgent(c, pop))
  private val mutatorAgents = mutators.map(m => MutatorAgent(m, pop))
  private val deletorAgents = deletors.map(d => DeletorAgent(d, pop))

  private var emigrationTargets: Seq[EvolutionaryProcess[Sol]] = Seq()
  private var currentEmigrationTargetIndex: Int = 0

  override def runAsync(stopAfter: StopAfter)
  : Future[Unit] = {
    Future {
      log.info(s"Island running with stopAfter=${stopAfter}")

      // TODO can we put all of these in some combined pool? don't like having to manage each
      creatorAgents.foreach(_.start())
      mutatorAgents.foreach(_.start())
      deletorAgents.foreach(_.start())

      // TODO this is not ideal. fix wait time/add features to termination criteria
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
  def addCreatorFromFunction(creatorFunc: CreatorFunctionType[Sol]): EvvoIslandBuilder[Sol] = {
    this.copy(creators = creators + CreatorFunc(creatorFunc, creatorFunc.toString))
  }

  def addCreator(creatorFunc: CreatorFunction[Sol]): EvvoIslandBuilder[Sol] = {
    this.copy(creators = creators + creatorFunc)
  }

  def addMutatorFromFunction(mutatorFunc: MutatorFunctionType[Sol]): EvvoIslandBuilder[Sol] = {
    this.copy(mutators = mutators + MutatorFunc(mutatorFunc, mutatorFunc.toString))
  }

  def addMutator(mutatorFunc: MutatorFunction[Sol]): EvvoIslandBuilder[Sol] = {
    this.copy(mutators = mutators + mutatorFunc)
  }

  def addDeletorFromFunction(deletorFunc: DeletorFunctionType[Sol]): EvvoIslandBuilder[Sol] = {
    this.copy(deletors = deletors + DeletorFunc(deletorFunc, deletorFunc.toString))
  }

  def addDeletor(deletorFunc: DeletorFunction[Sol]): EvvoIslandBuilder[Sol] = {
    this.copy(deletors = deletors + deletorFunc)
  }

  // TODO this calling API is dangerous because it assumes minimization. it should be removed,
  //      but this will require refactoring across examples
  def addObjective(objective: ObjectiveFunctionType[Sol], name: String = "")
  : EvvoIslandBuilder[Sol] = {
    val realName = if (name == "") objective.toString() else name
    this.copy(objectives = objectives + Objective(objective, realName, Minimize))
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
  fitnesses: Vector[Objective[Sol]]
)(
  implicit val log: LoggingAdapter = LocalLogger
) extends EvolutionaryProcess[Sol] {
  private val island = new EvvoIsland(creators, mutators, deletors, fitnesses)

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
  fitnesses: Vector[Objective[Sol]]
)
  extends Actor with EvolutionaryProcess[Sol] with ActorLogging {
  // for messages
  import com.evvo.island.RemoteEvvoIsland._ // scalastyle:ignore import.grouping

  implicit val logger: LoggingAdapter = log

  private val island = new EvvoIsland[Sol](creators, mutators, deletors, fitnesses)

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
  /**
    * @param creators  the functions to be used for creating new solutions.
    * @param mutators  the functions to be used for creating new solutions from current solutions.
    * @param deletors  the functions to be used for deciding which solutions to delete.
    * @param fitnesses the objective functions to maximize.
    */
  def from[Sol](creators: TraversableOnce[CreatorFunction[Sol]],
                mutators: TraversableOnce[MutatorFunction[Sol]],
                deletors: TraversableOnce[DeletorFunction[Sol]],
                fitnesses: TraversableOnce[Objective[Sol]])
               (implicit system: ActorSystem)
  : EvolutionaryProcess[Sol] = {
    // TODO validate that there is at least one of each creator/mutator/deletors/fitness

    val props = Props(new RemoteEvvoIsland[Sol](
      creators.toVector,
      mutators.toVector,
      deletors.toVector,
      fitnesses.toVector))
    RemoteEvvoIsland.Wrapper[Sol](system.actorOf(props, s"EvvoIsland_${java.util.UUID.randomUUID()}"))
  }

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
