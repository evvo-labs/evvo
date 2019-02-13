package integration

import com.diatom.TScored
import com.diatom.island.SingleIslandDumi
import org.scalatest.{FlatSpec, Matchers}

/**
  * Tests a single island cluster.
  *
  * The behavior under test is that an Island can sort a list given the proper mutator and fitness
  * function, and terminate successfully returning a set of lists.
  */
class SimpleIslandTest extends FlatSpec with Matchers {

  /** High level concept for the test:
    *
    * Create an island
    * - Supply mutators, deletors, creators
    * - Supply a termination condition
    * - Supply a starting population
    *
    * Start the evolutionary process
    *
    * Wait for the process to terminate, and see if result is sorted.
    */

  type Solution = List[Int]

  val listLength = 100

  def createFunc() = Set(listLength to 1 toList)

  def mutateFunc(s: Set[TScored[Solution]]): Set[Solution] = {
    s.map(scoredSol => {
      val sol = scoredSol.solution
      val i = util.Random.nextInt(sol.length)
      val j = util.Random.nextInt(sol.length)
      val tmp = sol(j)
      sol.updated(j, sol(i)).updated(i, tmp)
    })
  }

  def deleteFunc(s: Set[TScored[Solution]]): Set[TScored[Solution]] = {
    val sums = s.map(_.score.values.sum).toVector.sorted
    val cutoff = sums(s.size / 2)
    s.filter(_.score.values.sum < cutoff)
  }

  def numInversions(s: Solution): Double = {
    (for (partialList <- s.inits) yield {
      partialList match {
        case Nil => 0
        case head :: tail => tail.count(_ < head)
      }
    }).sum
  }

  val pareto: Set[Solution] = SingleIslandDumi.builder[Solution]()
    .addCreator(createFunc)
    .addMutator(mutateFunc)
    .addDeletor(deleteFunc)
    .addFitness(numInversions _)
    .build()
    .run()

  "Single Island Dumi" should "be able to sort a list" in {
    pareto should contain(1 to listLength toList)
  }

}
