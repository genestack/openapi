publicRepo <- "https://packagemanager.rstudio.com/all/__linux__/jammy/latest"
options("repos" = c(PUBLIC = publicRepo))

install.packages("remotes")

# Install public packages
remotes::install_version("jsonlite", version = "1.8.8")
remotes::install_version("httr", version = "1.4.7")
remotes::install_version("base64enc", version = "0.1-3")
remotes::install_version("stringr", version = "1.5.1")
