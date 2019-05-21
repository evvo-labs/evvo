package com.evvo

import com.evvo.island.population._
import org.scalatest.{Matchers, WordSpec}

class ParetoFrontierTest extends WordSpec with Matchers {
  "The Pareto Frontier" should {
    "Not include dominated solutions" in {
      val dominating = Scored[Int](Map(("a", Minimize) -> 0, ("b", Minimize) -> 0), 1)
      val losing = Scored[Int](Map(("a", Minimize) -> 99, ("b", Minimize) -> 99), 2)
      val pop = Set[TScored[Int]](dominating, losing)
      val pf = ParetoFrontier(pop).solutions

      pf should have size 1
      pf should contain(dominating)
      pf should not contain losing
    }

    "Not include solutions that tie on some solutions and lose on others" in {
      val dominating = Scored[Int](Map(("a", Minimize) -> 99, ("b", Minimize) -> 0), 1)
      val losing = Scored[Int](Map(("a", Minimize) -> 99, ("b", Minimize) -> 99), 2)
      val pop = Set[TScored[Int]](dominating, losing)
      val pf = ParetoFrontier(pop).solutions

      pf should have size 1
      pf should contain(dominating)
      pf should not contain losing
    }

    "Respect the direction objectives want to be optimized in" in {
      val lowMinSolution = Scored[Double](Map(("a", Minimize) -> 1d), 2)
      val highMinSolution =  Scored[Double](Map(("a", Minimize) -> 3d), 4)
      val minPop = Set[TScored[Double]](lowMinSolution, highMinSolution)
      val minParetoFrontier = ParetoFrontier(minPop).solutions

      minParetoFrontier should contain(lowMinSolution)
      minParetoFrontier should not contain(highMinSolution)


      val lowMaxSolution = Scored[Double](Map(("a", Maximize) -> 1d), 2)
      val highMaxSolution =  Scored[Double](Map(("a", Maximize) -> 3d), 4)
      val maxPop = Set[TScored[Double]](lowMaxSolution, highMaxSolution)
      val maxParetoFrontier = ParetoFrontier(maxPop).solutions

      maxParetoFrontier should not contain(lowMaxSolution)
      maxParetoFrontier should contain(highMaxSolution)
    }
  }
}
