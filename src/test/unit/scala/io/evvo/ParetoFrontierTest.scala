package io.evvo

import io.evvo.island.population._
import org.scalatest.{Matchers, WordSpec}

class ParetoFrontierTest extends WordSpec with Matchers {
  "The Pareto Frontier" should {
    "Not include dominated solutions" in {
      val dominating = Scored[Int](Map(("a", Minimize) -> 0, ("b", Minimize) -> 0), 1)
      val losing = Scored[Int](Map(("a", Minimize) -> 99, ("b", Minimize) -> 99), 2)
      val pop = Set[Scored[Int]](dominating, losing)
      val pf = ParetoFrontier(pop).solutions

      pf should have size 1
      pf should contain(dominating)
      pf should not contain losing
    }

    "Not include solutions that tie on some solutions and lose on others" in {
      val dominating = Scored[Int](Map(("a", Minimize) -> 99, ("b", Minimize) -> 0), 1)
      val losing = Scored[Int](Map(("a", Minimize) -> 99, ("b", Minimize) -> 99), 2)
      val pop = Set[Scored[Int]](dominating, losing)
      val pf = ParetoFrontier(pop).solutions

      pf should have size 1
      pf should contain(dominating)
      pf should not contain losing
    }

    "Respect the direction objectives want to be optimized in" in {
      val lowMinSolution = Scored[Double](Map(("a", Minimize) -> 1d), 2)
      val highMinSolution =  Scored[Double](Map(("a", Minimize) -> 3d), 4)
      val minPop = Set[Scored[Double]](lowMinSolution, highMinSolution)
      val minParetoFrontier = ParetoFrontier(minPop).solutions

      minParetoFrontier should contain(lowMinSolution)
      minParetoFrontier should not contain(highMinSolution)


      val lowMaxSolution = Scored[Double](Map(("a", Maximize) -> 1d), 2)
      val highMaxSolution =  Scored[Double](Map(("a", Maximize) -> 3d), 4)
      val maxPop = Set[Scored[Double]](lowMaxSolution, highMaxSolution)
      val maxParetoFrontier = ParetoFrontier(maxPop).solutions

      maxParetoFrontier should not contain(lowMaxSolution)
      maxParetoFrontier should contain(highMaxSolution)
    }

    "Always be dominated if empty" in {
      assert(ParetoFrontier[String](Set()).dominatedBy(Scored(Map(("a", Maximize) -> 0), "test")))
    }

    "Never dominate if empty" in {
      assert(!ParetoFrontier[String](Set()).dominates(Scored(Map(("a", Maximize) -> 0), "test")))
    }

    // A Pareto Frontier with just 3
    val sol3 = Scored(Map(("a", Maximize) -> 3), "str")
    val pf3 = ParetoFrontier[String](Set(sol3))

    "Dominate points that would not be on the Pareto frontier if added" in {
      assert(pf3.dominates(Scored(Map(("a", Maximize) -> 2), "str")))
    }

    "Be dominated by points that would extend the Pareto frontier if added" in {
      assert(pf3.dominatedBy(Scored(Map(("a", Maximize) -> 4), "str")))
    }

    "Not dominate or be dominated by points that are on the Pareto frontier exactly" in {
      assert(!pf3.dominates(sol3))
      assert(!pf3.dominatedBy(sol3))
    }
  }
}
