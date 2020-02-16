# Quickstart
This quickstart guide will walk through using Evvo to solve a modified version of the Traveling Salesperson Problem, with two objectives. It aims to explain how to use evolutionary computing to solve multi-objective problems, and then how those concepts translate into Evvo.

### The Bi-objective Traveling Salesman Problem
The [Traveling Salesperson Problem](https://en.wikipedia.org/wiki/Travelling_salesperson_problem) (TSP) is a problem in graph theory. You have a set of cities (nodes in the graph), each connected to each other city with a weighted edge. The goal is to find a _"tour"_, a path that goes from one city through all the other cities and then back to the origin, and to minimize the sum of the weights along that tour. 

The bi-objective traveling salesperson problem (TSP2) is a modified version of the traveling salesperson problem. The only difference is in the edges - instead of having only one weight, each edge has two different weights. Assume these are two things that can't be direcly combined into one weight (say, by adding them together). One example might be travel cost and travel enjoyability, say, the cost for a train ticket and a rating from 0-100 of how nice the train is.

There exist [very good approximation algorithms](http://www.math.uwaterloo.ca/tsp/world/) for TSP, so evolutionary computing will not provide any benefit there. However, TSP2 doesn't have well-known approximation algorithms, so evolutionary computing may be able to complete with or outperform other methods.

### Problem Type
The first step in using evolutionary computing for a problem is defining the type of data that you will use to represent the problem. Here, we know we have a graph theory problem with a fully-connected weighted graph: sounds like we can use an [adjacency matrix](https://en.wikipedia.org/wiki/Adjacency_matrix). Because we have two objectives, it'll actually be easier to use two "cost" matrices, one for each objective. So, how do we represent this in Scala?

First, we need a way to store a single cost matrix. This should do:
```scala
case class CostMatrix(costs: IndexedSeq[IndexedSeq[Double]])
```
I'll choose a case class with a matrix inside, as we will likely want to add some utility functions to `CostMatrix` later. For example, we'll almost certainly want to index into it, to extract the cost of travel between two cities. Without any utility functions, that looks like:

```scala
val costMatrix = CostMatrix(IndexedSeq(IndexedSeq(0, 2), IndexedSeq(2, 0)))
costMatrix.costs(from)(to)
```

That's no good. Let's update CostMatrix to have an apply method that extracts the cost:
```scala
case class CostMatrix(costs: IndexedSeq[IndexedSeq[Double]]) {
  def apply(from: Int, to: Int): Double = this.matrix(from)(to)
}
```
Now we can simply call the cost matrix:
```scala
costMatrix(from, to)
```

Much better. Now, we need a way to combine two `CostMatrix` instances into one `TSP2` problem. Another case class will do the trick:
```
case class TSP(cost1: CostMatrix, cost2: CostMatrix)
```

And that's it! This represents a single TSP2 _problem_, that is, the number of cities and costs to get between cities that make one problem different from another. However, we stil need a way to represent _solutions_ to the problem.

### Solution Type
We need to store the tour, defined above as the order in which you traverse the cities. To store an ordering over cities, my first intuition is simply a list of the cities you will visit. These indices will be relative to the cost matrices defined above - a value `2` in this list will reference the city at index `2` in `cost1` and `cost2`. An `IndexedSeq[Int]` can represent this.

```scala
package object tsp {
  type Tour = IndexedSeq[Int]
}
```
Note that this could just as easily be wrapped in a case class, but as you'll see below, it is more ergonomic to deal with an `IndexedSeq` directly.

### Objectives 
As this is the _bi-objective_ TSP problem, there are two objectives. Minimize() `cost1`, and minimize `cost2`. Sounds pretty simple. "Minimizing `cost1`" means minimizing the sum of the costs of getting from each city to the next in a given `Tour`. So, we should be able to loop through the tour, adding the cost at each city. 

In other words, it'll look something like this:
```scala
// `cost` is defined somewhere up here as the relevant CostMatrix
def sumOfCosts(sol: Tour): Double = {
  sol.sliding(2).map { case Vector(u, v) => cost(u, v) }.sum
}
```
You could also do this with a `.foldLeft` or `.foldRight` over the sliding pairs, but let's not get ahead of ourselves. There's an issue with this function, though – it's incorrect. This gives us the cost of getting from city `1` to city `2`, `2` to `3`, etc, up to `n-1` to `n`. But, this is a tour, so it must be a round-trip: we have to add the cost of getting from city `n` back to city `1`. Here's a corrected version:

```scala
def sumOfCosts(sol: Tour): Double = {
  sol.sliding(2).map { case Vector(u, v) => cost(u, v) }.sum + cost(sol.last, sol.head)
}
```

This highlights the importance of writing unit tests for your objectives. If you are optimizing solutions according to an objective that doesn't actually capture what you want, the solutions that seem optimal will give you bad results in the real world! Avoiding that is worth the cost of writing unit tests.

The free-floating function defined above won't work with Evvo. For that, we'll have to create our own class that extends `Objective`.

```scala
case class CostObjective(override val name: String, cost: CostMatrix)
  extends Objective[Tour](name, Minimize()) {
  
  override protected def objective(sol: Tour): Double = {
    sol.sliding(2).map { case Vector(u, v) => cost(u, v) }.sum + cost(sol.last, sol.head)
  }
}
```

There are a few important things to note here. First: this class still accepts a name and a CostMatrix. This allows the same case class to be used for each of the two objectives. They only need to differ in their data, not their behavior. And notice that `Objective` is type-bounded by the type of our solution: in this case, `Tour`. When extending `Objective`, we pass in the name (used for logging), but we also pass in `Minimize()`. This signals to Evvo that the goal is to minimize the output of the `objective(Tour)`  method.

Now, we'll have to read our TSP2 data from somewhere, and put it into two instances of the `CostObjective` class, but that can wait. First, let's take a look at what agents we need to write.

### Agents
Recall from the [README](./README.md) that agents generate new solutions, modify existing solutions, and delete bad solutions. These three operations, together, move the population of solutions in the direction specified by the objectives. 

#### Creator Agents
Creator agents create solutions. For economy of presentation, we will only describe one creator here.

The simplest way to create solutions here is a completely random tour. This function is simple to define:
```
def create(): Iterable[Tour] = {
  Vector.fill(10)(util.Random.shuffle(Vector.range(0, numCities)))
}
```

This is obviously not very effective, because it creates solutions that are very far from the optimal ones. A better idea for a creator would be a "greedy" creator, that starts at a random point, and at each step, picks the closest city not yet visited. 

As with the objective, a standalone function can't be used directly with Evvo. In this case, we have to extend `CreatorFunction`.

```scala
case class RandomTourCreator(numCities: Int, numSolutions: Int = 10)
  extends CreatorFunction[Tour]("RandomTourCreator") {
  override def create(): Iterable[Tour] = {
    Vector.fill(numSolutions)(util.Random.shuffle(Vector.range(0, numCities)))
  }
}
```

And we'll make the number of solutions a parameter with a default. Evvo strives to always provide sane (but overridable) defaults, and we'll extend that habit to this example.

#### Modifier Agents
The simplest modifier will just take two cities and swap their positions. Not very clever, and it'll take a while to get to the correct solution, but it will enact change in the population, and that's all we care about for now. While you could extend `ModifierFunction`, which would look like this:
```scala
case class SwapTwoCitiesModifier2() extends ModifierFunction[Tour]("SwapTwoCities") {
  override def modify(sols: IndexedSeq[Scored[Tour]]): Iterable[Tour] = {
    sols.map(_.solution).map(mutate)
  }

  private def mutate(sol: Tour): Tour = {
    val index1 = util.Random.nextInt(sol.length)
    val index2 = util.Random.nextInt(sol.length)
    sol.updated(index1, sol(index2)).updated(index2, sol(index1))
  }
}
```
It is very common to want your agents to simply perform a one-to-one mapping on the input solutions. If you extend `ModifierFunction`, you will have to implement this yourself - see how `modify` extracts the solution value from the scored solutions, and then does the mapping. Because this is to common, we provide `MutatorFunction`. Extending `MutatorFunction` here saves a few lines of code, but more importantly, it makes clear to people reading your code that all that's happening is a one-to-one mapping. 


```scala
case class SwapTwoCitiesModifier() extends MutatorFunction[Tour]("SwapTwoCities") {
  override protected def mutate(sol: Tour): Tour = {
    val index1 = util.Random.nextInt(sol.length)
    val index2 = util.Random.nextInt(sol.length)

    sol.updated(index1, sol(index2)).updated(index2, sol(index1))
  }
}
```

Another modifier performs crossover. This takes two tours and produces one, where the result has the first part of the first tour, and the rest of the cities in the same order as they appear in the second tour. In the same way that we were able to use `MutatorFunction` above to avoid code duplication and increase clarity, here we can use `CrossoverFunction`.

```scala
case class CrossoverModifier() extends CrossoverFunction[Tour](name="Crossover") {
  override protected def crossover(sol1: Tour, sol2: Tour): Tour = {
    val index = util.Random.nextInt(sol1.length) // The index of crossover
    val firstHalf = sol1.take(index) // Take the first `index` values from sol1
    // And we need the rest of the cities, so take them from sol2, in the order of sol2
    firstHalf ++ sol2.filterNot(firstHalf.contains) 
  }
} 
```

#### Deletor Agents
Deletor agents will remove bad solutions from the population, increasing the average fitness. Because what determines whether a solution is "bad" is entirely based on its scores on the various objectives, there are deletors that work for all problems. This is quite different from creators and mutators, where you need a new set for every problem type you want to solve. Evvo provides a deletor called `DeleteDominated`. It takes a sample from the population, and deletes all the solutions in the sample that are dominated.

Let's use that. We don't have to write any additional code for our deletor.

### Defining an Island
#### Taking Inventory
So far, we have defined:
* A problem type, `TSP2`
* A solution type, `Tour`
* A way to create objectives: `CostObjective`
* A creator: `RandomTourCreator`
* Two modifiers `SwapTwoCitiesModifier` and `CrossoverModifier`

And we've learned about `DeleteDominated`, a `DeletorFunction` that's already defined.

#### IslandBuilder
To run Evvo, you will need an IslandBuilder. An IslandBuilder follows the [builder pattern](https://en.wikipedia.org/wiki/Builder_pattern#Scala) (well, actually the [type-safe builder pattern](http://blog.rafaelferreira.net/2008/07/type-safe-builder-pattern-in-scala.html), but that's an implementation detail). It provides a fluent interface for constructing `Island`s.  You can use it to construct islands to run locally, through a `LocalIslandManager`, or remotely, with a `RemoteIslandManager`. 

```
val tsp2: TSP2 = … // Assume we have read the specifics of this problem into our TSP2 instance.
val islandBuilder = EvvoIsland.builder()
      .addObjective(CostObjective("CostA", tsp2.cost1))
      .addObjective(CostObjective("CostB", tsp2.cost2))
      .addCreator(RandomTourCreator(tsp2.cost1.length))
      .addCreator(RandomTourCreator())
      .addModifier(SwapTwoCitiesModifier())
      .addDeletor(DeleteDominated())
```
Now that we have our `IslandBuilder`, let's build some islands and try to optimize this problem.


### IslandManagers
`IslandManager` is the API for constructing and running islands. There are currently two implementations - a local one and a remote one. Once you have constructed the IslandBuilder, you simply pass it and the number of islands to create to an IslandManager, and then tell it to run.

#### Running Locally
Let's create a `LocalIslandManager` and use it to run one island for one second.

```scala
val islandManager = new LocalIslandManager(numIslands=1, islandBuilder)
islandManager.runBlocking(StopAfter(1.second))
```

Well, that was simple. Of course, we haven't actually looked at the solutions, so let's do that:

```scala
println(islandManager.currentParetoFrontier())
```

This will print the scores of each solution on the pareto frontier. Solutions aren't printed by default, because they are often very big (in a TSP instance with 10,000 cities, for example, each tour would be a vector of length 10,000). To see the solutions , simply run:
```scala
println(islandManager.currentParetoFrontier().solutions)
```

#### Parallelizing Locally
The above example only ran one island. Not to say that it wasn't running in parallel – each agent runs on its own thread. What if you want to run more than one at a time?
```scala
val islandManager = new LocalIslandManager(numIslands=1000, islandBuilder)
```
Okay, 1000 is probably excessive. But the point stands: you only have to change one number, and the degree of parallelism changes with it.

#### Emigration/Immigration Strategies
If you have multiple islands each working on their own population, it won't be much faster than just one island running – it's essentially equivalent to running one island `n` times, and combining the results. On the other hand, if islands exchange solutions, it is possible to get much more scalability out of parallelism. To change how islands exchange solutions, you will need to provide an `EmigrationStrategy` or `ImmigrationStrategy`. By default, each island selects a random sample from their population, and sends it to another island at random, and all islands accept all incoming solutions. To change it, add a new strategy to the builder before building:

```
val islandBuilder = EvvoIsland.builder()
  … // add objectives, creators, etc
  .withImmigrationStrategy(…)
  .withEmigrationStrategy(…)
  
``` 

### Distributing over GCP servers.
Network parallelism is also possible, with the bundled Docker container running a backend server,
and a client run from anywhere sending islands to that server. However, explaining all the setup 
required for that would take too much time for a `QUICKSTART`, so we'll hold off.


### Recap
We went over how to convert a problem into objectives, choose a datatype to represent that problem, and choose a datatype to represent its solutions. Then, we created agents for producing and modifying solutions to that problem. This should be enough of an overview to get started using Evvo.
