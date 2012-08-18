(ns config
  (:use clojure.java.io)
  (:import javax.sql.DataSource
           org.postgresql.ds.PGPoolingDataSource))

(def ^{:private true} config-file "blog.properties")
(def blog-config (atom nil))
(def db (atom nil))

(defn class-loader []
  (.. (Thread/currentThread) getContextClassLoader ))

(defn reset-config [config]
  (reset! blog-config (select-keys config [:ssl :ssl-port]))
  (reset! db 
          {:datasource 
           (doto (new PGPoolingDataSource)
             (.setServerName   (:host config) )
             (.setDatabaseName (:schema config))                       
             (.setUser         (:user config))                                  
             (.setPassword     (:pass config)))}))

(defn init-config []    
  (with-open
    [r (java.io.PushbackReader. (reader (.getResourceAsStream (class-loader) config-file)))]    
      (if-let [config (read r nil nil)]
        (reset-config config)        
        (println "could not read config file" config-file)))
  (println "configuration intialized"))

(defn write-config [config]  
  (with-open [w (clojure.java.io/writer (.findResource (class-loader) config-file))]
    (.write w (str config))
    (reset-config config)))
