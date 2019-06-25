
package com.evvo.examples.tsp

import com.evvo.agent.{CreatorFunction, MutatorFunction}
import com.evvo.island.population.{Minimize, Objective}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer


case class Distance(override val name: String, distances: DistanceMatrix)
  extends Objective[TSPSolution](name, Minimize) {
  override protected def objective(sol: TSPSolution): Double = {
    sol.sliding(2).map { case Vector(u, v) => distances(u, v) }.sum + distances(sol.last, sol.head)
  }
}

case class RandomTourCreator(numCities: Int, numSolutions: Int = 10)
  extends CreatorFunction[TSPSolution]("RandomTourCreator") {
  override def create(): TraversableOnce[TSPSolution] = {
    Vector.fill(numSolutions)(util.Random.shuffle((0 until numCities).toVector))
  }
}

/**
  * Creates a greedy tour, starting from a random city, picking at each step one of the closest N unvisited cities
  * as the next step.
  * @param distanceMatrix The matrix with the distances to be greedy about.
  * @param fromBestN The number to choose.
  */
case class GreedyTourCreator(distanceMatrix: DistanceMatrix, fromBestN: Int)
  extends CreatorFunction[TSPSolution]("GreedyTourCreator") {
  override def create(): TraversableOnce[TSPSolution] = {
    val tour: mutable.ArrayBuffer[Int] = mutable.ArrayBuffer()
    val visited: mutable.Set[Int] = mutable.Set[Int]()

    var currentCity: Int = util.Random.nextInt(distanceMatrix.numCities)
    tour += currentCity
    visited += currentCity

    while (tour.length < distanceMatrix.numCities) {
      // get the possible next cities
      val possibleNextCities = distanceMatrix.closesNCitiesOtherThan(currentCity, fromBestN, visited)
      // pick one and add it
      currentCity = possibleNextCities(util.Random.nextInt(possibleNextCities.length))
      tour += currentCity
      visited += currentCity
    }
    Vector(tour.toVector)
  }
}

case class SwapTwoCitiesModifier() extends MutatorFunction[TSPSolution]("SwapTwoCities") {
  override protected def mutate(sol: TSPSolution): TSPSolution = {
    val index1 = util.Random.nextInt(sol.length)
    val index2 = util.Random.nextInt(sol.length)

    sol.updated(index1, sol(index2)).updated(index2, sol(index1))
  }
}
