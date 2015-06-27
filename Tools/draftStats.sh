ls -hltr decks | grep json | grep -v "[a-z]\{16\}\.json$" | awk '{ A[$6" "$7]++}END{for(i in A){print i" {"A[i]"}"}}'
