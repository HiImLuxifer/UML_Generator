# Jaeger UML Generator

Tool Java per generare diagrammi UML (Component, Sequence, Deployment) a partire da tracce distribuite di Jaeger.

## Prerequisiti

- **Java 17** o superiore
- **Maven 3.6+**
- **Graphviz** (richiesto da PlantUML per il rendering)

### Installazione Maven

#### Windows
1. Scarica Maven da: https://maven.apache.org/download.cgi
2. Estrai il file zip in `C:\Program Files\Apache\maven`
3. Aggiungi `C:\Program Files\Apache\maven\bin` alla variabile d'ambiente PATH
4. Verifica: `mvn -version` in un nuovo terminale

#### Linux (Ubuntu/Debian)
```bash
sudo apt-get install maven
```

#### macOS
```bash
brew install maven
```

### Installazione Graphviz

#### Windows
Scarica e installa da: https://graphviz.org/download/
Assicurati che `dot.exe` sia nel PATH.

#### Linux (Ubuntu/Debian)
```bash
sudo apt-get install graphviz
```

#### macOS
```bash
brew install graphviz
```

## Build del Progetto

```bash
mvn clean package
```

Questo comando compila il progetto e crea un JAR eseguibile in `target/jaeger-uml-generator-1.0.0.jar`.

## Utilizzo

### Sintassi Base

```bash
java -jar target/jaeger-uml-generator-1.0.0.jar [OPTIONS]
```

### Opzioni

- `-f, --input-file <file>`: File JSON contenente trace Jaeger
- `-d, --input-dir <dir>`: Directory contenente file JSON di trace
- `-j, --jaeger-url <url>`: URL dell'API Jaeger (es. http://localhost:16686)
- `-s, --service <name>`: Filtra le trace per nome servizio (solo con --jaeger-url)
- `-o, --output-dir <dir>`: Directory di output per i diagrammi (default: ./output)
- `-t, --diagram-type <type>`: Tipo di diagramma: sequence, component, deployment, all (default: all)
- `-l, --limit <number>`: Numero massimo di trace da recuperare dall'API Jaeger (default: 100)
- `-v, --verbose`: Abilita logging dettagliato
- `-h, --help`: Mostra l'help

### Esempi

#### 1. Generare tutti i diagrammi da un file JSON

```bash
java -jar target/jaeger-uml-generator-1.0.0.jar \
  --input-file src/test/resources/sample-traces/example-trace.json \
  --output-dir ./output
```

#### 2. Generare solo Sequence Diagrams da una directory

```bash
java -jar target/jaeger-uml-generator-1.0.0.jar \
  --input-dir /path/to/traces \
  --diagram-type sequence \
  --output-dir ./diagrams
```

#### 3. Recuperare trace dall'API Jaeger e generare tutti i diagrammi

```bash
java -jar target/jaeger-uml-generator-1.0.0.jar \
  --jaeger-url http://localhost:16686 \
  --service frontend \
  --diagram-type all \
  --output-dir ./output
```

## Formato Input

### File JSON

Il tool accetta file JSON nel formato standard di Jaeger. Supporta:
- Singola trace
- Array di trace
- Formato API Jaeger con campo `"data"`

Esempio:
```json
{
  "data": [
    {
      "traceID": "abc123",
      "spans": [...],
      "processes": {...}
    }
  ]
}
```

### API Jaeger

Il tool può interrogare direttamente l'API di Jaeger:
- Endpoint: `http://<jaeger-host>:16686/api/traces`
- Supporta filtri per servizio e operazione
- Recupera le ultime 24 ore di trace

## Output

Il tool genera:
1. **File PlantUML** (`.puml`): Codice sorgente PlantUML
2. **Immagini PNG** (`.png`): Diagrammi renderizzati

Output per tipo:
- `sequence-diagram.png`: Sequence Diagram (una per trace)
- `component-diagram.png`: Component Diagram (aggregato)
- `deployment-diagram.png`: Deployment Diagram (aggregato)

## Tipi di Diagramma

### Sequence Diagram
Mostra il flusso cronologico delle chiamate tra servizi per ogni trace. Include:
- Partecipanti (servizi)
- Chiamate inter-servizio
- Timing delle operazioni

### Component Diagram
Vista aggregata dell'architettura del sistema. Mostra:
- Servizi come componenti
- Operazioni esposte
- Dipendenze tra servizi

### Deployment Diagram
Topologia infrastrutturale estratta dai tag delle span. Mostra:
- Nodi (host, pod, container)
- Namespace Kubernetes
- Deployment dei servizi

> **Nota**: La qualità del Deployment Diagram dipende dai tag presenti nelle trace Jaeger. Tag comuni: `hostname`, `pod.name`, `namespace`, `k8s.namespace`.

## Test

Esegui i test unitari:
```bash
mvn test
```

Prova con il file di esempio incluso:
```bash
java -jar target/jaeger-uml-generator-1.0.0.jar \
  -f src/test/resources/sample-traces/example-trace.json \
  -o ./test-output \
  -v
```

## Troubleshooting

### Errore: "Graphviz not found"
- Assicurati che Graphviz sia installato
- Verifica che `dot` sia nel PATH: `dot -V`

### Nessuna trace trovata
- Verifica il formato JSON del file
- Controlla che il servizio Jaeger sia raggiungibile
- Usa `--verbose` per logging dettagliato

### Diagrammi vuoti
- Alcune trace potrebbero non avere abbastanza informazioni
- Verifica che le trace contengano span con riferimenti parent-child

## Struttura del Progetto

```
jaeger-uml-generator/
├── src/main/java/com/uml/generator/
│   ├── model/          # Classi modello per Trace, Span, Process
│   ├── input/          # Lettori per JSON e API Jaeger
│   ├── analyzer/       # Aggregatori e analizzatori
│   ├── generator/      # Generatori UML (Sequence, Component, Deployment)
│   ├── renderer/       # Renderer PlantUML → PNG
│   ├── cli/            # Interfaccia command-line
│   └── JaegerUmlGenerator.java  # Classe main
├── src/test/
│   └── resources/sample-traces/
└── output/             # Directory di output (generata)
```

## Licenza

Questo progetto è stato creato per scopi educativi.
