def url = new URL("http://localhost:8081/nexus/service/local/status")
def times = 1_000_000

def start = new Date()
for (int i in 0..times) {
  url.text
}
def stop = new Date()
def e = stop.time - start.time
println "elasped $e ms"
