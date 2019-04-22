package com.diatom.island

import java.util.Calendar

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.event.LoggingReceive
import akka.pattern.ask
import akka.util.Timeout
import com.diatom._
import com.diatom.agent._
import com.diatom.agent.func._
import com.diatom.population.Population
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * A single-island evolutionary system, which will run on one computer (although on multiple
  * CPU cores).
  */
class SingleIslandEvvo[Sol]
(
  creators: Vector[TCreatorFunc[Sol]],
  mutators: Vector[TMutatorFunc[Sol]],
  deletors: Vector[TDeletorFunc[Sol]],
  fitnesses: Vector[TFitnessFunc[Sol]]
) extends Actor with TIsland[Sol] with ActorLogging {


  private val pop = Population(fitnesses)
  private val creatorAgents = creators.map(c => CreatorAgent.from(c, pop))
  private val mutatorAgents = mutators.map(m => MutatorAgent.from(m, pop))
  private val deletorAgents = deletors.map(d => DeletorAgent.from(d, pop))


  //   TODO should be able to pass configurations, have multiple logging environments
  private val config = ConfigFactory.parseString(
    """
      |akka {
      |  loggers = ["akka.event.slf4j.Slf4jLogger"]
      |  loglevel = "INFO"
      |  logging-filter = "akka.event.slf4j.Slf4jLoggingFilter"
      |  actor {
      |    debug {
      |      receive = true
      |    }
      |  }
      |}
    """.stripMargin)
  implicit val system: ActorSystem = ActorSystem("evvo", config)


  import com.diatom.island.SingleIslandEvvo._

  override def receive: Receive = LoggingReceive({
    case Run(t) => sender ! this.run(t)
    case GetParetoFrontier => sender ! this.currentParetoFrontier()
  })

  def run(terminationCriteria: TTerminationCriteria): TIsland[Sol] = {
    log.info("Island running with terminationCriteria=%s", terminationCriteria)

    // TODO can we put all of these in some combined pool? don't like having to manage each
    log.info("starting agents")
    creatorAgents.foreach(_.start())
    mutatorAgents.foreach(_.start())
    deletorAgents.foreach(_.start())
    log.info("started agents")

    // TODO this is not ideal. fix wait time/add features to termination criteria
    val startTime = Calendar.getInstance().toInstant.toEpochMilli

    while (startTime + terminationCriteria.time.toMillis >
      Calendar.getInstance().toInstant.toEpochMilli) {
      Thread.sleep(500)
      val pareto = pop.getParetoFrontier()
      log.info(f"pareto = ${pareto}")
    }

    log.info("stopping agents")
    creatorAgents.foreach(_.stop())
    mutatorAgents.foreach(_.stop())
    deletorAgents.foreach(_.stop())
    log.info("stopped agents")

    this
  }

  override def currentParetoFrontier(): TParetoFrontier[Sol] = pop.getParetoFrontier()
}

object SingleIslandEvvo {
  /**
    * @param creators  the functions to be used for creating new solutions.
    * @param mutators  the functions to be used for creating new solutions from current solutions.
    * @param deletors  the functions to be used for deciding which solutions to delete.
    * @param fitnesses the objective functions to maximize.
    */
  def from[Sol](creators: TraversableOnce[TCreatorFunc[Sol]],
                mutators: TraversableOnce[TMutatorFunc[Sol]],
                deletors: TraversableOnce[TDeletorFunc[Sol]],
                fitnesses: TraversableOnce[TFitnessFunc[Sol]])
  : TIsland[Sol] = {
    val system = ActorSystem("SingleIslandEvvo")
    val props = Props(new SingleIslandEvvo[Sol](
      creators.toVector,
      mutators.toVector,
      deletors.toVector,
      fitnesses.toVector))
    SingleIslandEvvo.Wrapper[Sol](system.actorOf(props, "SingleIslandEvvo"))
  }

  def builder[Sol](): SingleIslandEvvoBuilder[Sol] = SingleIslandEvvoBuilder[Sol]()

  /**
    * This is a wrapper for ActorRefs containing SingleIslandEvvo actors, serving as an
    * adapter to the TIsland interface
    *
    * @param ref the reference to wrap
    */
  case class Wrapper[Sol](ref: ActorRef) extends TIsland[Sol] {
    implicit val timeout: Timeout = Timeout(5.seconds)

    override def run(terminationCriteria: TTerminationCriteria): TIsland[Sol] = {
      // Block forever, `run` is meant to be a blocking call.
      Await.result(ref ? Run(terminationCriteria), Duration.Inf)
      this
    }

    override def currentParetoFrontier(): TParetoFrontier[Sol] = {
      Await.result(ref ? GetParetoFrontier, Duration.Inf).asInstanceOf[TParetoFrontier[Sol]]
    }
  }

  case class Run(terminationCriteria: TTerminationCriteria)
  case object GetParetoFrontier
}

/**
  * @param creators  the functions to be used for creating new solutions.
  * @param mutators  the functions to be used for creating new solutions from current solutions.
  * @param deletors  the functions to be used for deciding which solutions to delete.
  * @param fitnesses the objective functions to maximize.
  */
case class SingleIslandEvvoBuilder[Sol]
(
  creators: Set[TCreatorFunc[Sol]] = Set[TCreatorFunc[Sol]](),
  mutators: Set[TMutatorFunc[Sol]] = Set[TMutatorFunc[Sol]](),
  deletors: Set[TDeletorFunc[Sol]] = Set[TDeletorFunc[Sol]](),
  fitnesses: Set[TFitnessFunc[Sol]] = Set[TFitnessFunc[Sol]]()
) {
  def addCreator(creatorFunc: CreatorFunctionType[Sol]): SingleIslandEvvoBuilder[Sol] = {
    this.copy(creators = creators + CreatorFunc(creatorFunc, creatorFunc.toString))
  }

  def addCreator(creatorFunc: TCreatorFunc[Sol]): SingleIslandEvvoBuilder[Sol] = {
    this.copy(creators = creators + creatorFunc)
  }

  def addMutator(mutatorFunc: MutatorFunctionType[Sol]): SingleIslandEvvoBuilder[Sol] = {
    this.copy(mutators = mutators + MutatorFunc(mutatorFunc, mutatorFunc.toString))
  }

  def addMutator(mutatorFunc: TMutatorFunc[Sol]): SingleIslandEvvoBuilder[Sol] = {
    this.copy(mutators = mutators + mutatorFunc)
  }

  def addDeletor(deletorFunc: DeletorFunctionType[Sol]): SingleIslandEvvoBuilder[Sol] = {
    this.copy(deletors = deletors + DeletorFunc(deletorFunc, deletorFunc.toString))
  }

  def addDeletor(deletorFunc: TDeletorFunc[Sol]): SingleIslandEvvoBuilder[Sol] = {
    this.copy(deletors = deletors + deletorFunc)
  }

  def addFitness(fitnessFunc: FitnessFunctionType[Sol], name: String = null)
  : SingleIslandEvvoBuilder[Sol] = {
    val realName = if (name == null) fitnessFunc.toString() else name
    this.copy(fitnesses = fitnesses + FitnessFunc(fitnessFunc, realName))
  }

  def build(): TIsland[Sol] = {
    SingleIslandEvvo.from[Sol](creators, mutators, deletors, fitnesses)
  }

}