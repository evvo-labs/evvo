# Contributing to Evvo
First of all, thanks for looking at this page and considering contributing! We'd love to have you, and if you have any questions or this document is unclear, please let us know! Open an issue with your question, or email any contributor or admin at the emails found in our GitHub profiles.

----------------------------------------------------------------------------------------------------
### Git Workflow
We are using tagged releases on master to indicate milestones. There is no develop branch, so if you are used to git flow, things will be a little different. Every feature to be released starts on a branch named `feature/…`, is merged into `master` after a pull request, and then is considered "released" only once we have a tagged release after that commit.

#### Branch Names
* Branches that implement new features should be named `feature/<the-feature-name>`
* Branches that improve documentation should be named `doc/<what-docs-are-updated>`. Of course, feature branches will include lots of documentation as well - this name is only for branches that exist solely to update documentation.
* Branches that fix bugs __not__ on a tagged release should be named `fix/<what's-being-fixed>`
* Branches that fix bugs on a tagged release should be named `hotfix/<what's-being-fixed>`
* Refactors should be named `refactor/<what's-refactored-and-the-end-result>`

#### Commit Messages
Commit messages should start with a brief description of what the commit does - a verb phrase, like "add CONTRIBUTING.md" or "test foo under bar conditions". This verb phrase should indicate what changes will be caused by applying the code to the codebase. Then, if there is more to your commit than what you could say in the header, list everything else in bullets after two newline. For example:

```
apply scalastyle to test

 * fix tests to comply with scalastyle
 * drop magic number checker
 * removed MatrixCluster
```

You don't need to describe the full detail of every change – we can read the code for that. Just a brief overview of each task the commit accomplishes. On your first commit, remember to add your name to [CONTRIBUTORS.MD](CONTRIBUTORS.MD), as from the first commit onwards, you have contributed to Evvo!

#### Pull Requests
Pull requests require approval from an approved reviewer before being merged into master. Other branches do not have this protection, so merging together branches you have created locally won't cause any issues, and you can rebase your branch onto master as master is updated. Every pull request (and every commit run on a branch with an open pull request) will cause Travis build your code and run the whole test suite. Pull requests will only be allowed to merge if they pass the style checker and all tests. Pull request messages should be similar to commit messages. 

#### ScalaStyle
We use [`scalastyle`](http://www.scalastyle.org) as our style checker. You can find the configuration at [project/scalastyle_config.xml](project/scalastyle_config.xml). This style configuration may change over time, and we are open to suggestions – but make sure that any code you write passes the style configuration included in each push, or the Travis builds will fail. You can manually run the style checker with `mvn scalastyle:check`, which will print the filename and line number of every violation. You can also set up a pre-commit hook to run the style checker, as described [in the scalastyle docs](http://www.scalastyle.org/git-pre-commit-hook.html). Scalastyle is run on every pull request.

#### Issues, Projects, Milestones
Development is guided entirely by our [issues page](https://github.com/evvo-labs/evvo/issues). We use milestones to reflect which issues must be closed before the next tagged release. Projects are used to bundle issues that all touch the same component, or which depend on each other. In other words, if having a kanban board would be useful, use a project so that you get a kanban board.

#### Overcommit
We use [Overcommit](https://github.com/sds/overcommit) to run checks on every commit. Travis will also run these checks against any code it builds. Overcommit, when run locally, will only check files modified in the current commit, while Travis will run the checks on all files. While you don't have to use Overcommit, ensuring that your code will be accepted by Travis will be much easier if you do.

To install Overcommit, you will need Ruby >=2.4. To check your version, run 
```
$ ruby -v
ruby 2.3.7p456 (2018-03-28 revision 63024) [universal.x86_64-darwin18]
```

If your version is not 2.4 or greater, you will need to install a new Ruby. I used RVM to upgrade my Ruby installation, see [their docs](https://rvm.io/) to install it, and then you will be able to upgrade Ruby and install overcommit.

```
$ rvm install 2.4
$ ruby -v
ruby 2.4.6p354 (2019-04-01 revision 67394) [x86_64-darwin18]
$ gem install overcommit
```

To allow Overcommit to run scalastyle checks, you'll need a `scalastyle` executable on your $PATH.
```
$ brew install scalastyle
$ scalastyle -v
scalastyle 1.0.0
```

Locally install the Overcommit hooks (and those hook's dependencies) for this repository:

```
overcommit --install
gem install travis
```

Now, when you commit, you will see a message like
```
$ git commit
Running pre-commit hooks

✓ All pre-commit hooks passed

Running commit-msg hooks

✓ All commit-msg hooks passed
```

----------------------------------------------------------------------------------------------------
### IntelliJ
We all use IntelliJ as our primary editor for this project. If you prefer to use a different editor, let us know - there are probably things we failed to describe in this file because IntelliJ set them up for us. To set up IntelliJ for this project, you can simply clone it from Github using the "Import from VCS" dialogue, or clone it from the command line and then use the "Open project" dialogue.


----------------------------------------------------------------------------------------------------
### Locally Installing Evvo
If you are developing Evvo, for example patching it with changes you want for a project, you may want to use a local maven repository, so that your changes to Evvo can be seen by your project. Following the guide [here](https://sookocheff.com/post/java/local-maven-repository/):

Create a directory named `maven-repo` in your project, then deploy your current version of evvo to that repository by running the following command from the root  of `evvo`.
```
mvn deploy:deploy-file \
  -Durl=file:///<path to your project>/maven-repo \
  -Dfile=target/evvo_2.13-0.0.0.jar \
  -DgroupId=io.evvo \
  -DartifactId=evvo-dev \
  -Dpackaging=jar \
  -Dversion=0.0.0
```
Add the directory as a repository by changing your project's POM:
```
<repositories>
  <repository>
    <id>project.local</id>
    <name>project</name>
    <url>file:${project.basedir}/repo</url>
  </repository>
</repositories>
```
Add this new version of evvo, called evvo-dev, to your project's pom:
```
<dependency>
  <groupId>com.sookocheff</groupId>
  <artifactId>devlib</artifactId>
  <version>0.1</version>
</dependency>
```
And you should be good to go. 

----------------------------------------------------------------------------------------------------
#### Next Steps
You can find issues that we think are good fits for new contributors [here](https://github.com/evvo-labs/evvo/issues?q=is%3Aissue+is%3Aopen+label%3A%22good+first+issue%22). We look forward to seeing your PR! 
