# Contributing to Evvo


-------------------------------------------------------------------------------
### Architecture
#### Akka

#### Islands

##### Constructing an Island
We use Akka for network-parallelism between [islands](./README.md#terminology). Islands are created with an `IslandBuilder`, a class that specifies what happens in an island – mutators, deletors, and so on. An instance of this class is passed to a `IslandManager`, which 

-------------------------------------------------------------------------------
### Architecture
