package io.evvo.agent.defaults

import io.evvo.builtin.deletors.DeleteDominated
import io.evvo.island.population.{Maximize, Minimize, Scored}
import org.scalatest.{Matchers, WordSpec}

class DeleteDominatedTest extends WordSpec with Matchers {
  "DeleteDominated" should {
    "Delete dominated solutions" in {
      val d = DeleteDominated[Int]()

      // These three are not dominated
      val sol1 = Scored[Int](Map(("a", Maximize) -> 2, ("b", Minimize) -> 8), 1)
      val sol2 = Scored[Int](Map(("a", Maximize) -> 3, ("b", Minimize) -> 9), 2)
      val sol3 = Scored[Int](Map(("a", Maximize) -> 4, ("b", Minimize) -> 10), 3)
      // This one is.
      val sol4 = Scored[Int](Map(("a", Maximize) -> 1, ("b", Minimize) -> 20), 4)

      val toDelete = d.delete(Vector(sol1, sol2, sol3, sol4))
      toDelete shouldBe Vector(sol4)
    }
  }

}
