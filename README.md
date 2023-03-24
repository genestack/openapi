# Swagger API specification and documentation

This directory contains source files to generate the following artifacts:
- R and Python REST API client libraries
- Documentation for R and Python REST API client libraries

## Prerequisites
- Install Python2 and Python3 (follow [Python setup] instructions).
- Install the following prerequisites:
  ```
  brew install r

  # For building Swagger API documentation for Python:
  brew install pandoc
  pip install -r ./docs/python/requirements.txt

  # For building Swagger API documentation for R:
  brew install libgit2
  brew install basictex --cask
  R -e "install.packages(c('devtools', 'roxygen2'), repos = 'http://cran.us.r-project.org')"
  # on 09.12.2020, if you running MacOS Big Sur, you must execute
  # sudo tlmgr update --self
  # before running commands below
  sudo tlmgr install inconsolata
  sudo tlmgr install helvetic
  ```

[Python setup]: https://genestack.atlassian.net/wiki/x/NgBzGQ

## Building artifacts

The generated documentation varies depending on the product profile, which is
defined by `client.generator` build configuration parameter.
Additionally, several build configuration parameters are required to customize
hyperlinks in the resulting documentation.

The build command should be executed from the repository root directory:
- for `inc` configuration build command should look like:
  ```
  mvn clean package -DskipTests \
      -am -pl client \
      -Dclient.generator=inc
  ```
- for `odm` configuration build command should look like:
  ```
  mvn clean package -DskipTests \
      -am -pl client \
      -Dclient.generator=default
  ```

Resulting artifacts are generated in the `generated` directory at the
repository root.

## Troubleshooting

* If you see R error `pdflatex is not available` after installing `basictex`,
  try restart your session in Terminal: `pdflatex` must be in the `PATH` after
  restart.

* If you see roxygen2 installation errors due to unavailable `xml2` package, it
  may be because of libraries conflict. Install proper `xml2` package
  with xml2-config in the PATH, like:
  ```
  PATH=/usr/local/Cellar/libxml2/2.9.9_2/bin/:$PATH \
      R -e "install.packages(c('xml2'), repos = 'http://cran.us.r-project.org')"
  ```

* If `tlmgr` says that it requires update, try this:
  ```
  sudo tlmgr update --self
  ```
  or
  ```
  wget http://mirror.ctan.org/systems/texlive/tlnet/update-tlmgr-latest.sh
  chmod +x update-tlmgr-latest.sh
  sudo ./update-tlmgr-latest.sh
  ```
