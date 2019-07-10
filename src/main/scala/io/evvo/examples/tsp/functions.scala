
package io.evvo.examples.tsp

import io.evvo.agent.{CreatorFunction, CrossoverFunction, ModifierFunction, MutatorFunction}
import io.evvo.island.population.{Minimize, Objective, Scored}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/** An objective in TSP2.
  * @param name                  the name of this objective
  * @param cost The cost of getting from each city to each city.
  */
case class CostObjective(override val name: String, cost: CostMatrix)
  extends Objective[Tour](name, Minimize) {
  override protected def objective(sol: Tour): Double = {
    sol.sliding(2).map { case Vector(u, v) => cost(u, v) }.sum + cost(sol.last, sol.head)
  }
}

case class RandomTourCreator(numCities: Int, numSolutions: Int = 10)
  extends CreatorFunction[Tour]("RandomTourCreator") {
  override def create(): Iterable[Tour] = {
    Vector.fill(numSolutions)(util.Random.shuffle(Vector.range(0, numCities)))
  }
}

/** Creates a greedy tour, starting from a random city, picking at each step one of the closest N unvisited cities
  * as the next step.
  * @param distanceMatrix The matrix with the distances to be greedy about.
  * @param fromBestN The number to choose.
  */
case class GreedyTourCreator(distanceMatrix: CostMatrix, fromBestN: Int)
  extends CreatorFunction[Tour]("GreedyTourCreator") {
  override def create(): Iterable[Tour] = {
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

case class SwapTwoCitiesModifier() extends MutatorFunction[Tour]("SwapTwoCities") {
  override protected def mutate(sol: Tour): Tour = {
    val index1 = util.Random.nextInt(sol.length)
    val index2 = util.Random.nextInt(sol.length)

    sol.updated(index1, sol(index2)).updated(index2, sol(index1))
  }
}

case class CrossoverModifier() extends CrossoverFunction[Tour](name="Crossover") {
  override protected def crossover(sol1: Tour, sol2: Tour): Tour = {
    val crossoverPoint = util.Random.nextInt(sol1.length)
    val firstHalf = sol1.take(crossoverPoint) // Up to the crossover point, it's all sol1
    // And we need the rest of the cities, so take them from sol2, in the order of sol2
    firstHalf ++ sol2.filterNot(firstHalf.contains)
  }
}
