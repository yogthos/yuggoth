(defproject yuggoth "0.5.0-SNAPSHOT"
  
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  
  :dependencies
  [[org.clojure/clojure "1.4.0"]
   [lib-noir "0.3.4"]
   [compojure "1.1.3"]
   [hiccup "1.0.2"]
   [ring-server "0.2.5"]
   [clavatar "0.2.1"]
   [clj-rss "0.1.2"]
   [com.taoensso/timbre "1.2.0"]   
   [markdown-clj "0.9.18"]
   [net.sf.jlue/jlue-core "1.3"]
   [org.clojure/java.jdbc "0.2.3"]   
   [postgresql/postgresql "9.1-901.jdbc4"]]
  
  :ring
  {:handler yuggoth.handler/war-handler, 
   :init yuggoth.handler/init}
  
  :profiles
  {:production 
   {:ring
    {:open-browser? false, :stacktraces? false, :auto-reload? false}}
   :dev 
   {:dependencies [[ring-mock "0.1.3"] [ring/ring-devel "1.1.0"]]}}
   
  :plugins [[lein-ring "0.8.0"]])
