package com.evvo.island

import java.io.File

import akka.actor.{Actor, ActorSystem, Address, AddressFromURIString, Deploy, PoisonPill, Props}
import akka.event.LoggingReceive
import akka.remote.RemoteScope
import akka.util.Timeout
import com.evvo.island.population.{ParetoFrontier, Scored}
import com.typesafe.config.ConfigFactory
import com.evvo.island.EvvoIslandActor.{Emigrate, GetParetoFrontier, Run}


import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{Await, Future}

/**
  * Launches and manages multiple EvvoIslandActors.
  */
class IslandManager[Sol](val numIslands: Int,
                         islandBuilder: EvvoIslandBuilder[Sol],
                         val actorSystemName: String = "EvvoNode",
                         val userConfig: String = "src/main/resources/application.conf")
                        (implicit system: ActorSystem)
  extends EvolutionaryProcess[Sol] with Actor {


  private val configFile = ConfigFactory.parseFile(new File("src/main/resources/application.conf"))
  private val config = configFile.getConfig("IslandManager")
    .withFallback(configFile)
    .resolve()
  println(config)

  private val addresses: Vector[Address] = config.getList("nodes.locations")
    .unwrapped()
    .asScala.toVector
    .map(x => AddressFromURIString(x.toString))

//  implicit val system: ActorSystem = ActorSystem(actorSystemName, config)

  /**
    * The final pareto frontier after the island shuts down, or None until then.
    */
  private var finalParetoFrontier: Option[ParetoFrontier[Sol]] = None

  implicit val timeout: Timeout = 1.second


  // Round robin deploy new islands to the remote addresses
  private val islands = (0 until numIslands).map(i => {
    val remoteChoice = i % addresses.length // pick round robin
    val address = addresses(remoteChoice)
    system.actorOf(islandBuilder.toProps.withDeploy(Deploy(scope = RemoteScope(address))),
      s"EvvoIsland${i}")
  }).map(EvvoIslandActor.Wrapper[Sol])


  // the index of the current island in `islands` to send emigrations to
  private var islandEmigrationIndex = 0


  override def receive: Receive = LoggingReceive({
    case Run(stopAfter) => sender ! this.runBlocking(stopAfter)
    case GetParetoFrontier => sender ! this.currentParetoFrontier()
    case Emigrate(solutions: Seq[Sol]) => emigrate(solutions)
  })

  def emigrate(solutions: Seq[Sol]): Unit = {
    this.islands(this.islandEmigrationIndex).emigrate(solutions)
    this.islandEmigrationIndex += 1
  }


  override def runAsync(stopAfter: StopAfter)
  : Future[Unit] = {
    Future {
      val runIslands = this.islands.map(_.runAsync(stopAfter))
      runIslands.foreach(Await.result(_, Duration.Inf))
      this.finalParetoFrontier = Some(this.currentParetoFrontier())
      this.islands.foreach(_.poisonPill())
      this.system.terminate()
    }
  }

  def runBlocking(stopAfter: StopAfter): Unit = {
    // TODO replace Duration.Inf
    Await.result(this.runAsync(stopAfter), Duration.Inf)
  }

  override def currentParetoFrontier(): ParetoFrontier[Sol] = {
    finalParetoFrontier match {
      case Some(paretoFrontier) =>
        paretoFrontier
      case None =>
        val islandFrontiers = islands
          .map(_.currentParetoFrontier().solutions)
          .foldLeft(Set[Scored[Sol]]())(_ | _)

        ParetoFrontier(islandFrontiers)
    }
  }

  override def poisonPill(): Unit = {
    this.islands.foreach(_.poisonPill())
    self ! PoisonPill
  }
}

object IslandManager {
  def from[Sol](numIslands: Int,
                islandBuilder: EvvoIslandBuilder[Sol],
                actorSystemName: String = "EvvoNode",
                userConfig: String = "src/main/resources/application.conf")
               (implicit system: ActorSystem): EvolutionaryProcess[Sol] = {
    EvvoIslandActor.Wrapper(
      system.actorOf(Props(
        new IslandManager[Sol](numIslands, islandBuilder, userConfig=userConfig))))
  }
}
