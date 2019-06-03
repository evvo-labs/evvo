package com.evvo.island

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill, Props}
import akka.event.{LoggingAdapter, LoggingReceive}
import akka.pattern.ask
import akka.util.Timeout
import com.evvo.agent._
import com.evvo.island.population.{Objective, ParetoFrontier}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}


// for messages
import com.evvo.island.EvvoIslandActor._

/**
  * A single-island evolutionary system, which will run on one computer (although on multiple
  * CPU cores). Because it is an Akka actor, generally people will use SingleIslandEvvo.Wrapped
  * to use it in a type-safe way, instead of throwing messages.
  */
class EvvoIslandActor[Sol]
(
  creators: Vector[CreatorFunction[Sol]],
  mutators: Vector[MutatorFunction[Sol]],
  deletors: Vector[DeletorFunction[Sol]],
  fitnesses: Vector[Objective[Sol]]
)
  extends Actor with EvolutionaryProcess[Sol] with ActorLogging {
  implicit val logger: LoggingAdapter = log

  val island = new EvvoIsland[Sol](creators, mutators, deletors, fitnesses)

  override def receive: Receive = LoggingReceive({
    case Run(t) => sender ! this.runBlocking(t)
    case GetParetoFrontier => sender ! this.currentParetoFrontier()
    case Emigrate(solutions: Seq[Sol]) => this.emigrate(solutions)
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

  override def emigrate(solutions: Seq[Sol]): Unit = {
    island.emigrate(solutions)
  }

  override def poisonPill(): Unit = {
    self ! PoisonPill
  }
}

object EvvoIslandActor {
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

    val props = Props(new EvvoIslandActor[Sol](
      creators.toVector,
      mutators.toVector,
      deletors.toVector,
      fitnesses.toVector))
    EvvoIslandActor.Wrapper[Sol](system.actorOf(props, s"EvvoIsland_${java.util.UUID.randomUUID()}"))
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

    override def emigrate(solutions: Seq[Sol]): Unit = {
      ref ! Emigrate(solutions)
    }

    override def poisonPill(): Unit = {
      ref ! PoisonPill
    }
  }

  case class Run(stopAfter: StopAfter)

  case object GetParetoFrontier

  case class Emigrate[Sol](solutions: Seq[Sol])

}
