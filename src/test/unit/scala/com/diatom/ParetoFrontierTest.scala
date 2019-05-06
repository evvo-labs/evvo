package com.diatom

import com.diatom.island.population.{ParetoFrontier, Scored}
import org.scalatest.{Matchers, WordSpec}

class ParetoFrontierTest extends WordSpec with Matchers {
  "The Pareto Frontier" should {
    "Not include dominated solutions" in {
      val dominating = Scored[Int](Map("a" -> 0, "b" -> 0), 1)
      val losing = Scored[Int](Map("a" -> 99, "b" -> 99), 2)
      val pop = Vector(dominating, losing)
      val pf = ParetoFrontier(pop).solutions

      pf should have size 1
      pf should contain(dominating)
      pf should not contain losing
    }

    "Not include solutions that tie on some solutions and lose on others" in {
      val dominating = Scored[Int](Map("a" -> 99, "b" -> 0), 1)
      val losing = Scored[Int](Map("a" -> 99, "b" -> 99), 2)
      val pop = Vector(dominating, losing)
      val pf = ParetoFrontier(pop).solutions

      pf should have size 1
      pf should contain(dominating)
      pf should not contain losing
    }
  }
}
