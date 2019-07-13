package io.evvo.agent.defaults

import io.evvo.agent.defaults.trees.{BTLeaf, BTNode, BinaryTree, ChangeLeafDataModifier, ChangeNodeDataModifier, LeafCreator, SwapSubtreeModifier}
import io.evvo.island.population.Scored
import org.scalatest.{Matchers, WordSpec}

class TreesAgentsTest extends WordSpec with Matchers {

  val node:  BTNode[Boolean, Int] = BTNode(false,
    BTNode(true, BTLeaf(1), BTLeaf(2)),
    BTNode(true, BTLeaf(3), BTLeaf(4)))
  val scoredNode: Scored[BinaryTree[Boolean, Int]] = Scored(Map(), node)

  "Leaf Creator" should {
    "create trees with just leaves" in {
      val trees = LeafCreator[Int, Boolean](Seq(() => true, () => false)).create()
      trees should contain(BTLeaf[Int, Boolean](true))
      trees should contain(BTLeaf[Int, Boolean](false))
    }
  }

  "ChangeLeafDataModifier" should {
    "change the value of a leaf" in {
      val mod = ChangeLeafDataModifier[Boolean, Int](_ + 1).modify(IndexedSeq(scoredNode))
      // Test that the function that adds 1 was applied to some leaf
      mod.head.leaves.map(_.data).sum shouldBe node.leaves.map(_.data).sum + 1
    }

    "apply the function to a random leaf" in {
      val mod = ChangeLeafDataModifier[Boolean, Int](_ + 1).modify(IndexedSeq.fill(50)(scoredNode))
      // If it hits a different one each time, there are four unique trees that could result,
      // because there are four leaves.
      mod.toSet should have size 4
    }
  }

  "ChangeNodeDataModifier" should {
    "change the value in a random node" in {
      val modified = ChangeNodeDataModifier[Boolean, Int](!_)
        .modify(IndexedSeq.fill(50)(scoredNode))
      // If it changes the value in a random node, three possible nodes to flip
      modified.toSet should have size 3
    }
  }

  "SwapSubtreeModifier" should {
    "do something" in {
      val scoredNode2: Scored[BinaryTree[Boolean, Int]] = Scored(Map(),
        BTNode(false,
        BTNode(true, BTLeaf(5), BTLeaf(6)),
        BTNode(true, BTLeaf(7), BTLeaf(8))))

      val modified = SwapSubtreeModifier[Boolean, Int]()
        .modify(IndexedSeq.fill(1000)(Seq(scoredNode, scoredNode2)).flatten)
      // There are seven subtrees on the left, and seven on the right, so there are 49 possible
      // ways to put a subtree from one into the other.
      modified.toSet should have size 49
    }
  }
}
