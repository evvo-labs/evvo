package com.evvo.examples.tsp

import com.evvo.agent.defaults.DeleteDominated
import com.evvo.island.{EvvoIsland, EvvoIslandBuilder, IslandManager, LocalIslandManager, StopAfter}
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
      .addObjective(Distance("DistanceA", tsp2.distanceA))
      .addObjective(Distance("DistanceB", tsp2.distanceB))
      .addCreator(RandomTourCreator(tsp2.numCities))
      .addCreator(GreedyTourCreator(tsp2.distanceA, 3))
      .addCreator(GreedyTourCreator(tsp2.distanceB, 3))
      .addCreator(GreedyTourCreator(tsp2.distanceA.elementwisePlus(tsp2.distanceB), 3))
      .addModifier(SwapTwoCitiesModifier())
      .addDeletor(DeleteDominated())

    val islandManager = new LocalIslandManager[TSPSolution](10, islandBuilder)

    islandManager.runBlocking(StopAfter(100.seconds))

    val paretoFrontier = islandManager.currentParetoFrontier()
    println(paretoFrontier)
    println(paretoFrontier.solutions.take(5))
  }

}
