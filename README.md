Git History Loader
===================
A simple tool that parse a git repo and generates statistics based on the same.
![git_stats_image](git_stats.png)

Usage
-----------
java -jar githistoryloader-1.0.jar  *git_repo_directory*

1.  The tool will parse the repo directory provided. 
2.  The branch will be the current git branch the repo directory is set to.
3. The git data is parsed and loaded to a in-memory database.
4. The reports are produced out of the data in the database.  

