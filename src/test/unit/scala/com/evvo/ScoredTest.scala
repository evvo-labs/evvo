package com.evvo

import com.evvo.island.population.{
  HashingStrategy, Minimize, Scored}
import org.scalatest.{Matchers, WordSpec}

class ScoreHashingTest extends WordSpec with Matchers {
  "A Scored" should {
    "hash on scores when hashStrategy = ON_SCORES" in {
      val a = Scored(Map(("a", Minimize) -> 3, ("b", Minimize) -> 5), 3,
        HashingStrategy.ON_SCORES)
      val b = Scored(Map(("a", Minimize) -> 2, ("b", Minimize) -> 5), 3,
        HashingStrategy.ON_SCORES)

      a should not be b
      b should not be a
      a.hashCode() should not be b.hashCode()


      val three = Scored(Map(("a", Minimize) -> 3, ("b", Minimize) -> 5), 3,
        HashingStrategy.ON_SCORES)
      val four = Scored(Map(("a", Minimize) -> 3, ("b", Minimize) -> 5), 4,
        HashingStrategy.ON_SCORES)

      three shouldBe four
      four shouldBe three
      three.hashCode() shouldBe four.hashCode()
    }

    "hash on solutions when hashStrategy = ON_SOLUTIONS" in {
      val a = Scored(Map(("a", Minimize) -> 3, ("b", Minimize) -> 5), 3,
        HashingStrategy.ON_SOLUTIONS)
      val b = Scored(Map(("a", Minimize) -> 2, ("b", Minimize) -> 5), 3,
        HashingStrategy.ON_SOLUTIONS)

      a shouldBe b
      b shouldBe a
      a.hashCode() shouldBe b.hashCode()


      val three = Scored(Map(("a", Minimize) -> 3, ("b", Minimize) -> 5), 3,
        HashingStrategy.ON_SOLUTIONS)
      val four = Scored(Map(("a", Minimize) -> 3, ("b", Minimize) -> 5), 4,
        HashingStrategy.ON_SOLUTIONS)

      three should not be four
      four should not be three
      three.hashCode() should not be four.hashCode()
    }

    "default to hashing on scores" in {
      Scored[Double](Map(), 3).hashStrategy shouldBe
        HashingStrategy.ON_SCORES
    }
  }
}
