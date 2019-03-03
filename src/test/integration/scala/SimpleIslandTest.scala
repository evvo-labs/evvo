package integration

import com.diatom.TScored
import com.diatom.island.{SingleIslandEvvo, TerminationCriteria}
import com.diatom.tags.Integration
import org.scalatest.{FlatSpec, Matchers, WordSpec}
import scala.concurrent.duration._

/**
  * Tests a single island cluster.
  *
  * The behavior under test is that an Island can sort a list given the proper mutator and fitness
  * function, and terminate successfully returning a set of lists.
  */
class SimpleIslandTest extends WordSpec with Matchers {

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

  val listLength = 10

  def createFunc(): Set[Solution] = {
    val out = Vector.fill(10)((listLength to 1 by -1).toList).map(mutate).toSet
    out
  }

  def mutate(sol: Solution): Solution = {
    val i = util.Random.nextInt(sol.length)
    val j = util.Random.nextInt(sol.length)
    val tmp = sol(j)
    sol.updated(j, sol(i)).updated(i, tmp)
  }

  def mutateFunc(s: Set[TScored[Solution]]): Set[Solution] = {
    s.map(scoredSol => {
      val sol = scoredSol.solution
      val out = mutate(sol)
      out
    })
  }

  def deleteFunc(s: Set[TScored[Solution]]): Set[TScored[Solution]] = {
    val sums = s.map(_.score.values.sum).toVector.sorted
    val cutoff = sums(s.size / 2)
    s.filter(_.score.values.sum > cutoff)
  }

  def numInversions(s: Solution): Double = {
    (for ((elem, index) <- s.zipWithIndex) yield {
      s.drop(index).count(_ < elem)
    }).sum
  }


  val terminate = TerminationCriteria(10.seconds)

  "Single Island Evvo" should {
    "be able to sort a list within ten seconds" taggedAs Integration in {

      val pareto: Set[Solution] = SingleIslandEvvo.builder[Solution]()
        .addCreator(createFunc)
        .addMutator(mutateFunc)
        .addDeletor(deleteFunc)
        .addFitness(numInversions)
        .build()
        .run(terminate)
        .solutions
        .map(_.solution)

      pareto should contain(1 to listLength toList)
    }
  }

}
