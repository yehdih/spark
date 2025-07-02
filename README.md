# Apache Spark Mutual Friends Analysis

## Overview

This project demonstrates how to find mutual friends between users using Apache Spark's Resilient Distributed Datasets (RDDs). The application analyzes social network data to identify common connections between users.

## What is Apache Spark?

Apache Spark is a unified analytics engine for large-scale data processing. It provides high-level APIs in Java, Scala, Python, and R, and an optimized engine that supports general execution graphs.

### Key Spark Concepts

#### Resilient Distributed Datasets (RDDs)
- **Fault-tolerant**: Automatically recovers from node failures
- **Immutable**: Cannot be changed once created
- **Distributed**: Data is spread across multiple nodes
- **Lazy evaluation**: Operations are not executed until an action is called

```mermaid
graph TD
    A[Raw Data] --> B[RDD Creation]
    B --> C[Transformations]
    C --> D[Actions]
    D --> E[Results]
    
    C --> C1[map]
    C --> C2[filter]
    C --> C3[flatMap]
    C --> C4[reduceByKey]
    
    D --> D1[collect]
    D --> D2[count]
    D --> D3[save]
```

## Project Architecture

### Data Flow Diagram

```mermaid
graph LR
    A[Input Data User Friends List] --> B[Parse Lines Extract User Info]
    B --> C[Generate User Pairs Create Friend Sets]
    C --> D[Group by Pairs Find Intersections]
    D --> E[Add User Names Format Output]
    E --> F[Filter Results Display Mutual Friends]
```

### Spark Application Structure

```mermaid
graph TB
    SC[SparkContext] --> RDD1[Raw Data RDD]
    RDD1 --> RDD2[Parsed Users RDD]
    RDD2 --> RDD3[User Pairs RDD]
    RDD3 --> RDD4[Mutual Friends RDD]
    RDD4 --> RESULT[Final Results]
    
    SC --> DICT[Names Dictionary]
    DICT --> RESULT
```

## Code Breakdown

### 1. SparkContext Initialization

```python
from pyspark import SparkContext
sc = SparkContext("local", "MutualFriendsApp")
```

**Purpose**: Creates the entry point to Spark functionality
- `"local"`: Runs Spark locally on all available cores
- `"MutualFriendsApp"`: Application name for monitoring

### 2. Data Preparation

```python
data = [
    "1 Sidi 2,3,4",
    "2 Mohamed 1,3,5,4", 
    "3 Ahmed 1,2,4,5",
    "4 Mariam 1,3",
    "5 Zainab 2,3"
]
rdd = sc.parallelize(data)
```

**Data Format**: `user_id name friend_id1,friend_id2,friend_id3`

#### Social Network Visualization

```mermaid
graph TD
    U1[1: Sidi] --> U2[2: Mohamed]
    U1 --> U3[3: Ahmed]
    U1 --> U4[4: Mariam]
    
    U2 --> U1
    U2 --> U3
    U2 --> U5[5: Zainab]
    U2 --> U4
    
    U3 --> U1
    U3 --> U2
    U3 --> U4
    U3 --> U5
    
    U4 --> U1
    U4 --> U3
    
    U5 --> U2
    U5 --> U3
```

### 3. Data Parsing

```python
def parse_line(line):
    parts = line.strip().split()
    user_id = int(parts[0])
    name = parts[1]
    friends = list(map(int, parts[2].split(',')))
    return (user_id, name, friends)

users_rdd = rdd.map(parse_line)
```

**Transformation**: Raw strings → Structured tuples `(user_id, name, [friends])`

#### Parsing Flow

```mermaid
graph LR
    A["1 Sidi 2,3,4"] --> B[Split by space]
    B --> C[Extract user_id: 1]
    B --> D[Extract name: Sidi]
    B --> E[Extract friends: 2,3,4]
    E --> F[Split friends to list]
    
    C --> G[Tuple Result]
    D --> G
    F --> G
```

### 4. Pair Generation

```python
def generate_pairs(user_id, friends):
    return [((min(user_id, friend), max(user_id, friend)), set(friends)) 
            for friend in friends]

pairs_rdd = users_rdd.flatMap(lambda x: generate_pairs(x[0], x[2]))
```

**Purpose**: Creates user pairs with their respective friend sets
- `min/max`: Ensures consistent pair ordering (1,2) instead of (2,1)

#### Pair Generation Example

```mermaid
graph TD
    A[User 1 with friends 2,3,4] --> B[Generate pairs]
    B --> C[Pair 1,2 with friend set]
    B --> D[Pair 1,3 with friend set]
    B --> E[Pair 1,4 with friend set]
```

### 5. Finding Mutual Friends

```python
mutual_friends = pairs_rdd.reduceByKey(lambda x, y: x & y)
```

**Operation**: Groups pairs and finds set intersection using `&` operator

#### Mutual Friends Calculation

```mermaid
graph TD
    A[User 1 friends set] --> C[Intersection Operation]
    B[User 2 friends set] --> C
    C --> D[Mutual friends result]
    
    E[Remove self references]
    C --> E
    E --> F[Final mutual friends]
```

### 6. Name Resolution

```python
names_dict = users_rdd.map(lambda x: (x[0], x[1])).collectAsMap()
```

**Purpose**: Creates a lookup dictionary for user ID to name mapping

#### Dictionary Structure

```mermaid
graph LR
    A[users_rdd] --> B[map to id name pairs]
    B --> C[collectAsMap]
    C --> D[Dictionary Lookup Table]
```

### 7. Result Filtering and Display

```python
result = mutual_friends.filter(lambda x: x[0] == (1, 2)).collect()

for pair, mutual in result:
    id1, id2 = pair
    nom1, nom2 = names_dict[id1], names_dict[id2]
    mutual_names = [names_dict[friend_id] for friend_id in mutual]
    print(f"{id1}<{nom1}>{id2}<{nom2}> => Amis communs: {mutual_names}")
```

## Spark Operations Used

### Transformations (Lazy)
- **`map()`**: Transforms each element
- **`flatMap()`**: Transforms and flattens results
- **`filter()`**: Keeps elements matching condition
- **`reduceByKey()`**: Combines values for same key

### Actions (Eager)
- **`collect()`**: Returns all elements to driver
- **`collectAsMap()`**: Returns key-value pairs as dictionary

## Performance Characteristics

### RDD Lineage Graph

```mermaid
graph TD
    A[sc.parallelize] --> B[map: parse_line]
    B --> C[flatMap: generate_pairs]
    C --> D[reduceByKey: intersection]
    D --> E[filter: specific pair]
    E --> F[collect: results]
    
    B --> G[map: extract names]
    G --> H[collectAsMap: dictionary]
```

### Execution Plan

```mermaid
graph TD
    A[Stage 1: Data Loading and Parsing] --> B[Stage 2: Pair Generation]
    B --> C[Stage 3: Shuffle and Reduce]
    C --> D[Stage 4: Final Collection]
    
    E[Wide Transformation Shuffle Required] --> C
    F[Narrow Transformation No Shuffle] --> A
    F --> B
    F --> D
```

## Example Output

For the given data, when finding mutual friends between users 1 (Sidi) and 2 (Mohamed):

```
1<Sidi>2<Mohamed> => Amis communs: ['Ahmed', 'Mariam']
```

### Complete Mutual Friends Matrix

```mermaid
graph TD
    subgraph "Mutual Friends Analysis"
        A["Pair 1,2: Ahmed and Mariam"]
        B["Pair 1,3: Mohamed and Mariam"]
        C["Pair 1,4: Mohamed and Ahmed"]
        D["Pair 2,3: Sidi, Mariam, and Zainab"]
        E["Pair 2,4: Sidi and Ahmed"]
        F["Pair 2,5: Ahmed"]
        G["Pair 3,4: Sidi, Mohamed, and Zainab"]
        H["Pair 3,5: Mohamed"]
        I["Pair 4,5: None"]
    end
```

## Optimization Strategies

### 1. Partitioning
```python
# Partition data for better performance
pairs_rdd = pairs_rdd.partitionBy(numPartitions=4)
```

### 2. Caching
```python
# Cache frequently accessed RDDs
users_rdd.cache()
mutual_friends.cache()
```

### 3. Broadcast Variables
```python
# For large lookup dictionaries
broadcast_names = sc.broadcast(names_dict)
```

## Scaling Considerations

### Data Size Impact

```mermaid
graph LR
    A[Small Data less than 1GB] --> B[Single Machine local mode]
    C[Medium Data 1GB to 100GB] --> D[Small Cluster 2 to 10 nodes]
    E[Large Data greater than 100GB] --> F[Large Cluster 10 plus nodes]
```

### Memory Management

```mermaid
graph TD
    A[Executor Memory] --> B[Storage Memory 60 percent]
    A --> C[Execution Memory 40 percent]
    
    B --> D[Cached RDDs]
    B --> E[Broadcast Variables]
    
    C --> F[Shuffle Data]
    C --> G[Task Execution]
```

## Error Handling

### Common Issues
1. **OutOfMemoryError**: Increase executor memory or reduce data size
2. **Serialization errors**: Ensure all functions are serializable
3. **File not found**: Check data paths and permissions

### Monitoring
- **Spark UI**: http://localhost:4040
- **Application logs**: Check driver and executor logs
- **Metrics**: Monitor CPU, memory, and network usage

## Extensions

### 1. File-based Input
```python
# Read from HDFS or local file system
rdd = sc.textFile("hdfs://path/to/social_network.txt")
```

### 2. Multiple Queries
```python
# Find mutual friends for all pairs
all_results = mutual_friends.collect()
```

### 3. Recommendation System
```python
# Suggest friends based on mutual connections
def suggest_friends(user_id, mutual_threshold=2):
    # Implementation here
    pass
```

## Conclusion

This Spark application demonstrates core distributed computing concepts:
- **Data parallelization** across multiple cores/nodes
- **Fault tolerance** through RDD lineage
- **Lazy evaluation** for optimization
- **Functional programming** paradigms

The mutual friends algorithm showcases how complex social network analysis can be efficiently performed using Spark's distributed computing capabilities.

## Resources

- [Apache Spark Documentation](https://spark.apache.org/docs/latest/)
- [PySpark API Reference](https://spark.apache.org/docs/latest/api/python/)
- [Spark Performance Tuning Guide](https://spark.apache.org/docs/latest/tuning.html)
