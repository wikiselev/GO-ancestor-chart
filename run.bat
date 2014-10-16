echo 'QuickGO will be started shortly at:'
echo 'http://127.0.0.1:8080/QuickGO/'

java -Xmx256m -jar QuickGO5.1.jar web --config quickgo-config-mini.xml --port 8080 --root /QuickGO --password changeme
