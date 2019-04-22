package com.diatom

import java.util.UUID

import akka.actor.ActorSystem
import com.diatom.island.{EvvoIsland, TerminationCriteria}

import scala.collection.mutable
import scala.concurrent.duration._

object MatrixCluster {

  def main(args: Array[String]): Unit = {

    // it's a list of columns of different integers [0,2]
    type Matrix = Vector[Vector[Int]]

    case class Solution(id: String, matrix: Matrix) {
      override def hashCode(): Int = matrix.hashCode()

      override def equals(obj: Any): Boolean = obj match {
        case Solution(_, thatMatrix) => this.matrix equals thatMatrix
        case _ => false
      }
    }

    val width = 20
    val height = width
    val numClasses = 4

    def numAdjacentEqual: FitnessFunctionType[Solution] = (solution: Solution) => {
      def get(x: Int, y: Int) = {
        Option(solution.matrix
          .applyOrElse(x, (x: Int) => Vector[Int]())
          .applyOrElse(y, (x: Int) => null))
      }

      -(for (x <- solution.matrix.indices) yield {
        for (y <- solution.matrix.head.indices) yield {
          val neighbours = Vector(
            get(x + 1, y),
            get(x - 1, y),
            get(x, y + 1),
            get(x, y - 1),
            get(x - 1, y + 1),
            get(x - 1, y - 1),
            get(x + 1, y + 1),
            get(x + 1, y - 1),
          )

          neighbours.count {
            case Some(neighbourVal) => neighbourVal == solution.matrix(x)(y)
            case None => false
          }
        }
      }).flatten.sum
    }

    def num2StepNeighborsEqual: FitnessFunctionType[Solution] = (solution: Solution) => {
      def get(x: Int, y: Int) = {
        Option(solution.matrix
          .applyOrElse(x, (x: Int) => Vector[Int]())
          .applyOrElse(y, (x: Int) => null))
      }

      -(for (x <- solution.matrix.indices) yield {
        for (y <- solution.matrix.head.indices) yield {
          val neighbours = Vector(
            get(x - 2, y),
            get(x + 2, y),
            get(x - 1, y + 1),
            get(x + 1, y + 1),
            get(x - 1, y - 1),
            get(x + 1, y - 1),
            get(x, y + 2),
            get(x, y - 2))

          neighbours.count {
            case Some(neighbourVal) => neighbourVal == solution.matrix(x)(y)
            case None => false
          }
        }
      }).flatten.sum
    }

    def createMatrix: CreatorFunctionType[Solution] = () => {

      Vector.fill(32)({
        val contents = mutable.Queue(util.Random.shuffle((0 until numClasses)
          .flatMap(c => Vector.fill(width * height / numClasses + 1)(c))): _*)
        Vector.fill(width)(Vector.fill(height)(contents.dequeue()))
      }).map(m => Solution(UUID.randomUUID().toString, m))
        .toSet
    }

    def mutateMatrix: MutatorFunctionType[Solution] = (sols: IndexedSeq[TScored[Solution]]) => {
      def mutate(solution: Solution) = {
        val x1 = util.Random.nextInt(solution.matrix.length)
        val x2 = util.Random.nextInt(solution.matrix.length)
        val y1 = util.Random.nextInt(solution.matrix.head.length)
        val y2 = util.Random.nextInt(solution.matrix.head.length)


        val tmp = solution.matrix(x1)(y1)
        val newMatrix = solution.matrix.updated(x1, solution.matrix(x1).updated(y1, solution.matrix(x2)(y2)))
          .updated(x2, solution.matrix(x2).updated(y2, tmp))
        solution.copy(matrix = newMatrix)
      }

      sols.map(s => mutate(s.solution))
    }


    def deleteDominated: DeletorFunctionType[Solution] = (s: IndexedSeq[TScored[Solution]]) => {
       s.filterNot(elem => ParetoFrontier(s).solutions.contains(elem))
    }

    def deleteWorstHalf: DeletorFunctionType[Solution] = (s: IndexedSeq[TScored[Solution]]) => {
      if (s.isEmpty) {
        s
      } else {
        val funcs = s.head.score.keys.toVector
        val func = funcs(util.Random.nextInt(funcs.size))

        s.toVector.sortBy(_.score(func)).take(s.size / 2).toSet
      }
    }

    def floodFill(cl: Int): FitnessFunctionType[Solution] = {
      sol: Solution => {
        val m = sol.matrix

        def get(x: Int, y: Int) = {
          m.applyOrElse(x, (x: Int) => Vector[Int]())
            .andThen(Option[Int])
            .applyOrElse(y, (x: Int) => None)
        }

        var max = 0
        val seen = mutable.Set[(Int, Int)]()


        def flood(x: Int, y: Int): Int = {
          if (seen contains(x, y)) {
            return 0
          }

          seen += ((x, y))
          get(x, y) match {
            case Some(v) if v == cl =>
              1 + flood(x - 1, y) + flood(x + 1, y) + flood(x, y + 1) + flood(x, y - 1)
            case _ => 0
          }
        }

        for (x <- m.indices) {
          for (y <- m.head.indices) {
            val floodSize = flood(x, y)
            max = math.max(max, floodSize)
          }
        }

        -max.toDouble
      }
    }

    def allFloods: FitnessFunctionType[Solution] =
      sol => {
        (0 until numClasses).map(c => floodFill(c)(sol)).sum
      }

    implicit val system = ActorSystem("MatrixCluster")
    val island = EvvoIsland.builder[Solution]()
      .addCreator(createMatrix)
      .addMutator(mutateMatrix)
      .addMutator(mutateMatrix)
      .addMutator(mutateMatrix)
      .addMutator(mutateMatrix)
      .addDeletor(deleteWorstHalf)
      .addDeletor(deleteWorstHalf)
      .addDeletor(deleteWorstHalf)
      .addDeletor(deleteWorstHalf)
      .addDeletor(deleteDominated)
      .addDeletor(deleteDominated)
      .addFitness(numAdjacentEqual)
      .addFitness(allFloods)
      .build()

    val pareto = island.run(TerminationCriteria(10.minutes))
    println(s"pareto = ${pareto}")

  }
}
