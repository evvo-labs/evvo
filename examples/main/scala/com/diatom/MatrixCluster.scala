package com.diatom

import com.diatom.island.{SingleIslandEvvo, TerminationCriteria}

import scala.collection.mutable
import scala.concurrent.duration._

object MatrixCluster extends App {

  // it's a list of columns of different integers [0,2]
  type Solution = Vector[Vector[Int]]


  val width = 5
  val height = 6
  val numClasses = 3

  def numAdjacentEqual: FitnessFunctionType[Solution] = (solution: Solution) => {
    def get(x: Int, y: Int) = {
      Option(solution
        .applyOrElse(x, (x: Int) => Vector[Int]())
        .applyOrElse(y, (x: Int) => null))
    }

    - (for (x <- solution.indices) yield {
      for (y <- solution.head.indices) yield {
        val neighbours = Vector(
          get(x + 1, y),
          get(x - 1, y),
          get(x, y + 1),
          get(x, y - 1))

        neighbours.count {
          case Some(neighbourVal) => neighbourVal == solution(x)(y)
          case None => false
        }
      }
    }).flatten.sum
  }


  def createMatrix: CreatorFunctionType[Solution] = () => {

    Vector.fill(32)({
      val contents = mutable.Queue(util.Random.shuffle((0 until numClasses)
        .flatMap(c => Vector.fill(width * height / numClasses + 1)(c))):_*)
      Vector.fill(width)(Vector.fill(height)(contents.dequeue()))
    }).toSet
  }

  def mutateMatrix: MutatorFunctionType[Solution] = (sols: Set[TScored[Solution]]) => {
    def mutate(solution: Solution) = {
      val x1 = util.Random.nextInt(solution.length)
      val x2 = util.Random.nextInt(solution.length)
      val y1 = util.Random.nextInt(solution.head.length)
      val y2 = util.Random.nextInt(solution.head.length)


      val tmp = solution(x1)(y1)
      solution.updated(x1, solution(x1).updated(y1, solution(x2)(y2)))
        .updated(x2, solution(x2).updated(y2, tmp))
    }

    sols.map(s => mutate(s.solution))
  }


  def deleteHalf: DeletorFunctionType[Solution] = (s: Set[TScored[Solution]]) => {
    if (s.isEmpty) {
      s
    } else {
      val sums = s.map(_.score.values.sum).toVector.sorted
      val cutoff = sums(sums.size / 2)
      s.filter(_.score.values.sum > cutoff)
    }
  }


  val island = SingleIslandEvvo.builder[Solution]()
    .addCreator(createMatrix)
    .addMutator(mutateMatrix)
    .addMutator(mutateMatrix)
    .addMutator(mutateMatrix)
    .addMutator(mutateMatrix)
    .addDeletor(deleteHalf)
    .addDeletor(deleteHalf)
    .addDeletor(deleteHalf)
    .addDeletor(deleteHalf)
    .addDeletor(deleteHalf)
    .addFitness(numAdjacentEqual)
    .build()


  val pareto = island.run(  TerminationCriteria(10.seconds))
}
