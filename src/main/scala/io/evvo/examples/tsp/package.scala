package io.evvo.examples

package object tsp {
  /**
    * Represents an ordered sequence of visited cities by index.
    */
  type Tour = IndexedSeq[Int]

  /**
    * When indexed by (from,to) gives cost from `from` to `to` by this objective.
    */
  case class CostMatrix(matrix: IndexedSeq[IndexedSeq[Double]]) {
    /** For each city, each other city sorted by distance by A. */
    private val citiesByDistanceA: IndexedSeq[IndexedSeq[Int]] = {
      import Ordering.Double.TotalOrdering
      this.matrix.map(distances => distances.indices.sortBy(distances))
    }

    def numCities: Int = matrix.length

    /**
      *
      * @param origin The city to start at, the city that the other cities are closest to.
      * @param n      The number of cities to return.
      * @return The N closest cities to origin by A
      */
    def closestNCities(origin: Int, n: Int): IndexedSeq[Int] = citiesByDistanceA(origin).tail.take(n)

    /**
      * Same as above, but it only returns the closest N that don't pass the filter.
      */
    def closesNCitiesOtherThan(origin: Int, n: Int, ignore: Int => Boolean): IndexedSeq[Int] = {
      citiesByDistanceA(origin).tail.filterNot(ignore).take(n)
    }

    /** @return The elementwise sum of each element of this matrix and that one. */
    def elementwisePlus(that: CostMatrix): CostMatrix = {
      CostMatrix(matrix.zip(that.matrix).map {
        case (thisRow, thatRow) => thisRow.zip(thatRow).map {
          case (thisElem, thatElem) => thisElem + thatElem
        }
      })
    }

    /** @return the distance from `from` to `to` */
    def apply(from: Int, to: Int): Double = {
      this.matrix(from)(to)
    }
  }
}
