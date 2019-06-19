
package com.evvo.examples.tsp

import com.evvo.island.population.{Minimize, Objective}


class DistanceA(distances: DistanceMatrix) extends Objective[TSPSolution]("distanceA", Minimize) {

  override protected def objective(sol: TSPSolution): Double = {
    sol.sliding(2).map{case Vector(u,v) => distances(u)(v)._1}.sum + distances(sol.last)(sol.head)._1
  }
}

class DistanceB(distances: DistanceMatrix) extends Objective[TSPSolution]("distanceB", Minimize) {

  override protected def objective(sol: TSPSolution): Double = {
    sol.sliding(2).map{case Vector(u,v) => distances(u)(v)._2}.sum + distances(sol.last)(sol.head)._2
  }
}
