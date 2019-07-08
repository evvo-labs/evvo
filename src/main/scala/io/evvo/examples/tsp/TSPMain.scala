package io.evvo.examples.tsp

import io.evvo.agent.defaults.DeleteDominated
import io.evvo.island.{EvvoIsland, EvvoIslandBuilder, IslandManager, LocalIslandManager, StopAfter}
import scala.concurrent.duration._

object TSPMain {

  def main(args: Array[String]): Unit = {

    // Read sample data
    val filename1 = args(0)
    val filename2 = args(1)

    def parseEuclideanCoordinates(filename: String): IndexedSeq[(Double,Double)] = {

      val data = scala.io.Source.fromFile(filename)
      data.getLines()
        .map(
          _.split(" ")
            .drop(1)
            .map(_.toDouble))
        .map(arr => (arr(0), arr(1)))
        .toVector
    }

    val tsp2 = TSP2.fromEuclideanPoints(parseEuclideanCoordinates(filename1), parseEuclideanCoordinates(filename2))

    val islandBuilder = EvvoIsland.builder()
      .addObjective(CostObjective("CostA", tsp2.cost1))
      .addObjective(CostObjective("CostB", tsp2.cost2))
      .addCreator(RandomTourCreator(tsp2.numCities))
      .addCreator(GreedyTourCreator(tsp2.cost1, 3))
      .addCreator(GreedyTourCreator(tsp2.cost2, 3))
      .addCreator(GreedyTourCreator(tsp2.cost1.elementwisePlus(tsp2.cost2), 3))
      .addModifier(SwapTwoCitiesModifier())
      .addDeletor(DeleteDominated())

    val islandManager = new LocalIslandManager[Tour](1, islandBuilder)

    islandManager.runBlocking(StopAfter(100.seconds))

    val paretoFrontier = islandManager.currentParetoFrontier()
    println(paretoFrontier)
    println(paretoFrontier.solutions.take(5))
  }

}
