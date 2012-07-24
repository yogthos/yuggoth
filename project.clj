(defproject yuggoth "0.4.0-SNAPSHOT"
            :description "personal blog engine"
            :dependencies [[org.clojure/clojure "1.4.0"]
                           [noir "1.3.0-beta3" :exclude [org.clojure/clojure]]
                           [markdown-clj "0.9"]
                           [org.clojure/java.jdbc "0.2.3"]
                           [net.sf.jlue/jlue-core "1.3"]
                           [joda-time "2.0"]
                           [postgresql/postgresql "9.1-901.jdbc4"]]
	    :dev-dependencies [[lein-ring "0.7.1"]]
     :ring {:handler yuggoth.server/handler}
     :main yuggoth.server)

