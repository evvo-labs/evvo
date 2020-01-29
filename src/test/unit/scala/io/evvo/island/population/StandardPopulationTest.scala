package io.evvo.island.population

import io.evvo.island.population
import org.scalatest._

class StandardPopulationTest extends WordSpec with Matchers with BeforeAndAfter {

  val identityFitness = new Objective[Double]("Identity", Minimize) {
    override protected def objective(sol: Double): Double = sol
  }
  val fitnesses = Set(identityFitness)

  "An empty population" should {
    val emptyPop = StandardPopulation(fitnesses)

    "not return any solutions" in {
      val sols = emptyPop.getSolutions(1)
      sols.length shouldBe 0
    }

    "not do anything when deleted from" in {
      emptyPop.deleteSolutions(Seq(Scored(Map(("a", Minimize) -> 1.0), 1.0)))

      val sols = emptyPop.getSolutions(1)
      sols.length shouldBe 0
    }

    "become non-empty when added to" in {
      val pop = StandardPopulation(fitnesses)
      pop.addSolutions(Set(1.0))
      val sols = pop.getSolutions(1)
      sols.length should not be 0
    }

    "have an empty pareto frontier" in {
      val paretoFront = emptyPop.getParetoFrontier()
      paretoFront.solutions shouldBe empty
    }

    "has zero elements, according to getInformation()" in {
      val info = emptyPop.getInformation()
      info.numSolutions shouldBe 0
    }
  }
  val popSize = 10
  val returnOne: Objective[Double] = new Objective[Double]("one", Maximize) {
    override protected def objective(sol: Double): Double = 1
  }
  "A non-empty population hashing on solutions" should {
    before {
      pop = population.StandardPopulation(fitnesses, HashingStrategy.ON_SOLUTIONS)
      pop.addSolutions((1 to popSize).map(_.toDouble))
    }

    "not contain elements after the elements are removed" in {
      val sol = pop.getSolutions(1)
      pop.deleteSolutions(sol)
      val remaining = pop.getSolutions(popSize)
      remaining.toSet & sol.toSet shouldBe empty
    }

    "only remove the specified elements" in {
      val before = pop.getSolutions(popSize)
      val sol = pop.getSolutions(1)
      pop.deleteSolutions(sol)
      val after = pop.getSolutions(popSize)
      before.toSet &~ after.toSet shouldBe sol.toSet
    }

    "return random subsections of the population" in {
      val multipleSolSelections: List[Set[Scored[Double]]] =
        List.fill(12)(pop.getSolutions(popSize / 2).toSet)
      assert(multipleSolSelections.toSet.size > 1)
    }

    "allow adding elements" in {
      pop.addSolutions(Set(11.0))
      val sol = pop.getSolutions(popSize + 1)
      sol.map(_.solution) should contain(11.0)
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
  val uniqueScore: Objective[Double] = new Objective[Double]("one", Maximize) {
    var counter = 0

    override protected def objective(sol: Double): Double = {
      counter += 1
      counter
    }
  }
  var pop: StandardPopulation[Double] = _

  "A population hashing on solutions" should {

    "not allow duplicate solutions" in {
      val pop = StandardPopulation(Vector(uniqueScore), HashingStrategy.ON_SOLUTIONS)
      pop.addSolutions(Vector(1, 1))
      val sol = pop.getSolutions(2)
      sol.length shouldBe 1
    }

    "allow duplicate scores for different solutions" in {
      val pop = StandardPopulation(Vector(returnOne), HashingStrategy.ON_SOLUTIONS)
      pop.addSolutions(Vector(1, 2))
      val sol = pop.getSolutions(2)
      sol.length shouldBe 2
    }
  }

  "A population hashing on scores" should {

    "not allow duplicate scores" in {
      val pop = StandardPopulation(Vector(returnOne), HashingStrategy.ON_SCORES)
      pop.addSolutions(Vector(1, 2))
      val sol = pop.getSolutions(2)
      sol.length shouldBe 1
    }

    "allow duplicate solutions for different scores" in {
      val pop = StandardPopulation(Vector(uniqueScore), HashingStrategy.ON_SCORES)
      pop.addSolutions(Vector(1, 1))
      val sol = pop.getSolutions(2)
      sol.length shouldBe 2
    }
  }
}
