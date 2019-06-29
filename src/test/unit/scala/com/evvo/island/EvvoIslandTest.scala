package com.evvo.island

import com.evvo.NullLogger
import com.evvo.island.population.{Maximize, Objective, Scored}
import org.scalatest.{Matchers, WordSpec}

class EvvoIslandTest extends WordSpec with Matchers {
  "EvvoIsland" should {
    "use immigration strategy to filter incoming solutions" in {
      implicit val log = NullLogger

      object MaximizeInt extends Objective[Int]("Test", Maximize) {
        override protected def objective(sol: Int): Double = sol
      }

      val island = new EvvoIsland(
          Vector(),
          Vector(),
          Vector(),
          Vector(MaximizeInt),
          ElitistImmigrationStrategy)

      island.immigrate(Seq(Scored[Int](Map(("Test", Maximize) -> 10), 10)))
      island.immigrate(Seq(Scored[Int](Map(("Test", Maximize) -> 3), 3)))

      // The three shouldn't be added, because Elitist will prevent anything < 10 from being added
      island.currentParetoFrontier().solutions should have size 1

      // But 11 should make it through.
      val solution11 = Scored[Int](Map(("Test", Maximize) -> 11), 11)
      island.immigrate(Seq(solution11))
      island.currentParetoFrontier().solutions should be(Set(solution11))
    }
  }

}
