
checkAndInstall <- function(mypkg){
   if (! is.element(mypkg, installed.packages()[,1])) install.packages(mypkg)
  } 

options(repos = "https://cran.stat.unipd.it/")

print("Loading libraries...")
checkAndInstall("validate")
checkAndInstall("validatetools")
checkAndInstall("errorlocate")
checkAndInstall("univOutl")
checkAndInstall("simputation")
checkAndInstall("VIM")
checkAndInstall("rspa")

print("Loading libraries...ok ")