package com.evvo.agent

import com.evvo.agent.defaults.DeleteWorstHalfByRandomObjective
import com.evvo.island.population.{Maximize, Minimize, Scored}
import org.scalatest.{Matchers, WordSpec}

class DeleteWorstHalfByRandomObjectiveTest extends WordSpec with Matchers {
  "DeleteWorstHalfByRandomObjective" should {
    val scored1 = Scored(Map(("a", Maximize) -> 1), 12)
    val scored2 = Scored(Map(("a", Maximize) -> 2), 13)
    val scored3 = Scored(Map(("a", Maximize) -> 3), 14)
    val scored4 = Scored(Map(("a", Maximize) -> 4), 14)

    val deletor = (DeleteWorstHalfByRandomObjective[Int]().delete _).andThen(_.toVector)

    "not error if given empty input" in {
      deletor(Vector())
    }

    "delete the worst half" in {
      val toDelete = deletor(Vector(scored1, scored2, scored3, scored4))
      toDelete should have length 2
      toDelete should contain (scored1)
      toDelete should contain (scored2)
    }

    "sometimes pick different objectives" in {
      val scored1 = Scored(Map(("a", Maximize) -> 1, ("b", Minimize) -> 1), 12)
      val scored2 = Scored(Map(("a", Maximize) -> 2, ("b", Minimize) -> 2), 12)

      // it should have two results, one when it picks "a", one when it picks "b"
      Vector.fill(100)(deletor(Vector(scored1, scored2))).toSet should have size 2
    }
  }
}
