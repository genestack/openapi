publicRepo <- "https://packagemanager.rstudio.com/all/__linux__/jammy/latest"
options("repos" = c(PUBLIC = publicRepo))

install.packages("remotes")

# Install public packages
remotes::install_version("devtools", version = "2.4.5")
remotes::install_version("roxygen2", version = "7.2.3")
remotes::install_version("usethis", version = "2.1.6")
remotes::install_version("miniUI", version = "0.1.1.1")
remotes::install_version("pkgdown", version = "2.0.7")
remotes::install_version("rcmdcheck", version = "1.4.0")
remotes::install_version("rversions", version = "2.1.2")
remotes::install_version("urlchecker", version = "1.0.1")
