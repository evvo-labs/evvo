package io.evvo.island.population

/** Represents the topology of a network of islands.
  */
trait NetworkTopology {

  /**
    * Given the number of islands, produces an explanation of which should be connected to which.
    *
    * @param numIslands The number of islands to configure.
    * @return The connections that should be created.
    */
  def configure(numIslands: Int): Seq[Connection]
}

/** Represents a connection from `from` to `to`. */
case class Connection(from: Int, to: Int)

case class RingNetworkTopology() extends NetworkTopology {
  override def configure(numIslands: Int): Seq[Connection] = {
    if (numIslands == 1) {
      Seq()
    } else {
      (Vector.range[Int](0, numIslands) ++ Seq(0))
        .sliding(2)
        .flatMap {
          case Vector(from, to) => Seq(Connection(from, to), Connection(to, from))
        }
        .toVector
    }
  }
}

case class FullyConnectedNetworkTopology() extends NetworkTopology {
  override def configure(numIslands: Int): Seq[Connection] = {
    Vector.tabulate(numIslands, numIslands)(Connection).flatten.filterNot(c => c.from == c.to)
  }
}

case class StarNetworkTopology() extends NetworkTopology {
  override def configure(numIslands: Int): Seq[Connection] = {
    // 0 is the center point, connect everything else
    Vector
      .range(1, numIslands)
      .flatMap(outer => Seq(Connection(0, outer), Connection(outer, 0)))
  }
}
