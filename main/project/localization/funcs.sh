# Utility functions for location-aware programs

getnames () {
    sed -ne 's/^.*<\(string\|plurals\)\s*name\s*=\s*"\([^\"]*\)".*$/\2/p' $1
}
