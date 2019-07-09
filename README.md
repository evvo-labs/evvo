# Evvo ![travis master status](https://travis-ci.org/evvo-labs/evvo.svg?branch=master) 

Evvo is the Scala framework for distributed [multi-objective](https://en.wikipedia.org/wiki/Multi-objective_optimization) [evolutionary computing](https://en.wikipedia.org/wiki/Evolutionary_computation).

Evvo supports:
 * Distributed evolutionary computing on heterogenous networks
 * User-defined problem and solution types
 * Sane, but overridable, defaults, almost everywhere
 * Built-in problem types for common solution type representations, e.x. bitstrings
 * (planned) Real-time visualization of the evolutionary process
 * (planned) The ability for users to dynamically add solutions and contraints to the population

####NOTE: This code is provided as a public beta
Evvo is in beta. The API is not stable, and is guaranteed to change in the future. This is made clear by the version being `v0.*.*`. Once the major version is `1`, the API will be stable. Do not use this code in production. 

-------------------------------------------------------------------------------

Here's an example showing how simple it is to set up and solve a basic problem (on one machine) using Evvo. Let's say we want to maximize the number of ones in a `Bitstring`. To represent this objective, we'll need to write an `Objective` (who would have guessed?). Then we can use Evvo to search for `Bitstring`s that satisfy the `Objective`. After running for a second, we'll print the current [pareto frontier](https://en.wikipedia.org/wiki/Pareto_efficiency#Use_in_engineering). 
```scala
import io.evvo.agent.defaults.{Bitstring, Bitflipper, BitstringGenerator, DeleteDominated}
import io.evvo.island.{EvvoIsland, LocalIslandManager, StopAfter}
import io.evvo.island.population.{Maximize, Objective}
import scala.concurrent.duration._

object Maximize1Bits extends Objective[Bitstring]("1Bits", Maximize) {
  override protected def objective(sol: Bitstring): Double = {
    sol.count(identity) // Bitstrings are represented as Seq[Boolean], count `true`s
  }
}


val islandBuilder = EvvoIsland.builder[Bitstring]()
  .addCreator(BitstringGenerator(length=16))
  .addModifier(Bitflipper())
  .addDeletor(DeleteDominated[Bitstring]())
  .addObjective(Maximize1Bits)

val islandManager = new LocalIslandManager(numIslands = 1, islandBuilder)
islandManager.runBlocking(StopAfter(1.second))

print(islandManager.currentParetoFrontier())
```

This will run three asynchronous agents locally. `BitstringGenerator` creates `Bitstring`s of length 16 with randomly initialized bits, `Bitflipper` flips a random bit, and `DeleteDominated` will pick `Bitstring`s out of the generated solutions and delete the [dominated](https://en.wikipedia.org/wiki/Pareto_efficiency#Use_in_engineering_and_economics) ones. If you run this code on anything more powerful than a toaster, here's what will be printed:
```
ParetoFrontier(Map(1Bits -> 16.0))
```

This means that there was one solution on the pareto frontier, which scored `16.0` according to the objective named `"1Bits"`. Since the generated `Bitstring`s only have 16 bits, the best possible score is `16.0` bits that are 1. Note that the pareto frontier doesn't print the actual solutions. The solutions are available within the `ParetoFrontier` class, but the `toString` method prints only the scores, because solutions to more complex problems are very large.

If our [built-in](./src/main/scala/com/io/agent/defaults/defaults.scala) _Creators_, _Modifiers_, and _Deletors_ do not work for your problem, you can define your own as easily as we defined `Maximize1Bits`.

If you want to jump directly into an example, check out the [quickstart guide](./QUICKSTART.md). It assumes some familiarity with evolutionary computing concepts, so you may need to cross reference the terminology and diagrams in this file while you are working through the example.

-------------------------------------------------------------------------------
### Terminology

**Island**: a _population_ with multiple _agents_ which can communicate with other islands for _immigration_ and _emigration_.

**Population**: a mutable set of scored solutions from which the _Pareto Frontier_ is calculated.

**Pareto Frontier**: a set of [non-dominated solutions](https://en.wikipedia.org/wiki/Pareto_efficiency#Pareto_frontier).

**Dominated Solution**: a dominated solution is worse in every regard (as captured by the _objectives_) than another solution, or worse in one regard and equal on others. For example, if there are three objective functions, each of which is to be maximized, then a solution scoring `(3, 9, 4)` would be dominated by one scoring `(5, 11, 4)`, but not one scoring `(5, 11, 3)`.

**Objective**: a single goal in the broader optimization problem.

**Fitness**: how strong a solution is as determined by the _objectives_.

**Agent**: _not_ an Akka actor, but a part of an [asynchronous evolutionary system](#asynchronous-evolutionary-computing). Each one runs on a separate thread.

**Creator Agent**: often shortened to "Creator", a Creator Agent generates a set of solutions and adds those solutions to the _population_. 

**Modifier Agent**: often shortened to "Modifier", a Modifier Agent retrieves some number of solutions from the _population_, calls a function on that set of solutions to produce new solutions based on the input, and adds those new solutions to the _population_.

**Mutator Agent**: A type of _ModifierAgent_ that applies a one-to-one mapping over a set of solutions, and adds all the results.

**Crossover Agent**: A type of _ModifierAgent_ that applies a two-to-one function over a set of solutions, taking part of each input solution and combining them to produce new solutions.

**Deletor Agent**: often shortened to "Deletor", a Deletor Agent retrieves some number of solutions from the _population_ and deletes the bad ones (for whatever definition of bad it's working with) from the _population_.

**Emigration**: an _island_ sending solutions to another _island_.

**Immigration**: an _island_ receiving solutions from another _island_.

-------------------------------------------------------------------------------
### The Model
#### Asycnchronous Evolutionary Computing
As described in [John Rachlin's paper on paper mill optimization](https://www.researchgate.net/profile/Richard_Goodwin2/publication/245797473_Cooperative_Multiobjective_Decision_Support_for_the_Paper_Industry/links/0046352ca1becd5890000000.pdf), asynchronous multi-agent evolutionary computing systems consist of a common population and multiple "evolutionary agents" that operate on the population. These agents, working in parallel, gradually push the overall fitness of a population upwards. (Assuming that the modifiers have a chance of improving fitness, and the deletors remove solutions that tend to be worse than average.) This system is easily parallelizable, as there is only one piece of shared memory - the set of solutions currently in the population. Much of the work (the work of computing new solutions, mutating existing solutions, and deciding which solutions to delete) can be distributed across multiple CPU cores, or even multiple machines.

This diagram illustrates each of the major components in Evvo and their roles:
```
+-------------------------------------------------------------------------+
|                                                                         |
| EvvoIsland                                                              |
|                                                                         |
+-------------------------------------------------------------------------+
|                                                                         |
|                                                   +------------------+  |
|  +---------------+       Generates new solutions  |                  |  |
|  |               |<-------------------------------| Creator Agent(s) |  |
|  | Population    |                                |                  |  |
|  |               |                                +------------------+  |
|  +---------------+                                                      |
|  |               | Reads some solutions           +------------------+  |<--+
|  |  - Objectives |------------------------------->|                  |  |   |
|  |               |                                | Modifier Agent(s)|  |   |
|  |  - Solutions  |<-------------------------------|                  |  |   | Immigration +
|  |               |         Derives new solutions  +------------------+  |   | Emigration
|  |               |                                                      |   |  Peer-to-peer gossip
|  |               | Reads some solutions           +------------------+  |   |  protocol for
|  |               |------------------------------->|                  |  |   |  sharing solutions
|  |               |                                | Deletor Agent(s) |  |   |  increases
|  |               |<-------------------------------|                  |  |   |  scalability without
|  |               |        Chooses some to delete  +------------------+  |   |  converging to local
|  +---------------+                                                      |   |  optima.
|                                                                         |   |
+-------------------------------------------------------------------------+   |  This is how Evvo
                                   ^                                          |  takes advantage of
                                   |  Immigration / Emigration                |  parallelism.
                                   v                                          |
+--------------------------------------------------------------------------+  |
|                                                                          |  |
| EvvoIsland     [Same contents as above, abbreviated for clarity]         |  |
|                                                                          |  |
+--------------------------------------------------------------------------+  |
                                   ^                                          |
                                   |  Immigration / Emigration                |
                                   v                                          |
+--------------------------------------------------------------------------+  |
|                                                                          |  |
| EvvoIsland     [Same contents as above, abbreviated for clarity]         |<-+
|                                                                          |
+--------------------------------------------------------------------------+

                                    .
                                    .
                                    .

                                and so on
```


### Distributing islands on a network
This diagram shows a simplified version of our network model:
```
                                                  +------------------------------------------------+
                                                  |                                                |
                                                  | Server                                         |
                                                  |                                                |
                                                  | +--------------------------------------------+ |
                                                  | |                                            | |
                                           +----->| | JVM running ActorSystem with remoting      | |
                                           |      | |                                            | |
                                           |      | |  +----------+  +----------+  +----------+  | |
+---------------------------------+        |      | |  |          |  |          |  |          |  | |
|                                 |        |      | |  | Island 1 |  | Island 4 |  | Island 7 |  | |
| IslandManager (client)          |        |      | |  |          |  |          |  |          |  | |
|                                 |        |      | |  +----------+  +----------+  +----------+  | |
+---------------------------------+        |      | |                                            | |
|                                 |        |      | +--------------------------------------------+ |
|  - Round robin deploys Islands  |<-------+      |                                                |
|     to remote ActorSystems      |        |      +------------------------------------------------+
|                                 |        |             ^
|  - Combines results at end      |        |             |  Some gossip happens over network
|     of optimization             |        |             |  Some gossip happens within a server
|                                 |        |             v
+---------------------------------+        |      +------------------------------------------------+
                                           |      |                                                |
                                           |      | Server                                         |
                                           |      |                                                |
                                           |      | +--------------------------------------------+ |
                                           |      | |                                            | |
                                           +----->| | JVM running ActorSystem with remoting      | |
                                           |      | |                                            | |
                                           |      | |  +----------+  +----------+  +----------+  | |
                                           |      | |  |          |  |          |  |          |  | |
                                           |      | |  | Island 2 |  | Island 5 |  | Island 8 |  | |
                                           |      | |  |          |  |          |  |          |  | |
                                           |      | |  +----------+  +----------+  +----------+  | |
                                           |      | |                                            | |
                                           |      | +--------------------------------------------+ |
                                           |      |                                                |
                                           |      +------------------------------------------------+
                                           |             ^
                                           |             |
                                           |             v
                                           |      +------------------------------------------------+
                                           |      |                                                |
                                           |      | Server                                         |
                                           |      |                                                |
                                           |      | +--------------------------------------------+ |
                                           |      | |                                            | |
                                           +----->| | JVM running ActorSystem with remoting      | |
                                                  | |                                            | |
                                                  | |  +----------+  +----------+                | |
                                                  | |  |          |  |          |                | |
                                                  | |  | Island 3 |  | Island 6 |                | |
                                                  | |  |          |  |          |                | |
                                                  | |  +----------+  +----------+                | |
                                                  | |                                            | |
                                                  | +--------------------------------------------+ |
                                                  |                                                |
                                                  +------------------------------------------------+

                                                                          .
                                                                          .
                                                                          .

                                                     and so on, until your server budget runs out
```

-------------------------------------------------------------------------------
### Quickstart
The [quickstart guide](./QUICKSTART.md) will walk you through writing and running a problem using Evvo. It solves a variant on the traveling salesperson problem with two objectives. If you're interested in seeing more of the developer API or understanding how to use Evvo, check this out next. 

-------------------------------------------------------------------------------
### Running a Custom Optimization Problem
This is going to change in the future, eventually you'll be able to include Evvo as a [Maven or sbt](#downloads) dependency. If you want to use Evvo before then, all of your code has to be part of the same src directory as Evvo (this is in order to ensure that remote islands can be [deserialized](#serializability)).

You'll want to check out our getting started (coming soon) for an example of how to use Evvo locally and remotely.

#### Keeping your Code Private
While we would love some well groomed examples in our repo, if you want to keep your code private, either put your new code in the [`src/main/scala/io/evvo/ignored`](src/main/scala/com/io/ignored) directory **or** locally ignore your new package/files with `git update-index --assume-unchanged [<file>...]`.

##### Best VCS Practices
For now, you should create a git repo inside the [`src/main/scala/io/evvo/ignored`](src/main/scala/io/evvo/ignored) directory and use that to version your code.

###### Using Submodules (Advanced)
Until Evvo is a Maven or sbt dependency, things may look a little tricky. If you need to freeze Evvo's version with the version of your optimization problem repository (`<YOUR_PROJECT>`), you could use 2 [`git submodules`](https://git-scm.com/book/en/v2/Git-Tools-Submodules): one for Evvo and one for your project. If you don't need to, please skip this section. Unfortunately, git doesn't support non-recursive nested submodules, so development would be difficult to do using the repo with the submodules.

Here's how you can set it up:

```bash
mkdir my_new_project
cd my_new_project
git init
echo "# Uh... it's... it's a dinosaur" > README.md

git submodule add git@github.com:evvo-labs/evvo.git
git submodule add <YOUR_PROJECT>
git submodule init

git add -A
git commit -m "Life finds a way"
```

Every time you want to freeze a version of your code with a version of Evvo, just checkout the Evvo and optimization problem commits that you wish to pair:

```bash
cd evvo
git checkout <STABLE_COMMIT>
cd ../<YOUR_PROJECT_DIR>
git checkout <STABLE_COMMIT>
cd ..

git add -A
git commit -m "Major theme parks have delays. When they opened Disneyland in 1956, nothing worked"
git push
```

Now, it is possible, but messy, to hard link the files to get around the lack of support for non-recursive nested submodules to make development possible (instead of just version pinning) using `my_new_project`. Note that it is not possible to hard link directories, so you would have to hard link every file indiviudally. I do not recommend this, but will leave the option open to you. For each file in `<YOUR_PROJECT>`, run:

```bash
ln <YOUR_PROJECT_DIR>/<YOUR_FILE_PATH> ./evvo/src/main/scala/io/evvo/ignored/<HARD_LINK_NAME>
```

Note that since this is in the ignored directory, everybody who clones your repo will not have the hard links set up, so you probably want to include a setup script that creates the hard links in `my_new_project`.

#### Setting up Servers
Evvo is [dockerized](https://www.docker.com/). Follow the [instructions](docker/README.md) to get started running your own network-parallel instance.

#### Serializability
Because we have to ship Islands to remote servers, Islands need to be [serializable](https://docs.oracle.com/javase/8/docs/api/java/io/Serializable.html). All of the code provided by Evvo is serializable, but the creators, modifiers, deletors, and objectives that you provide must also be serializable for the Islands to serialize and deserialize correctly. 

See [the relevant section](https://www.oreilly.com/library/view/scala-cookbook/9781449340292/ch12s08.html) of the Scala cookbook on serialization. Note that serialized classes should also have referential transparency, that is, they should not reference variables from an external scope. A class defined within an object (or another class, or a code block) that references variablees defined in the outer object (or class, or code block) may cause serialization issues if those values are not present in the deserialization context. In general, extending `Creator`, `Modifier`, or `DeletorFunction`, with case classes, and ensuring that those case classes take all the data they need as arguments will be sufficient to ensure that there are no serialization issues.

If you use a `LocalIslandManager` to create `LocalEvvoIsland`s, your data will still be serialized and deserialized, albeit on the same machine. This means that some of the most flagrant serialization exceptions can be caught early by testing with `LocalIslandManager`. 

-------------------------------------------------------------------------------
### Downloads
We plan to release Evvo so that it can be used with Maven or sbt. The project is not yet stable enough for that to make sense.

-------------------------------------------------------------------------------
### Contributing
See [`CONTRIBUTING.MD`](CONTRIBUTING.md) for information on how to contribute code to Evvo.
