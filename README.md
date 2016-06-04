# TTSDeckImporter
This is an MTG deck importer for Table Top Simulator.

# Web server
The web server hosts some static pages, the set/deck assets, and interacts with the deck maker. The web server also logs errors and decks that are made and sends email reports.

Run the web server with 
>node server.js

# Deck Maker
The deck maker code is stored in libs/src, and the entry point is core.DeckMaker. This main class recieves and parses requests from the web server. It uses the classes in the utils package to retrieve the card assets, stitch the decks, and construct the JSON files.

Run the deck maker with 
>java -cp libs/gson-2.3.1.jar;libs/bin core.DeckMaker
