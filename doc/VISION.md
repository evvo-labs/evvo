## Vision

The goal is to be the best evolutionary computing framework. This is defined along multiple objectives. This file aims to explain exactly what we mean by "the best", and details on how we want to achieve it.

#### Easy for developers to solve easy problems with 
If you have a multi-objective optimization problem, writing code using Evvo to solve it should be as fast or faster than writing a custom optimizer. This is especially hard in the local case, as end users wouldn't have to deal with network parallelism. We need a simple API for simple problems, so that getting started with Evvo is easy. We also want more complex APIs for tougher problems or for more complex algorithms for solving problems, but they shouldn't make using Evvo to solve the most basic problems any harder. We can evaluate this by looking at how many lines of code are required for a minimal example in Evvo vs other frameworks.

#### Easy to change from using a local one-node system to a cluster
Code that runs on one machine locally should run the same (but faster!) on a cluster. (Assuming the cluster is set up: that may be a challenge, but once it's running, minimal code changes should be required to change who does the processing.) We have already achieved this at least partially by having all optimizers implement the same trait, so only one line determines who does the computation. We can evaluate this by looking at how many lines of code have to change between examples for local and cluster-based solvers for the same problem. 

#### Fast
The goal of Evvo is to produce good solutions to multi-objective optimization problems. To do so, it will have to run concurrently on multiple machines, scaling near-linearly with the number of machines (and cores) available. Evvo should outperform other systems, at least other systems that have equivalent APIs. We can evaluate this by running common benchmarks for multi-objective optimizers, and comparison performance on real problems that other people have solved. 

#### Hide implementation complexity from end users
We use Akka internally to communicate between islands, and we use Docker to help deploy servers. End users may need to know these two facts, but we want to minimize the contact between users of Evvo and the underlying architecture. This will help us swap out architecture components later, and also ensure that the cognitive load of using Evvo is as small as possible. This is hard to evaluate explicitly, but it will be apparent if our docs mention Akka or Docker more than they ought to.

#### Extensibility through plugins
Many different people will want to solve the same types of problems, and allowing them to share code will help build a community, increase the utility of Evvo, and help us identify specific applications or optimization techniques to add to the core. Allowing plugins for solution types, agents, and everything else pluggable will allow Evvo to grow as a platform, beyond the work we do to make it a framework. We can evaluate this by watching how many people choose to contribute libraries and plugins, once that is possible. 