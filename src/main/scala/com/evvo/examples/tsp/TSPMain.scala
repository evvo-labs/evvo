package com.evvo.examples.tsp

object TSPMain {

  def main(args: Array[String]): Unit = {

    // Read sample data
    val filename1 = args(0)
    val filename2 = args(1)

    def parseEuclideanCoordinates(filename: String): IndexedSeq[(Double,Double)] = {

      val data = scala.io.Source.fromFile(filename)
      data.getLines()
        .map(
          _.split(" ")
            .drop(1)
            .map(_.toDouble))
        .map(arr => (arr(0), arr(1)))
        .toVector
    }

    val tsp2 = TSP2.fromEuclideanPoints(parseEuclideanCoordinates(filename1), parseEuclideanCoordinates(filename2))
    print(tsp2)




  }

}
