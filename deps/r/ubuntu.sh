set -e

apt update && apt install -y build-essential \
     pandoc texinfo texlive-latex-extra \
     texlive-fonts-extra libz-dev libxml2-dev \
     libcurl4-openssl-dev libssl-dev libssl-doc \
     automake file libfribidi-dev libgfortran5 \
     libfontconfig1-dev libgdal-dev libzmq3-dev \
     libharfbuzz-dev libfreetype6-dev libpng-dev \
     libtiff5-dev libjpeg-dev libxml2-dev \
     libicu70 libgomp1 libreadline8

R -e "install.packages(c('devtools', 'roxygen2', 'usethis', 'miniUI', 'pkgdown', 'rcmdcheck', 'rversions', 'urlchecker'), repos = 'http://cran.us.r-project.org')"
