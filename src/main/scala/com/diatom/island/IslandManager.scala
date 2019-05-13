package com.diatom.island

import java.io.File

import akka.actor.ActorSystem
import com.diatom.island.population.{ParetoFrontier, TParetoFrontier, TScored}
import com.typesafe.config.ConfigFactory

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global


/**
  * Launches and manages multiple EvvoIslands.
  */
class IslandManager[Sol](val numIslands: Int,
                         islandBuilder: EvvoIslandBuilder[Sol],
                         val actorSystemName: String = "EvvoNode",
                         val userConfig: String = "src/main/resources/application.conf")
  extends TEvolutionaryProcess[Sol] {

  private val config = ConfigFactory
    // TODO should be configurable by end users
    .parseFile(new File(userConfig))
    .withFallback(ConfigFactory.parseFile(new File("src/main/resources/application.conf")))
    .resolve()

  implicit val system: ActorSystem = ActorSystem(actorSystemName, config)

  /**
    * The final pareto frontier after the island shuts down, or None until then.
    */
  private var finalParetoFrontier: Option[TParetoFrontier[Sol]] = None

  private val islands: Vector[TEvolutionaryProcess[Sol]] =
    Vector.fill(numIslands)(islandBuilder.build())

  def runBlocking(terminationCriteria: TTerminationCriteria): TEvolutionaryProcess[Sol] = {
    // TODO replace Duration.Inf
    Await.result(this.runAsync(terminationCriteria), Duration.Inf)
  }

  override def runAsync(terminationCriteria: TTerminationCriteria)
  : Future[TEvolutionaryProcess[Sol]] = {
    Future {
      this.islands.map(_.runAsync(terminationCriteria)).map(Await.result(_, Duration.Inf))
      this.finalParetoFrontier = Some(currentParetoFrontier())
      this.system.terminate()
      this
    }
  }

  override def currentParetoFrontier(): TParetoFrontier[Sol] = {
    finalParetoFrontier match {
      case Some(paretoFrontier) => paretoFrontier
      case None =>
        val islandFrontiers = islands
          .map(_.currentParetoFrontier().solutions)
          .foldLeft(Set[TScored[Sol]]())(_ | _)

        ParetoFrontier(islandFrontiers)
    }
  }
}
