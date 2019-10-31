package io.evvo.island

import java.io.File

import akka.actor.{ActorSystem, Address, AddressFromURIString, Deploy}
import akka.remote.RemoteScope
import com.typesafe.config.ConfigFactory
import io.evvo.agent.AgentStatus
import io.evvo.island.population.{FullyConnectedNetworkTopology, NetworkTopology, ParetoFrontier, Scored}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Random, Try}

/** Common component implementing management of islands.
  *
  * @param islands the islands to manage
  */
private class IslandManager[Sol](
    islands: Seq[EvolutionaryProcess[Sol]],
    networkTopology: NetworkTopology)
    extends EvolutionaryProcess[Sol] {

  connectNetwork()

  /** The final pareto frontier after the island manager shuts down, or None until then. */
  private var finalParetoFrontier: Option[ParetoFrontier[Sol]] = None

  override def runAsync(stopAfter: StopAfter): Future[Unit] = {
    Future {
      // Represents all islands having completed their runs.
      val allIslandsRun = Future.sequence(this.islands.map(_.runAsync(stopAfter)))
      // Wait for that to happen
      Try { Await.result(allIslandsRun, stopAfter.time * 10) }
      // Then perform cleanup
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

  override def addSolutions(solutions: Seq[Sol]): Unit = {
    Random.shuffle(islands)  // Don't want to always add to the same islands more
      .zip(solutions.grouped(solutions.length / islands.length))
      .foreach({case (isle: EvolutionaryProcess[Sol], sols: Seq[Sol]) => isle.addSolutions(sols)})
  }

  override def poisonPill(): Unit = {
    this.islands.foreach(_.poisonPill())
  }

  override def registerIslands(islands: Seq[EvolutionaryProcess[Sol]]): Unit = {
    this.islands.foreach(_.registerIslands(islands))
  }

  private def connectNetwork(): Unit = {
    val connectionsToMake = networkTopology.configure(islands.length)
    connectionsToMake.foreach(c => {
      islands(c.from).registerIslands(Seq(islands(c.to)))
    })
  }

  override def agentStatuses(): Seq[AgentStatus] = islands.flatMap(_.agentStatuses())
}

// =================================================================================================
// Remote manager
/** Launches and manages multiple `EvvoIslandActor`s. */
class RemoteIslandManager[Sol](
    val numIslands: Int,
    val islandBuilder: FinishedEvvoIslandBuilder[Sol],
    val remoteAddresses: Seq[String],
    val networkTopology: NetworkTopology = FullyConnectedNetworkTopology(),
    val actorSystemName: String = "EvvoNode",
    val userConfig: String = "src/main/resources/application.conf"
) extends EvolutionaryProcess[Sol] {

  private val configFile = ConfigFactory.parseFile(new File("src/main/resources/application.conf"))
  private val config = configFile
    .withFallback(ConfigFactory.parseFile(new File(userConfig)))
    .resolve()

  private val addresses: Seq[Address] = remoteAddresses.map(AddressFromURIString.apply)

  implicit val system: ActorSystem = ActorSystem(actorSystemName, config)

  // Round robin deploy new islands to the remote addresses
  private val islands: IndexedSeq[EvolutionaryProcess[Sol]] = (0 until numIslands)
    .map(i => {
      val remoteChoice = i % addresses.length // pick round robin
      val address = addresses(remoteChoice)
      system.actorOf(
        islandBuilder.toProps.withDeploy(Deploy(scope = RemoteScope(address))),
        s"EvvoIsland${i}"
      )
    })
    .map(RemoteEvvoIsland.Wrapper[Sol])

  private val islandManager = new IslandManager[Sol](islands, networkTopology)

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

  override def agentStatuses(): Seq[AgentStatus] = islandManager.agentStatuses()

  override def addSolutions(solutions: Seq[Sol]): Unit = {
    this.islandManager.addSolutions(solutions)
  }
}

// =================================================================================================
// Local manager
/** Launches and manages multiple `LocalEvvoIsland`s. */
class LocalIslandManager[Sol](
    val numIslands: Int,
    islandBuilder: FinishedEvvoIslandBuilder[Sol],
    val networkTopology: NetworkTopology = FullyConnectedNetworkTopology())
    extends EvolutionaryProcess[Sol] {

  private val islands: IndexedSeq[EvolutionaryProcess[Sol]] =
    Vector.fill(numIslands)(islandBuilder.buildLocalEvvo())

  private val islandManager = new IslandManager[Sol](islands, networkTopology)

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

  override def agentStatuses(): Seq[AgentStatus] = islandManager.agentStatuses()

  override def addSolutions(solutions: Seq[Sol]): Unit = {
    this.islandManager.addSolutions(solutions)
  }
}
