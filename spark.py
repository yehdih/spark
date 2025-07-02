from pyspark import SparkContext
sc = SparkContext("local", "MutualFriendsApp")
# Simuler les données (dans le vrai cas, utilisez sc.textFile("chemin_du_fichier"))
data = [
    "1 Sidi 2,3,4",
    "2 Mohamed 1,3,5,4",
    "3 Ahmed 1,2,4,5",
    "4 Mariam 1,3",
    "5 Zainab 2,3"
]
rdd = sc.parallelize(data)

# Étape 1: Extraire les données sous forme (user_id, nom, [friend_ids])
def parse_line(line):
    parts = line.strip().split()
    user_id = int(parts[0])
    name = parts[1]
    friends = list(map(int, parts[2].split(',')))
    return (user_id, name, friends)

users_rdd = rdd.map(parse_line)

# Étape 2: Créer une RDD de (user, friend) -> list of user’s friends
def generate_pairs(user_id, friends):
    return [((min(user_id, friend), max(user_id, friend)), set(friends)) for friend in friends]

pairs_rdd = users_rdd.flatMap(lambda x: generate_pairs(x[0], x[2]))

# Étape 3: Grouper par paire et intersecter les listes pour obtenir les amis communs
mutual_friends = pairs_rdd.reduceByKey(lambda x, y: x & y)

# Étape 4: Ajouter noms (en supposant que les noms sont accessibles)
names_dict = users_rdd.map(lambda x: (x[0], x[1])).collectAsMap()

# Étape 5: Trouver la paire (1, 2)
result = mutual_friends.filter(lambda x: x[0] == (1, 2)).collect()

# Affichage
for pair, mutual in result:
    id1, id2 = pair
    nom1, nom2 = names_dict[id1], names_dict[id2]
    mutual_names = [names_dict[friend_id] for friend_id in mutual]
    print(f"{id1}<{nom1}>{id2}<{nom2}> => Amis communs: {mutual_names}")
