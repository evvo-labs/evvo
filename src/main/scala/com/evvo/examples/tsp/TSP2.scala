package com.evvo.examples.tsp

/**
  * An instance of travelling salesman problem with two objectives
  *
  * @param distanceA indexed by (from,to) gives distance by first objective
  * @param distanceB indexed by (from,to) gives distance by second objective
  */
case class TSP2(distanceA: DistanceMatrix, distanceB: DistanceMatrix) {
  def numCities: Int = distanceA.numCities
}

object TSP2 {

  /**
    * Derives instance of TSP2 from two sets of euclidean coordinates, each corresponding to one cost or distance
    * objective.
    */
  def fromEuclideanPoints(coordsA: IndexedSeq[(Double, Double)], coordsB: IndexedSeq[(Double, Double)]): TSP2 = {

    def euclideanDistance(city1: (Double, Double), city2: (Double, Double)): Double =
      math.sqrt(math.pow(city1._1 - city2._1, 2) + math.pow(city1._2 - city2._2, 2))

    assert(coordsA.length == coordsB.length)
    val distanceA = Vector.tabulate(coordsA.length, coordsA.length)((city1index, city2index) =>
      euclideanDistance(coordsA(city1index), coordsA(city2index)))

    val distanceB = Vector.tabulate(coordsB.length, coordsB.length)((city1index, city2index) =>
      euclideanDistance(coordsB(city1index), coordsB(city2index)))

    TSP2(DistanceMatrix(distanceA), DistanceMatrix(distanceB))
  }
}



