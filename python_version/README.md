# Jaeger UML Generator - Python Version

Tool Python per generare diagrammi UML (Component, Sequence, Deployment) in formato XMI 2.5.1 a partire da tracce distribuite di Jaeger.

## Prerequisiti

- **Python 3.9** o superiore
- **pip** per l'installazione delle dipendenze

## Installazione

### 1. Crea un ambiente virtuale (raccomandato)

```bash
cd python_version
python3 -m venv venv
source venv/bin/activate  # Su Linux/macOS
# oppure
venv\Scripts\activate  # Su Windows
```

### 2. Installa le dipendenze

```bash
pip install -r requirements.txt
```

### 3. Installazione del pacchetto (opzionale)

```bash
pip install -e .
```

Questo renderà disponibile il comando `jaeger-uml-generator` nel sistema.

## Utilizzo

### Sintassi Base

```bash
python -m jaeger_uml_generator.main [OPTIONS]
```

Oppure, se installato:

```bash
jaeger-uml-generator [OPTIONS]
```

### Opzioni

- `-f, --input-file <file>`: File JSON contenente trace Jaeger
- `-d, --input-dir <dir>`: Directory contenente file JSON di trace
- `-j, --jaeger-url <url>`: URL dell'API Jaeger (es. http://localhost:16686)
- `-s, --service <name>`: Filtra le trace per nome servizio (solo con --jaeger-url)
- `-o, --output-dir <dir>`: Directory di output per i diagrammi (default: ./output)
- `-t, --diagram-type <type>`: Tipo di diagramma: sequence, component, deployment, all (default: all)
- `-l, --limit <number>`: Numero massimo di trace da recuperare dall'API Jaeger (default: 100)
- `--lookback <time>`: Periodo di tempo da analizzare (default: 24h)
- `-v, --verbose`: Abilita logging dettagliato
- `-h, --help`: Mostra l'help

### Esempi

#### 1. Generare tutti i diagrammi da un file JSON

```bash
python -m jaeger_uml_generator.main \
  --input-file ../traces/trace.json \
  --output-dir ./output
```

#### 2. Generare solo Sequence Diagrams da una directory

```bash
python -m jaeger_uml_generator.main \
  --input-dir ../traces \
  --diagram-type sequence \
  --output-dir ./diagrams
```

#### 3. Recuperare trace dall'API Jaeger e generare tutti i diagrammi

```bash
python -m jaeger_uml_generator.main \
  --jaeger-url http://localhost:16686 \
  --service frontend \
  --diagram-type all \
  --output-dir ./output
```

#### 4. Con logging verbose

```bash
python -m jaeger_uml_generator.main \
  -f ../traces/trace.json \
  -o ./output \
  -v
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
- Supporta filtri per servizio
- Recupera le trace del periodo specificato (default: ultime 24 ore)

## Output

Il tool genera file **XMI 2.5.1** compatibili con:
- Eclipse Papyrus
- Enterprise Architect
- Altri tool UML che supportano XMI

Output per tipo:
- `sequence-<trace_name>.xmi`: Sequence Diagram (uno per trace)
- `component-<trace_name>.xmi`: Component Diagram
- `deployment-<trace_name>.xmi`: Deployment Diagram

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
- Stereotipi automatici (WebUI, Microservice, Database, etc.)

### Deployment Diagram
Topologia infrastrutturale estratta dai tag delle span. Mostra:
- Nodi (host, pod, container)
- Namespace Kubernetes
- Deployment dei servizi
- Comunicazione tra nodi

> **Nota**: La qualità del Deployment Diagram dipende dai tag presenti nelle trace Jaeger. Tag comuni: `hostname`, `pod.name`, `namespace`, `k8s.namespace`.

## Struttura del Progetto

```
python_version/
├── jaeger_uml_generator/        # Pacchetto principale
│   ├── models/                  # Modelli dati (Trace, Span, Process)
│   ├── input/                   # Lettori (JSON, Jaeger API)
│   ├── analyzer/                # Aggregatori e analizzatori
│   ├── generators/              # Generatori UML
│   ├── renderer/                # Writer XMI
│   ├── cli/                     # Interfaccia command-line
│   ├── utils/                   # Utility
│   └── main.py                  # Entry point
├── requirements.txt             # Dipendenze
├── setup.py                     # Setup per installazione
└── README.md                    # Questa documentazione
```

## Confronto con la Versione Java

La versione Python mantiene la piena compatibilità con quella Java:
- **Stesso formato XMI 2.5.1**
- **Stessa logica di generazione**
- **Stessi tipi di diagrammi**
- **Nomi file identici**

Vantaggi della versione Python:
- Più leggera (nessuna dipendenza da JVM)
- Installazione più semplice
- Codice più compatto
- Integrazione facile in pipeline Python

## Troubleshooting

### Errore: "No module named 'requests'"
```bash
pip install requests
```

### Errore: "No traces found"
- Verifica il formato JSON del file
- Controlla che il servizio Jaeger sia raggiungibile
- Usa `--verbose` per logging dettagliato

### Diagrammi vuoti
- Alcune trace potrebbero non avere abbastanza informazioni
- Verifica che le trace contengano span con riferimenti parent-child

### File XMI non valido
- Verifica che l'output sia stato salvato correttamente
- Controlla il log per eventuali errori durante la generazione

## Test

Per testare il tool con un file di esempio:

```bash
# Dalla directory python_version
python -m jaeger_uml_generator.main \
  -f ../traces/trace.json \
  -o ./test_output \
  -v
```

## Sviluppo

Per contribuire allo sviluppo:

1. Clona il repository
2. Crea un ambiente virtuale
3. Installa in modalità development: `pip install -e .`
4. Esegui i test (se disponibili)

## Licenza

Questo progetto è stato creato per scopi educativi.
