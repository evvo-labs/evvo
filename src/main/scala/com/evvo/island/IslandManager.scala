package com.evvo.island

import java.io.File

import akka.actor.{Actor, ActorSystem, Address, AddressFromURIString, Deploy, PoisonPill, Props}
import akka.event.LoggingReceive
import akka.remote.RemoteScope
import com.evvo.island.EvvoIsland.{Emigrate, GetParetoFrontier, Run}
import com.evvo.island.population.{ParetoFrontier, TParetoFrontier, TScored}
import com.typesafe.config.ConfigFactory

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import collection.JavaConverters._
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._

/**
  * Launches and manages multiple EvvoIslands.
  */
class IslandManager[Sol](val numIslands: Int,
                         islandBuilder: EvvoIslandBuilder[Sol],
                         val actorSystemName: String = "EvvoNode",
                         val userConfig: String = "src/main/resources/application.conf")
  extends TEvolutionaryProcess[Sol] with Actor {

  private val config = ConfigFactory
    // TODO should be configurable by end users
    .parseFile(new File(userConfig))
    .withFallback(ConfigFactory.parseFile(new File("src/main/resources/application.conf")))
    .resolve()

  println("making island manager")

  private val addresses: Vector[Address] = config.getList("nodes.locations")
    .unwrapped()
    .asScala.toVector
    .map(x => AddressFromURIString(x.toString))

  implicit val system: ActorSystem = ActorSystem(actorSystemName, config)

  /**
    * The final pareto frontier after the island shuts down, or None until then.
    */
  private var finalParetoFrontier: Option[TParetoFrontier[Sol]] = None

  implicit val timeout: Timeout = 1.second

  context.actorSelection("akka.tcp://RemoteEvvoNode@127.0.0.1:3451/user/Island0").resolveOne

  // Round robin deploy new islands to the remote addresses
  private val islands = (0 until numIslands).map(i => {
    val remoteChoice = i % addresses.length // pick round robin
    val address = addresses(remoteChoice)
    system.actorOf(islandBuilder.props().withDeploy(Deploy(scope = RemoteScope(address))),
      s"EvvoIsland${i}")
  }).map(EvvoIsland.Wrapper[Sol])

  println("islands created")

  context.actorSelection("akka.tcp://RemoteEvvoNode@127.0.0.1:3451/user/Island0").resolveOne


  // the index of the current island in `islands` to send emigrations to
  private var islandEmigrationIndex = 0


  override def receive: Receive = LoggingReceive({
    case Run(terminationCriteria) => sender ! this.runBlocking(terminationCriteria)
    case GetParetoFrontier => sender ! this.currentParetoFrontier()
    case Emigrate(solutions: Seq[Sol]) => emigrate(solutions)
  })

  def emigrate(solutions: Seq[Sol]): Unit = {
    this.islands(this.islandEmigrationIndex).emigrate(solutions)
    this.islandEmigrationIndex += 1
  }


  override def runAsync(terminationCriteria: TTerminationCriteria)
  : Future[Unit] = {
    Future {
      val runIslands = this.islands.map(_.runAsync(terminationCriteria))
      println("RunIslands declared")
      runIslands.foreach(Await.result(_, Duration.Inf))
      println("RunIslands foreach")
      this.finalParetoFrontier = Some(this.currentParetoFrontier())
      println("got pareto frontier")
      this.islands.foreach(_.poisonPill())
      println(s"this.finalParetoFrontier = ${this.finalParetoFrontier}")
      this.system.terminate()
      println("finishing runBlocking in IslandManager")
    }
  }

  def runBlocking(terminationCriteria: TTerminationCriteria): Unit = {
    // TODO replace Duration.Inf
    Await.result(this.runAsync(terminationCriteria), Duration.Inf)
    println("finishing runBlocking in IslandManager")
  }

  override def currentParetoFrontier(): TParetoFrontier[Sol] = {
    println("IM CPF call")
    finalParetoFrontier match {
      case Some(paretoFrontier) =>
        println("Some(paretoFrontier) ")
        paretoFrontier
      case None =>
        println("None")
        println(s"islands(0).currentParetoFrontier() = ${islands(0).currentParetoFrontier()}")

        val islandFrontiers = islands
          .map(_.currentParetoFrontier().solutions)
          .foldLeft(Set[TScored[Sol]]())(_ | _)

        println("islandFrontiers")
        ParetoFrontier(islandFrontiers)
    }
  }

  override def poisonPill(): Unit = {
    println("PoisonPill received")
    this.islands.foreach(_.poisonPill())
    self ! PoisonPill
  }
}

object IslandManager {
  def from[Sol](numIslands: Int,
                islandBuilder: EvvoIslandBuilder[Sol],
                actorSystemName: String = "EvvoNode",
                userConfig: String = "src/main/resources/application.conf")
               (implicit system: ActorSystem): TEvolutionaryProcess[Sol] = {
    EvvoIsland.Wrapper(system.actorOf(Props(new IslandManager[Sol](numIslands, islandBuilder))))
  }
}
