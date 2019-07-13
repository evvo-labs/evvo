package io.evvo.agent.defaults

import io.evvo.agent.{CreatorFunction, CrossoverFunction, MutatorFunction}

object trees {
  /** Represents a tree. Generic over the type of data in its nodes and its leaves.
    *
    * @tparam N The type of data in each node.
    * @tparam L The type of data in each leaf.
    */
  sealed trait BinaryTree[N, L] {
    /** @return the path to a random leaf in this tree. Each leaf is given equal weight. */
    def pathToRandomLeaf(): TreePath

    /** @return the path to a random subtree in this tree. Each subtree is given equal weight. */
    def pathToRandomSubtree(): TreePath

    /** @return the tree at the specified path, if there is one. */
    def getSubtree(path: TreePath): Option[BinaryTree[N, L]]

    /** Replaces the tree at the specified path with the given `newValue`, or returns None if
      * the path was invalid.
      */
    def setSubtree(path: TreePath, newValue: BinaryTree[N, L]): Option[BinaryTree[N, L]]

    /** @return the number of leaves in this tree. */
    def numLeaves: Int
  }

  case class BTNode[N, L](data: N, left: BinaryTree[N, L], right: BinaryTree[N, L])
    extends BinaryTree[N, L] {
    override val numLeaves: Int = left.numLeaves + right.numLeaves

    override def pathToRandomLeaf(): TreePath = {
      val numLeaves = left.numLeaves + right.numLeaves
      if (util.Random.nextInt(numLeaves) < left.numLeaves) {
        GoLeft +: left.pathToRandomLeaf()
      } else {
        GoRight +: right.pathToRandomLeaf()
      }
    }

    override def pathToRandomSubtree(): TreePath = {
      val leftSubtrees = left.numLeaves * 2 - 1 // nodes + leaves = leaves + (leaves - 1)
      val rightSubtrees = right.numLeaves * 2 - 1
      if (util.Random.nextInt(leftSubtrees + rightSubtrees) < leftSubtrees) {
        GoLeft +: left.pathToRandomSubtree()
      } else {
        GoRight +: right.pathToRandomSubtree()
      }
    }


    override def getSubtree(path: TreePath): Option[BinaryTree[N, L]] = {
      path match {
        case Nil => Some(this)
        case step :: rest => {
          step match {
            case GoLeft => this.left.getSubtree(rest)
            case GoRight => this.right.getSubtree(rest)
          }
        }
      }
    }

    override def setSubtree(path: TreePath, newValue: BinaryTree[N, L])
    : Option[BinaryTree[N, L]] = {
      path match {
        case Nil => Some(newValue)
        case next :: pathRest => next match {
          case GoLeft => left.setSubtree(pathRest, newValue).map(t => BTNode(data, t, right))
          case GoRight => right.setSubtree(pathRest, newValue).map(t => BTNode(data, left, t))
        }
      }
    }
  }


  case class BTLeaf[N, L](data: L) extends BinaryTree[N, L] {
    override def pathToRandomLeaf(): TreePath = Seq()

    override def pathToRandomSubtree(): TreePath = Seq()

    override def getSubtree(path: TreePath): Option[BinaryTree[N, L]] = {
      path match {
        case Nil => Some(this)
        case _ => None
      }
    }

    override def setSubtree(path: TreePath, newValue: BinaryTree[N, L])
    : Option[BinaryTree[N, L]] = {
      path match {
        case Nil => Some(newValue)
        case _ => None
      }
    }

    override val numLeaves: Int = 1
  }

  // Represents a path to index into a tree
  type TreePath = Seq[TreePathElement]
  sealed trait TreePathElement
  case object GoLeft extends TreePathElement
  case object GoRight extends TreePathElement
}
