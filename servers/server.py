from flask import Flask, request, jsonify
import random

app = Flask(__name__)

# Memoria simple del juego
games = {}

@app.route('/game/state', methods=['GET'])
def get_game_state():
    game_id = request.args.get('gameId')

    # Si la partida no existe, devolvemos datos vacíos
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
    data = request.json
    game_id = data.get('gameId')
    player_name = data.get('playerName')

    if game_id not in games:
        # Crear nueva partida
        games[game_id] = {
            "gameId": game_id,
            "player1": player_name,
            "player2": None,
            "turn": player_name, # Empieza el que crea
            "status": "WAITING",
            "winner": None,
            "lastMoveRow": None,
            "lastMoveCol": None,
            "status": "LOBBY",
            "ships_p1": [], # Lista de índices donde P1 tiene barcos
            "ships_p2": [], # Lista de índices donde P2 tiene barcos
            "p1_ready": False,
            "p2_ready": False
        }
    else:
        # Unirse a partida existente
        game = games[game_id]
        if game['player2'] is None and game['player1'] != player_name:
            game['player2'] = player_name
            game['status'] = "PLAYING"
            game['turn'] = game['player1']
    return jsonify(games[game_id])



@app.route('/game/place', methods=['POST'])
def place_ships():
    data = request.json
    game_id = data['gameId']
    player = data['playerName']
    ships = data['ships'] # Lista de números, ej: [0, 5, 24]

    game = games.get(game_id)
    if not game: return jsonify({"error": "No existe la partida"}), 404

    # Guardamos los barcos del jugador correspondiente
    if player == game['player1']:
        game['ships_p1'] = ships
        game['p1_ready'] = True
    elif player == game['player2']:
        game['ships_p2'] = ships
        game['p2_ready'] = True

    # Si AMBOS están listos, empieza la guerra
    if game['p1_ready'] and game['p2_ready']:
        game['status'] = "PLAYING"
        game['turn'] = game['player1'] # Empieza el jugador 1 por defecto

    return jsonify(game)


@app.route('/game/attack', methods=['POST'])
def handle_attack():
    data = request.json
    game_id = data['gameId']
    shooter = data['playerName']
    row = data.get('row')
    col = data.get('col')

    game = games.get(game_id)
    if not game: return jsonify({"status": "ERROR"})

    # Calculamos el índice lineal (0-24) basado en fila/columna
    target_index = row * 5 + col
    result = "MISS" # Por defecto es Agua 💧

    # LÓGICA DE IMPACTO
    # Si dispara el Jugador 1, miramos los barcos del Jugador 2
    if shooter == game['player1']:
        if target_index in game['ships_p2']:
            result = "HIT" 💥
            # Opcional: Podrías quitar el barco de la lista para saber cuándo hundió todos

    # Si dispara el Jugador 2, miramos los barcos del Jugador 1
    elif shooter == game['player2']:
        if target_index in game['ships_p1']:
            result = "HIT" 💥

    # Cambiamos el turno
    if shooter == game['player1']:
        game['turn'] = game['player2']
    else:
        game['turn'] = game['player1']

    # Guardamos el último movimiento para que el otro lo vea
    game['lastMoveRow'] = row
    game['lastMoveCol'] = col
    # Importante: Podrías querer enviar si fue HIT o MISS al otro jugador también
# Truco rápido: Guardamos los aciertos en el estado del juego
    if 'hits_p1' not in game: game['hits_p1'] = 0
    if 'hits_p2' not in game: game['hits_p2'] = 0

    if result == "HIT":
        if shooter == game['player1']:
            game['hits_p1'] += 1
            if game['hits_p1'] >= 3: # Si hunde los 3 barcos
                game['winner'] = game['player1']
        else:
            game['hits_p2'] += 1
            if game['hits_p2'] >= 3:
                game['winner'] = game['player2']

    return jsonify({
        "status": result,
        "lastMoveRow": row,
        "lastMoveCol": col,
        "winner": game.get('winner') # Importante devolver esto
    })

if __name__ == '__main__':
    # host='0.0.0.0' permite que el emulador o el móvil accedan a tu PC
    app.run(host='0.0.0.0', port=5000)