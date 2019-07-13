package io.evvo.agent.defaults

import io.evvo.agent.defaults.trees._
import org.scalatest.{Matchers, WordSpec}

class TreesTest extends WordSpec with Matchers {
  "Leaves" should {
    val leaf: BinaryTree[String, Int] = BTLeaf(3)
    val node: BinaryTree[String, Int] = BTNode("hi", BTLeaf(3), BTLeaf(4))

    "have 1 leaf" in {
      leaf.numLeaves shouldBe 1
    }

    "always return an empty path when searching for a leaf" in {
      leaf.pathToRandomSubtree() should be(empty)
    }


    "always return an empty path when searching for a subtree" in {
      leaf.pathToRandomSubtree() should be(empty)
    }

    "allow getting a subtree with an empty path" in {
      leaf.getSubtree(Seq()) should be(Some(leaf))
    }

    "not allow getting a subtree with a non-empty path" in {
      leaf.getSubtree(Seq(GoRight)) should be(None)
    }

    "allow setting a subtree with an empty path" in {
      leaf.setSubtree(Seq(), node) shouldBe Some(node)
    }

    "not allow setting subtrees at non-empty paths" in {
      leaf.setSubtree(Seq(GoLeft), node) shouldBe None
    }
  }

  "Nodes" should {
    val node34: BTNode[String, Int] = BTNode("hi", BTLeaf(3), BTLeaf(4))
    val node3434: BTNode[String, Int] = BTNode("hi2", node34, node34)
    val leftSkewedNode: BTNode[String, Int] =
      BTNode("",
        BTNode("",
          BTNode("",
            BTNode("",
              BTLeaf(0), BTLeaf(0)),
            BTLeaf(0)),
          BTLeaf(0)),
        BTLeaf(0))

    "have the correct number of leaves" in {
      node34.numLeaves shouldBe 2
      node3434.numLeaves shouldBe 4
      leftSkewedNode.numLeaves shouldBe 5
    }


    "return a possible path when searching for a leaf" in {
      node34.getSubtree(node34.pathToRandomLeaf()) should not be None
      leftSkewedNode.getSubtree(leftSkewedNode.pathToRandomLeaf()).get shouldBe BTLeaf(0)
    }

    "return each leaf with equal probability" in {
      val paths = Vector.fill(1000)(leftSkewedNode.pathToRandomLeaf())
      // There are five leaves, so expect 1000 / 5 = 200, use 150 for confidence interval
      assert(paths.distinct.length == 5)
      assert(paths.groupBy(x => x).view.mapValues(_.length).values.forall(_ > 150))
    }

    "return each subtree with equal probability" in {
      val paths = Vector.fill(1000)(leftSkewedNode.pathToRandomSubtree())
      // There are nine subtrees, so expect 1000 / 9
      assert(paths.distinct.length == 9)
      assert(paths.groupBy(x => x).view.mapValues(_.length).values.forall(_ > 80))
    }


    "allow getting a subtree with an empty path" in {
      node34.getSubtree(Seq()).get shouldBe node34
      node3434.getSubtree(Seq()).get shouldBe node3434
    }

    "allow getting a subtree with a non-empty path" in {
      node34.getSubtree(Seq(GoLeft)) shouldBe Some(BTLeaf(3))
      node34.getSubtree(Seq(GoRight)) shouldBe Some(BTLeaf(4))
      node3434.getSubtree(Seq(GoRight)) shouldBe Some(node34)
      leftSkewedNode.getSubtree(Seq.fill(4)(GoLeft)) shouldBe Some(BTLeaf(0))
    }

    "allow setting a subtree with an empty path" in {
      node34.setSubtree(Seq(), node3434) shouldBe Some(node3434)
    }

    "allow setting subtrees at non-empty paths" in {
      node34.setSubtree(Seq(GoLeft), node34) shouldBe Some(node34.copy(left=node34))
    }

  }
}
