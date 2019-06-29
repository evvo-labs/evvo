package com.evvo.island

import java.io.File

import akka.actor.{ActorSystem, Address, AddressFromURIString, Deploy}
import akka.remote.RemoteScope
import com.evvo.island.population.{ParetoFrontier, Scored}
import com.typesafe.config.ConfigFactory

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
  * Common component implementing management of islands.
  *
  * @param islands the islands to manage
  */
class IslandManager[Sol](islands: Seq[EvolutionaryProcess[Sol]])
  extends EvolutionaryProcess[Sol] {

  this.registerIslands(islands)

  /** The final pareto frontier after the island manager shuts down, or None until then. */
  private var finalParetoFrontier: Option[ParetoFrontier[Sol]] = None

  override def runAsync(stopAfter: StopAfter)
  : Future[Unit] = {
    Future {
      val runIslands = this.islands.map(_.runAsync(stopAfter))
      runIslands.foreach(Await.result(_, Duration.Inf))
      this.finalParetoFrontier = Some(this.currentParetoFrontier())
      this.islands.foreach(_.poisonPill())
    }
  }

  override def runBlocking(stopAfter: StopAfter): Unit = {
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

  override def immigrate(solutions: Seq[Scored[Sol]]): Unit = {
    this.islands.foreach(_.immigrate(solutions))
  }

  override def poisonPill(): Unit = {
    this.islands.foreach(_.poisonPill())
  }

  override def registerIslands(islands: Seq[EvolutionaryProcess[Sol]]): Unit = {
    this.islands.foreach(_.registerIslands(islands))
  }
}


// =================================================================================================
// Remote manager

/**
  * Launches and manages multiple `EvvoIslandActor`s.
  */
class RemoteIslandManager[Sol](val numIslands: Int,
                               islandBuilder: FinishedEvvoIslandBuilder[Sol],
                               val actorSystemName: String = "EvvoNode",
                               val userConfig: String = "src/main/resources/application.conf")
  extends EvolutionaryProcess[Sol] {

  private val configFile = ConfigFactory.parseFile(new File("src/main/resources/application.conf"))
  private val config = configFile.getConfig("IslandManager")
    .withFallback(configFile)
    .resolve()
  println(config)

  private val addresses: Vector[Address] = config.getList("nodes.locations")
    .unwrapped()
    .asScala.toVector
    .map(x => AddressFromURIString(x.toString))

  implicit val system: ActorSystem = ActorSystem(actorSystemName, config)

  // Round robin deploy new islands to the remote addresses
  private val islands: IndexedSeq[EvolutionaryProcess[Sol]] = (0 until numIslands).map(i => {
    val remoteChoice = i % addresses.length // pick round robin
    val address = addresses(remoteChoice)
    system.actorOf(islandBuilder.toProps.withDeploy(Deploy(scope = RemoteScope(address))),
      s"EvvoIsland${i}")
  }).map(RemoteEvvoIsland.Wrapper[Sol])

  val islandManager = new IslandManager[Sol](islands)

  def immigrate(solutions: Seq[Scored[Sol]]): Unit = {
    this.islandManager.immigrate(solutions)
  }

  override def runAsync(stopAfter: StopAfter): Future[Unit] = {
    this.islandManager.runAsync(stopAfter)
  }

  def runBlocking(stopAfter: StopAfter): Unit = {
    this.islandManager.runBlocking(stopAfter)
  }

  override def currentParetoFrontier(): ParetoFrontier[Sol] = {
    this.islandManager.currentParetoFrontier()
  }

  override def poisonPill(): Unit = {
    this.islandManager.poisonPill()
  }

  override def registerIslands(islands: Seq[EvolutionaryProcess[Sol]]): Unit = {
    this.islandManager.registerIslands(islands)
  }
}


// =================================================================================================
// Local manager

/**
  * Launches and manages multiple `LocalEvvoIsland`s.
  */
class LocalIslandManager[Sol](val numIslands: Int,
                              islandBuilder: FinishedEvvoIslandBuilder[Sol])
  extends EvolutionaryProcess[Sol] {

  private val islands: IndexedSeq[EvolutionaryProcess[Sol]] =
    Vector.fill(numIslands)(islandBuilder.buildLocalEvvo())

  val islandManager = new IslandManager[Sol](islands)

  def immigrate(solutions: Seq[Scored[Sol]]): Unit = {
    this.islandManager.immigrate(solutions)
  }

  override def runAsync(stopAfter: StopAfter): Future[Unit] = {
    this.islandManager.runAsync(stopAfter)
  }

  def runBlocking(stopAfter: StopAfter): Unit = {
    this.islandManager.runBlocking(stopAfter)
  }

  override def currentParetoFrontier(): ParetoFrontier[Sol] = {
    this.islandManager.currentParetoFrontier()
  }

  override def poisonPill(): Unit = {
    this.islandManager.poisonPill()
  }

  override def registerIslands(islands: Seq[EvolutionaryProcess[Sol]]): Unit = {
    this.islandManager.registerIslands(islands)
  }
}
