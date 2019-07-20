package io.evvo.builtin

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

    /** @return the path to a random node in this tree. Each node is given equal weight. */
    def pathToRandomNode(): Option[TreePath]

    /** @return the path to a random subtree in this tree. Each subtree is given equal weight. */
    def pathToRandomSubtree(): TreePath

    /** @return the tree at the specified path, if there is one. */
    def getSubtree(path: TreePath): Option[BinaryTree[N, L]]

    /** Replaces the tree at the specified path with the given `newValue`, or returns None if
      * the path was invalid.
      */
    def setSubtree(path: TreePath, newValue: BinaryTree[N, L]): Option[BinaryTree[N, L]]

    /** Updates the subtree at the given path. */
    def updateSubtree(path: TreePath, update: BinaryTree[N, L] => BinaryTree[N, L])
    : Option[BinaryTree[N, L]]

    /** @return the leaves in this tree. */
    def leaves: Seq[BTLeaf[N, L]]

    /** @return the number of leaves in this tree. */
    def numLeaves: Int
  }

  case class BTNode[N, L](data: N, left: BinaryTree[N, L], right: BinaryTree[N, L])
    extends BinaryTree[N, L] {
    override val numLeaves: Int = left.numLeaves + right.numLeaves

    override def pathToRandomLeaf(): TreePath = {
      if (util.Random.nextInt(this.numLeaves) < left.numLeaves) {
        GoLeft +: left.pathToRandomLeaf()
      } else {
        GoRight +: right.pathToRandomLeaf()
      }
    }

    override def pathToRandomNode(): Option[TreePath] = {
      val path = this.pathToRandomSubtree()
      if (this.getSubtree(path).exists(_.isInstanceOf[BTNode[N, L]])) {
        Some(path)
      } else {
        pathToRandomNode()
      }
    }

    override def pathToRandomSubtree(): TreePath = {
      val leftSubtrees = left.numLeaves * 2 - 1 // nodes + leaves = leaves + (leaves - 1)
      val totalSubtrees = this.numLeaves * 2 - 1
      val random = util.Random.nextInt(totalSubtrees)
      if (random == 0) {
        // 1 chance to stop here
        Seq()
      } else if (random <= leftSubtrees) {
        // 1 chance to stop at
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
            case GoLeft => left.getSubtree(rest)
            case GoRight => right.getSubtree(rest)
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

    override def updateSubtree(path: TreePath, update: BinaryTree[N, L] => BinaryTree[N, L])
    : Option[BinaryTree[N, L]] = {
      // Could be done in one pass through the tree instead of two, but this is clearer, revisit
      // this is it is a performance bottleneck.
      this.getSubtree(path).flatMap(subtree => this.setSubtree(path, update(subtree)))
    }

    override def leaves: Seq[BTLeaf[N, L]] = this.left.leaves ++ this.right.leaves
  }


  case class BTLeaf[N, L](data: L) extends BinaryTree[N, L] {
    override def pathToRandomLeaf(): TreePath = Seq()

    override def pathToRandomSubtree(): TreePath = Seq()

    override def pathToRandomNode(): Option[TreePath] = None

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

    override def updateSubtree(path: TreePath, update: BinaryTree[N, L] => BinaryTree[N, L])
      : Option[BinaryTree[N, L]] = {
      path match {
        case Nil => Some(update(this))
        case _ => None
      }
    }

    override val numLeaves: Int = 1

    override def leaves: Seq[BTLeaf[N, L]] = Seq(this)

  }

  // Represents a path to index into a tree
  type TreePath = Seq[TreePathElement]
  sealed trait TreePathElement
  case object GoLeft extends TreePathElement
  case object GoRight extends TreePathElement


  // ===============================================================================================
  // AGENTS
  case class LeafCreator[N, L](leafValues: Seq[() => L])
    extends CreatorFunction[BinaryTree[N, L]]("LeafCreator") {
    override def create(): Iterable[BTLeaf[N, L]] =
      Vector.fill(16)(BTLeaf(leafValues(util.Random.nextInt(leafValues.length))()))
  }

  case class ChangeLeafDataModifier[N, L](modifier: (L => L))
    extends MutatorFunction[BinaryTree[N, L]]("ChangeLeafValue") {
    override protected def mutate(sol: BinaryTree[N, L]): BinaryTree[N, L] = {
      def update(subtree: BinaryTree[N, L]): BinaryTree[N, L] = {
        subtree match {
          case BTLeaf(data) => BTLeaf(modifier(data))
          case n: BTNode[N, L] => n
        }
      }

      sol.updateSubtree(sol.pathToRandomLeaf(), update).getOrElse(sol)
    }
  }

  case class ChangeNodeDataModifier[N, L](modifier: (N => N))
    extends MutatorFunction[BinaryTree[N, L]]("ChangeNodeData") {
    override protected def mutate(sol: BinaryTree[N, L]): BinaryTree[N, L] = {
      def update(subtree: BinaryTree[N, L]): BinaryTree[N, L] = {
        subtree match {
          case l: BTLeaf[N, L] => l
          case BTNode(data, left, right) => BTNode(modifier(data), left, right)
        }
      }

      sol.pathToRandomNode()
        .flatMap(p => sol.updateSubtree(p, update))
        .getOrElse(sol)
    }
  }

  case class ReplaceLeafWithNodeModifier[N, L](nodeGenerator: () => BTNode[N, L])
    extends MutatorFunction[BinaryTree[N, L]]("ReplaceLeafWithNode") {
    override protected def mutate(sol: BinaryTree[N, L]): BinaryTree[N, L] = {
      val path = sol.pathToRandomLeaf()
      sol.setSubtree(path, nodeGenerator()).getOrElse(sol)
    }
  }

  case class SwapSubtreeModifier[N, L]()
    extends CrossoverFunction[BinaryTree[N, L]]("SwapSubtree") {
    override protected def crossover(sol1: BinaryTree[N, L], sol2: BinaryTree[N, L])
    : BinaryTree[N, L] = {
      sol1.getSubtree(sol1.pathToRandomSubtree())
        .flatMap(tree => sol2.setSubtree(sol2.pathToRandomSubtree(), tree))
        .getOrElse(sol1)
    }
  }
}
