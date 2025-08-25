# Set CRAN mirror
options(repos = c(CRAN = "https://cran.rstudio.com/"))

# Set number of cores for parallel installation
Ncpus <- parallel::detectCores()

# Install remotes if not already installed
if (!requireNamespace("remotes", quietly = TRUE)) {
    install.packages("remotes", Ncpus = Ncpus)
}

# Install packages with specific versions using remotes
remotes::install_version("jsonlite", version = "2.0.0", Ncpus = Ncpus)
remotes::install_version("curl", version = "7.0.0", Ncpus = Ncpus)
remotes::install_version("httr", version = "1.4.7", Ncpus = Ncpus)
remotes::install_version("base64enc", version = "0.1-3", Ncpus = Ncpus)
remotes::install_version("stringr", version = "1.5.1", Ncpus = Ncpus)
remotes::install_version("testthat", version = "3.2.3", Ncpus = Ncpus)
