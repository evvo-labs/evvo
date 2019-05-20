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