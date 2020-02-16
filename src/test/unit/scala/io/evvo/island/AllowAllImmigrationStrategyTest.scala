package io.evvo.island

import io.evvo.island
import io.evvo.island.population.{Maximize, StandardPopulation}
import org.scalatest.{Matchers, WordSpec}

class AllowAllImmigrationStrategyTest extends WordSpec with Matchers {
  "AllowAllImmigrationStrategy" should {
    "allow all immigrants" in {
      val solutions =
        Vector(
          island.population.Scored(Map(("a", Maximize() -> 3)), 3),
          island.population.Scored(Map(("a", Maximize() -> 4)), 9))
      val population = StandardPopulation[Int](Vector())

      // Filtering should return the whole list of solutions passed in
      AllowAllImmigrationStrategy().addImmigrants(solutions, population)
      population.getSolutions(2) should contain theSameElementsAs solutions
    }
  }
}
