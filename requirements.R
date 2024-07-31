library(parallel)
Ncpus <- detectCores()

publicRepo <- "https://packagemanager.rstudio.com/all/__linux__/jammy/latest"
options("repos" = c(PUBLIC = publicRepo))

install.packages("remotes", Ncpus = Ncpus)

# Install public packages
remotes::install_version("jsonlite", version = "1.8.8", Ncpus = Ncpus)
remotes::install_version("curl", version = "5.2.0", Ncpus = Ncpus)
remotes::install_version("httr", version = "1.4.7", Ncpus = Ncpus)
remotes::install_version("base64enc", version = "0.1-3", Ncpus = Ncpus)
remotes::install_version("stringr", version = "1.5.1", Ncpus = Ncpus)
remotes::install_version("testthat", version = "3.2.1", Ncpus = Ncpus)
