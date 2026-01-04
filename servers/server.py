from flask import Flask, request, jsonify

app = Flask(__name__)

# In-memory game storage
# Structure: { "gameId": { game_state_dict } }
games = {}

@app.route('/game/state', methods=['GET'])
def get_game_state():
    """
    Retrieves the current snapshot of the game.
    Used by the Android client for polling status updates.
    """
    game_id = request.args.get('gameId')

    # If the game does not exist, return an empty/default state
    if game_id not in games:
        return jsonify({
            "gameId": game_id,
            "player1": None,
            "player2": None,
            "turn": None,
            "status": "LOBBY",
            "winner": None,
            "lastMoveRow": None,
            "lastMoveCol": None
        })

    return jsonify(games[game_id])


@app.route('/game/join', methods=['POST'])
def join_game():
    """
    Handles players joining a game room.
    - If the room doesn't exist, it creates it (Player 1).
    - If the room exists and has space, Player 2 joins.
    """
    data = request.json
    game_id = data.get('gameId')
    player_name = data.get('playerName')

    if game_id not in games:
        # Create a new game instance
        games[game_id] = {
            "gameId": game_id,
            "player1": player_name,
            "player2": None,
            "turn": player_name, # Creator starts by default
            "winner": None,
            "lastMoveRow": None,
            "lastMoveCol": None,
            "status": "LOBBY",   # Initial state
            "ships_p1": [],      # List of grid indices for P1 ships
            "ships_p2": [],      # List of grid indices for P2 ships
            "p1_ready": False,
            "p2_ready": False,
            "hits_p1": 0,        # Track successful hits for P1
            "hits_p2": 0         # Track successful hits for P2
        }
    else:
        # Join an existing game
        game = games[game_id]
        if game['player2'] is None and game['player1'] != player_name:
            game['player2'] = player_name
            # Both players present, move to SETUP phase logic (handled by client)
            # The turn remains with Player 1 initially
            game['turn'] = game['player1']

    return jsonify(games[game_id])


@app.route('/game/place', methods=['POST'])
def place_ships():
    """
    Receives the ship positions from a player.
    Transition the game state to 'PLAYING' only when both players are ready.
    """
    data = request.json
    game_id = data['gameId']
    player = data['playerName']
    ships = data['ships'] # List of grid indices, e.g., [0, 5, 24]

    game = games.get(game_id)
    if not game:
        return jsonify({"error": "Game not found"}), 404

    # Store ships for the specific player
    if player == game['player1']:
        game['ships_p1'] = ships
        game['p1_ready'] = True
    elif player == game['player2']:
        game['ships_p2'] = ships
        game['p2_ready'] = True

    # Check if BOTH players are ready to start the war
    if game['p1_ready'] and game['p2_ready']:
        game['status'] = "PLAYING"
        game['turn'] = game['player1'] # Default: Player 1 starts

    return jsonify(game)


@app.route('/game/attack', methods=['POST'])
def handle_attack():
    """
    Processes an attack move.
    1. Checks if the shot hit a ship.
    2. Switches turns.
    3. Checks for a victory condition (3 hits).
    """
    data = request.json
    game_id = data['gameId']
    shooter = data['playerName']
    row = data.get('row')
    col = data.get('col')

    game = games.get(game_id)
    if not game:
        return jsonify({"status": "ERROR"})

    # Calculate 1D index from 2D coordinates (5x5 grid)
    target_index = row * 5 + col
    result = "MISS"

    # 1. HIT LOGIC
    # If P1 shoots, check P2's ships
    if shooter == game['player1']:
        if target_index in game['ships_p2']:
            result = "HIT"
    # If P2 shoots, check P1's ships
    elif shooter == game['player2']:
        if target_index in game['ships_p1']:
            result = "HIT"

    # 2. TURN SWITCHING
    if shooter == game['player1']:
        game['turn'] = game['player2']
    else:
        game['turn'] = game['player1']

    # 3. UPDATE LAST MOVE (For UI synchronization)
    game['lastMoveRow'] = row
    game['lastMoveCol'] = col

    # 4. VICTORY CHECK
    # Initialize hit counters if they don't exist (failsafe)
    if 'hits_p1' not in game: game['hits_p1'] = 0
    if 'hits_p2' not in game: game['hits_p2'] = 0

    if result == "HIT":
        if shooter == game['player1']:
            game['hits_p1'] += 1
            # Win Condition: 3 Ships sunk
            if game['hits_p1'] >= 3:
                game['winner'] = game['player1']
                game['status'] = "FINISHED"
        else:
            game['hits_p2'] += 1
            if game['hits_p2'] >= 3:
                game['winner'] = game['player2']
                game['status'] = "FINISHED"

    return jsonify({
        "status": result, # "HIT" or "MISS"
        "lastMoveRow": row,
        "lastMoveCol": col,
        "winner": game.get('winner')
    })

if __name__ == '__main__':
    # host='0.0.0.0' makes the server accessible from other devices (like the Android Emulator)
    app.run(host='0.0.0.0', port=5000)