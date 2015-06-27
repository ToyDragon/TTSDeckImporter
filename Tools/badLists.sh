ls lists -hltr | awk '{cmd="head -c 1 decks/"$9".json"; cmd | getline char;if(char == "["){print $9}}' 2> /dev/null
