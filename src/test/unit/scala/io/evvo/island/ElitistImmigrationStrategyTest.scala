package io.evvo.island

import io.evvo.NullLogger
import io.evvo.island.population.{Maximize, Objective, Scored, StandardPopulation}
import org.scalatest.{Matchers, WordSpec}

class ElitistImmigrationStrategyTest extends WordSpec with Matchers {
  "ElitistImmigrationStrategy" should {
    "allow immigrants on the pareto frontier" in {
      implicit val log = NullLogger

      val objective = new Objective[Int]("test", Maximize) {
        override protected def objective(sol: Int): Double = sol.toDouble
      }

      val population = StandardPopulation(Vector(objective))
      population.addSolutions(Vector(4))

      val solutions = Vector(
        Scored[Int](Map(("test", Maximize) -> 3), 3),
        Scored[Int](Map(("test", Maximize) -> 4), 4),
        Scored[Int](Map(("test", Maximize) -> 5), 5),
        Scored[Int](Map(("test", Maximize) -> 6), 6)
      )
      // Filtering should only keep the solutions that would expand the pareto frontier.
      // Because we added the solution 4 to the population, only solutions with values greater
      // than 4 will be added.
      (ElitistImmigrationStrategy.filter(solutions, population)
        shouldBe solutions.filter(_.solution > 4))
    }
  }

}
