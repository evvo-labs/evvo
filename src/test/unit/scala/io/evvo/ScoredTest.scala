package io.evvo

import io.evvo.island.population.{Maximize, Minimize, Scored}
import org.scalatest.{Matchers, WordSpec}

class ScoredTest extends WordSpec with Matchers {
  "Scored" should {
    "hash on scores " in {
      val a = Scored(Map(("a", Minimize() -> 3), ("b", Minimize() -> 5)), 3)
      val b = Scored(Map(("a", Minimize() -> 2), ("b", Minimize() -> 5)), 3)

      a should not be b
      b should not be a
      a.hashCode() should not be b.hashCode()

      val three =
        Scored(Map(("a", Minimize() -> 3), ("b", Minimize() -> 5)), 3)
      val four =
        Scored(Map(("a", Minimize() -> 3), ("b", Minimize() -> 5)), 4)

      three shouldBe four
      four shouldBe three
      three.hashCode() shouldBe four.hashCode()
    }

    // None because solution doesn't matter for domination,
    // Minimize() included to ensure code works both ways.
    val a3b4c5 =
      Scored(
        Map(("a", (Maximize() -> 3)), ("b", (Maximize() -> 4)), ("c", (Minimize() -> 5))),
        None)

    "dominate solutions if all scores are better" in {
      assert(
        a3b4c5.dominates(
          Scored(
            Map(("a", (Maximize() -> 0)), ("b", (Maximize() -> 0)), ("c", (Minimize() -> 10))),
            None)
        )
      )
    }

    "dominate solutions if all scores are tied with at least one better" in {
      assert(
        a3b4c5.dominates(
          Scored(
            Map(("a", (Maximize() -> 3)), ("b", (Maximize() -> 3)), ("c", (Minimize() -> 5))),
            None)
        )
      )
    }

    "not dominate solutions if all scores are tied" in {
      assert(!a3b4c5.dominates(a3b4c5))
    }

    "not dominate solutions that have at least one score better" in {
      assert(
        !a3b4c5.dominates(
          Scored(
            Map(("a", (Maximize() -> 4)), ("b", (Maximize() -> 4)), ("c", (Minimize() -> 5))),
            None)
        )
      )

      assert(
        !a3b4c5.dominates(
          Scored(
            Map(("a", (Maximize() -> 100)), ("b", (Maximize() -> 100)), ("c", (Minimize() -> 0))),
            None)
        )
      )
    }
  }
}
