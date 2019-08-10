package io.evvo.island.population

import org.scalatest.{Matchers, WordSpec}

class NetworkTopologyTest extends WordSpec with Matchers {
  "RingNetworkTopology" should {
    "connect islands in a ring" in {
      RingNetworkTopology().configure(1).toSet shouldBe Set()
      RingNetworkTopology().configure(2).toSet shouldBe Set(Connection(0, 1), Connection(1, 0))
      RingNetworkTopology().configure(3).toSet shouldBe Set(
        Connection(0, 1),
        Connection(1, 0),
        Connection(1, 2),
        Connection(2, 1),
        Connection(2, 0),
        Connection(0, 2))

      RingNetworkTopology().configure(4).toSet shouldBe Set(
        Connection(0, 1),
        Connection(1, 0),
        Connection(1, 2),
        Connection(2, 1),
        Connection(2, 3),
        Connection(3, 2),
        Connection(0, 3),
        Connection(3, 0))
    }
  }

  "FullyConnectedNetworkTopology" should {
    "connect all islands" in {
      FullyConnectedNetworkTopology().configure(1).toSet shouldBe Set()
      FullyConnectedNetworkTopology().configure(2).toSet shouldBe Set(
        Connection(0, 1),
        Connection(1, 0))
      FullyConnectedNetworkTopology().configure(3).toSet shouldBe
        Set(
          Connection(0, 1),
          Connection(1, 0),
          Connection(0, 2),
          Connection(2, 0),
          Connection(1, 2),
          Connection(2, 1))
    }
  }

  "StarNetworkTopology" should {
    "connect all islands" in {
      StarNetworkTopology().configure(1).toSet shouldBe Set()
      StarNetworkTopology().configure(2).toSet shouldBe Set(Connection(0, 1), Connection(1, 0))
      StarNetworkTopology().configure(3).toSet shouldBe
        Set(Connection(0, 1), Connection(1, 0), Connection(0, 2), Connection(2, 0))

      StarNetworkTopology().configure(4).toSet shouldBe
        Set(
          Connection(0, 1),
          Connection(1, 0),
          Connection(0, 2),
          Connection(2, 0),
          Connection(0, 3),
          Connection(3, 0))

    }
  }
}
