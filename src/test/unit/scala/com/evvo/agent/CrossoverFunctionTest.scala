package com.evvo.agent

import com.evvo.island.population.Scored
import org.scalatest.{Matchers, WordSpec}

class CrossoverFunctionTest extends WordSpec with Matchers {
  "CrossoverFunction" should {
    val crossoverFunction = new CrossoverFunction[Double]("+") {
      override protected def crossover(sol1: Double, sol2: Double): Double = {
        sol1 + sol2
      }
    }

    "work on odd numbers of solutions" in {
      crossoverFunction.modify(Vector(Scored(Map(), 0d)))
    }

    "apply the function to pairs" in {
      val results = crossoverFunction.modify(
        Vector(Scored(Map(), 1d), Scored(Map(), 3d))).toVector

      results should have length 1
      results should contain(4d)
    }
  }
}
