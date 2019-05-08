package com.diatom.agent

import org.scalatest.{Matchers, WordSpec}
import com.diatom.agent.default.DeleteDominated
import com.diatom.island.population
import com.diatom.island.population.{Maximize, Minimize, Scored}

class DeleteDominatedFunctionTest extends WordSpec with Matchers {
  val scored1 = population.Scored(Map(("a", Maximize) -> 1), 12)
  val scored2 = population.Scored(Map(("a", Maximize) -> 2), 13)
  val scored3 = population.Scored(Map(("a", Maximize) -> 3), 14)

  val deleteDominated = DeleteDominated[Int](5).delete.andThen(_.toVector)
  "A DeleteDominated function" should {
    "not error on empty sets" in {
      deleteDominated(Vector())
    }

    "keep one solution, if there's only one" in {
      val toDelete = deleteDominated(Vector(scored1))
      toDelete should have length 0
    }

    "keep non-dominated set, if there are multiple" in {
      val toDelete = deleteDominated(Vector(scored1, scored3, scored2))
      toDelete should have length 2
      toDelete should contain (scored1)
      toDelete should contain (scored2)
      toDelete should not contain (scored3)
    }
  }
}
