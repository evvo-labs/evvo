package com.diatom.island
import java.io.File

import akka.actor.ActorSystem
import com.diatom.island.population.{ParetoFrontier, TParetoFrontier, TScored}
import com.diatom._
import com.typesafe.config.ConfigFactory
import sun.security.provider.PolicyParser.ParsingException


/**
  * Launches and manages multiple EvvoIslands.
  */
class IslandManager[Sol](val numIslands: Int,
                         islandBuilder: EvvoIslandBuilder[Sol],
                         val actorSystemName: String = "EvvoCluster")
  extends TEvolutionaryProcess[Sol] {

  private val config = ConfigFactory.parseFile(new File("application.conf")).resolve()
  implicit val system: ActorSystem = ActorSystem(actorSystemName, config)

  private val islands: Vector[TEvolutionaryProcess[Sol]] =
    Vector.fill(numIslands)(islandBuilder.build())

  def run(terminationCriteria: TTerminationCriteria): TEvolutionaryProcess[Sol] = {
    islands.foreach(_.run(terminationCriteria))
    this
  }

  override def currentParetoFrontier(): TParetoFrontier[Sol] = {
    val islandFrontiers = islands
      .map(_.currentParetoFrontier().solutions)
      .foldLeft(Set[TScored[Sol]]())(_ | _)

    ParetoFrontier(islandFrontiers)
  }
}
