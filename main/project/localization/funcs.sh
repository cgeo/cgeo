# Utility functions for location-aware programs

getnames () {
    sed -ne 's/^.*<string\s*name\s*=\s*"\([^\"]*\)".*$/\1/p' $1
}
