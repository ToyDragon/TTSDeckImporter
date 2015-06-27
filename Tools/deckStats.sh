ls decks/ -hltr |grep json | awk '{ A[$6" "$7]++}END{for(i in A){print i" {"A[i]"}"}}'
