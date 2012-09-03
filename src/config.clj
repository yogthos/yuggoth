(ns config
  (:use clojure.java.io yuggoth.models.schema)
  (:import java.io.File
           java.sql.DriverManager
           org.postgresql.ds.PGPoolingDataSource))

(def blog-config (atom nil))
(def db (atom nil))

(defn load-config-file []
  (let [url (.. (Thread/currentThread) getContextClassLoader (findResource "blog.properties"))] 
    (if (or (nil? url) (.. url getPath (endsWith "jar!/blog.properties")))
      (doto (new File "blog.properties") (.createNewFile))
      url)))

(defn reset-config [config]      
  (if (not-empty config)
    (reset! db 
            {:datasource 
             (doto (new PGPoolingDataSource)
               (.setServerName   (:host config) )
               (.setDatabaseName (:schema config))
               (.setPortNumber   (:port config))
               (.setUser         (:user config))                                  
               (.setPassword     (:pass config)))})
    (reset! blog-config (select-keys config [:ssl :ssl-port :initialized]))))

(defn init-config []
  (with-open
    [r (java.io.PushbackReader. (reader (load-config-file)))]
      (if-let [config (read r nil nil)]
        (reset-config config)))
  (println "configuration intialized"))

(defn write-config [config]   
  (with-open [con (DriverManager/getConnection 
                    (str "jdbc:postgresql://" (:host config) "/" (:schema config)) (:user config) (:pass config))])
  (with-open [w (clojure.java.io/writer (load-config-file))]
    (.write w (str config))
    (reset-config config)))

