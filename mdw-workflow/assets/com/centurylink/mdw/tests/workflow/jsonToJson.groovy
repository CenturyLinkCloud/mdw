// maps jsonInput to new JSONObject jsonToJsonOutput

jsonToJsonOutput {
    game {
        name 'chess'
        currentChamp jsonInput.chess.champion
        gameboard jsonInput.chess.board
    }
}
