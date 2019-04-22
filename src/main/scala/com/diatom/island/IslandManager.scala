package com.diatom.island
import akka.actor.ActorSystem
import com.diatom.{ParetoFrontier, TParetoFrontier, TScored}
import sun.security.provider.PolicyParser.ParsingException


/**
  * Launches and manages multiple EvvoIslands.
  */
class IslandManager[Sol](val numIslands: Int,
                         islandBuilder: EvvoIslandBuilder[Sol],
                         val actorSystemName: String = "Evvo")
  extends TEvolutionaryProcess[Sol] {

  implicit val system: ActorSystem = ActorSystem(actorSystemName)

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
