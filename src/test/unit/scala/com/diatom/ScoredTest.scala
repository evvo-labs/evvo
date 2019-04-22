package com.diatom

import org.scalatest.{Matchers, WordSpec}

class ScoreHashingTest extends WordSpec with Matchers {
  "A Scored" should {
    "hash on scores when hashStrategy = ON_SCORES" in {
      val a = Scored(Map("a" -> 3, "b" -> 5), 3, HashingStrategy.ON_SCORES)
      val b = Scored(Map("a" -> 2, "b" -> 5), 3, HashingStrategy.ON_SCORES)

      a should not be b
      b should not be a
      a.hashCode() should not be b.hashCode()


      val three = Scored(Map("a" -> 3, "b" -> 5), 3, HashingStrategy.ON_SCORES)
      val four = Scored(Map("a" -> 3, "b" -> 5), 4, HashingStrategy.ON_SCORES)

      three shouldBe four
      four shouldBe three
      three.hashCode() shouldBe four.hashCode()
    }

    "hash on scores when hashStrategy = ON_SOLUTIONS" in {
      val a = Scored(Map("a" -> 3, "b" -> 5), 3, HashingStrategy.ON_SOLUTIONS)
      val b = Scored(Map("a" -> 2, "b" -> 5), 3, HashingStrategy.ON_SOLUTIONS)

      a shouldBe b
      b shouldBe a
      a.hashCode() shouldBe b.hashCode()


      val three = Scored(Map("a" -> 3, "b" -> 5), 3, HashingStrategy.ON_SOLUTIONS)
      val four = Scored(Map("a" -> 3, "b" -> 5), 4, HashingStrategy.ON_SOLUTIONS)

      three should not be four
      four should not be three
      three.hashCode() should not be four.hashCode()
    }

    "default to hashing on scores" in {
      Scored(Map(), null).hashStrategy shouldBe HashingStrategy.ON_SCORES
    }
  }
}
