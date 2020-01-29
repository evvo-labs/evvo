package io.evvo.island

import io.evvo.island.population.{Scored, StandardPopulation}
import org.scalatest.{Matchers, WordSpec}

class AllowAllImmigrationStrategyTest extends WordSpec with Matchers {
  "AllowAllImmigrationStrategy" should {
    "allow all immigrants" in {
      val solutions = Vector(Scored(Map(), 3), Scored(Map(), 9))
      val population = StandardPopulation[Int](Vector())

      // Filtering should return the whole list of solutions passed in
      AllowAllImmigrationStrategy.filter(solutions, population) shouldBe solutions
    }
  }
}
