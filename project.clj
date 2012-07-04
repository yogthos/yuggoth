(defproject yuggoth "0.1.0-SNAPSHOT"
            :description "personal blog engine"
            :dependencies [[org.clojure/clojure "1.4.0"]
                           [noir "1.3.0-beta3"]
                           [markdown-clj "0.8"]
                           [org.clojure/java.jdbc "0.2.3"]
                           [hsqldb/hsqldb "1.8.0.10"]]
            :main yuggoth.server)

