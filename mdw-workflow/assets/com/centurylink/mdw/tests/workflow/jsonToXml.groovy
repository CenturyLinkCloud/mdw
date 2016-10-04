// maps jsonInput to new DOM Document jsonToXmlOutput

jsonToXmlOutput {
    namespaces << [ns: 'http://www.centurylink.com/mdw']
    ns.game {
        ns.name 'chess'
        ns.currentChamp jsonInput.chess.champion
        ns.gameboard jsonInput.chess.board
    }
}
