package io.evvo.agent

import io.evvo.island.population.Scored
import org.scalatest.{Matchers, WordSpec}

class MutatorFunctionTest extends WordSpec with Matchers {
  "MutatorFunction" should {
    "apply a one-to-one mapping" in {
      val mutator = new MutatorFunction[Double]("Add1") {
        override protected def mutate(sol: Double): Double = {
          sol + 1
        }
      }

      mutator.modify(Vector()).iterator should have length 0
      mutator.modify(Vector(Scored(Map(), 3d))) should contain(4d)

      val fourAndFive = mutator.modify(Vector(Scored(Map(), 3d), Scored(Map(), 4d)))
          .iterator.toVector
      fourAndFive should have length 2
      fourAndFive should contain(4d)
      fourAndFive should contain(5d)
    }
  }
}
