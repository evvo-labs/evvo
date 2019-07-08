package com.evvo.examples.tsp

/**
  * An instance of travelling salesman problem with two objectives
  *
  *
  * @param cost1 The cost of getting from each city to each, by the first objective.
  * @param cost2 The cost of getting from each city to each, by the second objective.
  */
case class TSP2(cost1: CostMatrix, cost2: CostMatrix) {
  require(cost1.numCities == cost2.numCities)
  def numCities: Int = cost1.numCities
}

object TSP2 {

  /**
    * Derives instance of TSP2 from two sets of euclidean coordinates, each corresponding to one cost or distance
    * objective.
    */
  def fromEuclideanPoints(coordsA: IndexedSeq[(Double, Double)],
                          coordsB: IndexedSeq[(Double, Double)]
                         ): TSP2 = {
    def costMatrix(coords: IndexedSeq[(Double, Double)]): CostMatrix = {
      CostMatrix(Vector.tabulate(coords.length, coords.length)((city1index, city2index) =>
        euclideanDistance(coords(city1index), coords(city2index))))
    }

    def euclideanDistance(city1: (Double, Double), city2: (Double, Double)): Double =
      math.sqrt(math.pow(city1._1 - city2._1, 2) + math.pow(city1._2 - city2._2, 2))

    TSP2(costMatrix(coordsA), costMatrix(coordsB))
  }
}



