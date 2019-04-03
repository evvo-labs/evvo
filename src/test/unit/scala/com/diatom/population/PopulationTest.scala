package com.diatom.population

import com.diatom.agent.FitnessFunc
import com.diatom.{Scored, TScored}
import org.scalatest._

class PopulationTest extends WordSpec with Matchers with BeforeAndAfter {
  val identityFitness = FitnessFunc[Double](x => x, "Identity")
  val fitnesses = Set(identityFitness)


  "An empty population" should {
    val emptyPop = Population(fitnesses)

    "not return any solutions" in {
      val sols = emptyPop.getSolutions(1)
      sols shouldBe 'empty
    }

    "not do anything when deleted from" in {
      emptyPop.deleteSolutions(Seq(Scored(Map("a" -> 1.0), 1.0)))

      val sols = emptyPop.getSolutions(1)
      sols shouldBe 'empty
    }

    "become non-empty when added to" in {
      val pop = Population(fitnesses)
      pop.addSolutions(Set(1.0))
      val sols = pop.getSolutions(1)
      sols should not be 'empty
    }

    "have an empty pareto frontier" in {
      val paretoFront = emptyPop.getParetoFrontier()
      paretoFront.solutions shouldBe 'empty
    }

    "has zero elements, according to getInformation()" in {
      val info = emptyPop.getInformation()
      info.numSolutions shouldBe 0
    }
  }

  "A non-empty population" should {
    var pop = Population(fitnesses)
    val popSize = 10
    before {
      pop = Population(fitnesses)
      pop.addSolutions((1 to popSize).map(_.toDouble))
    }

    "not contain elements after the elements are removed" in {
      val sol = pop.getSolutions(1)
      pop.deleteSolutions(sol)
      val remaining = pop.getSolutions(popSize)
      remaining & sol shouldBe 'empty
    }

    "only remove the specified elements" in {
      val before = pop.getSolutions(popSize)
      val sol = pop.getSolutions(1)
      pop.deleteSolutions(sol)
      val after = pop.getSolutions(popSize)
      before &~ after shouldBe sol
    }

    "return random subsections of the population" in {
      val multipleSolSelections: List[Set[TScored[Double]]] =
        List.fill(12)(pop.getSolutions(popSize / 2))
      assert(multipleSolSelections.toSet.size > 1)
    }

    "allow adding elements" in {
      pop.addSolutions(Set(11.0))
      val sol = pop.getSolutions(popSize + 1)
      sol.map(_.solution) should contain(11.0)
    }

    "not allow duplicates" in {
      pop.addSolutions(Set(10.0))
      val sol = pop.getSolutions(popSize + 1)
      sol.size shouldBe popSize
    }

    "have only one element in a one-dimensional pareto frontier" in {
      val p = pop.getParetoFrontier()
      p.solutions.size shouldBe 1
    }

    "have non-zero number of elements, according to getInformation()" in {
      val info = pop.getInformation()
      info.numSolutions shouldBe 10
    }
  }
}
