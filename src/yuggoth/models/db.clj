(ns yuggoth.models.db
  (:require [clojure.java.jdbc :as sql])
  (:import java.sql.Timestamp 
           java.util.Date
           javax.sql.DataSource
           org.postgresql.ds.PGPoolingDataSource))

#_ (def ^{:private true} db 
  {:datasource (doto (new PGPoolingDataSource)
                 (.setServerName   "localhost")
                 (.setDatabaseName "yourdb")                       
                 (.setUser         "user")                 
                 (.setPassword     "pass")
                 (.setMaxConnections 10))})

(defn drop-table
  "drops the supplied table from the DB, table name must be a keyword
eg: (drop-table :users)"
  [table]
  (try
   (sql/with-connection db (sql/drop-table table))
   (catch Exception _)))

(defn db-read
  "returns the result of running the supplied SQL query"
  [query & args]
  (sql/with-connection 
    db
    (sql/with-query-results res (vec (cons query args)) (doall res))))

(defn transaction
  "runs a function with the supplied arguments in an SQL transaction
eg: (transaction add-user email firstname lastname password)"
  [f & args]
  (sql/with-connection db
    (sql/transaction
      (apply f args))))

;file management

(defn create-file-table []
  (sql/create-table
    :file
    [:type "varchar(50)"]
    [:name "varchar(50)"]
    [:data "bytea"]))

(defn to-byte-array [x]  
  (with-open [input (new java.io.FileInputStream x)
              buffer (new java.io.ByteArrayOutputStream)]
    (clojure.java.io/copy input buffer)
    (.toByteArray buffer)))

(defn fix-file-name [filename]
  (.replaceAll filename "[^a-zA-Z0-9-\\.]" ""))

(defn store-file [{:keys [tempfile filename content-type]}]
  (sql/with-connection 
    db
    (sql/update-or-insert-values
      :file
      ["name=?" filename]
      {:type content-type :name (fix-file-name filename) :data (to-byte-array tempfile)})))

(defn list-files []
  (map :name (db-read "select name from file")))

(defn delete-file [name]
  (sql/with-connection db (sql/delete-rows :file ["name=?" name])))

(defn get-file [name]
  (first (db-read "select * from file where name=?" name)))

;;blog table management
(defn create-blog-table []
  (sql/create-table
    :blog
    [:id "SERIAL"]
    [:time :timestamp]
    [:title "varchar(100)"]
    [:content "TEXT"]
    [:author "varchar(100)"]    
    [:public boolean]))

(defn update-post [id title content public]
  (let [int-id (Integer/parseInt id)] 
    (sql/with-connection
      db
      (sql/update-values
        :blog
        ["id=?" int-id]
        {:id int-id :title title :content content :public (Boolean/parseBoolean public)}))))

(defn get-posts [& [limit full? private?]]    
  (try
    (db-read (str "select id, time, title, public" (if full? ", content") 
                  " from blog " (if (not private?) "where public='true'") " order by id desc " 
                  (if limit (str "limit " limit))))
    (catch Exception ex nil)))

(defn get-post [id]  
  (first (db-read "select * from blog where id=?" (Integer/parseInt id))))

(defn get-public-post-id [id next?]
  (:id
    (first
      (db-read 
        (if next?
          "select id from blog where id > ? and public='true' order by id asc limit 1"
          "select id from blog where id < ? and public='true' order by id desc limit 1") 
        (Integer/parseInt id)))))


(defn store-post [title content author public]
  (sql/with-connection 
    db
    (sql/insert-values
      :blog
      [:time :title :content :author :public]
      [(new Timestamp (.getTime (new Date))) title content author (Boolean/parseBoolean public)])))

(defn post-visible [id public]
  (sql/with-connection 
    db
    (sql/update-values
      :blog 
      ["id=?" (Integer/parseInt id)]
      {:public public})))

(defn get-last-post [] 
  (first (db-read "select * from blog where id = (select max(id) from blog)")))

(defn last-post-id []
  (or (:id (first (db-read "select id from blog order by id desc limit 1"))) 0))

(defn create-comments-table []
  (sql/create-table
    :comment
    [:blogid :int]
    [:time :timestamp]    
    [:content "TEXT"]
    [:author "varchar(100)"]))

(defn add-comment [blog-id content author]
  (sql/with-connection 
    db
    (sql/insert-values
      :comment
      [:blogid :time :content :author]
      [(Integer/parseInt blog-id) (new Timestamp (.getTime (new Date))) content author])))

(defn get-comments [blog-id]
  (db-read "select * from comment where blogid=?" blog-id))

(defn get-latest-comments [n]
  (db-read "select * from comment order by time desc limit ?" n))


;;admin table management
(defn create-admin-table []
  (sql/create-table
    :admin   
    [:title "varchar(100)"]
    [:style "varchar(50)"]
    [:about "TEXT"]
    [:handle "varchar(100)"]
    [:pass   "varchar(100)"]
    [:email  "varchar(50)"]))

(defn set-admin [admin]
  (sql/with-connection db (sql/insert-record :admin admin)))

(defn update-admin [admin]
  (sql/with-connection 
    db
    (sql/update-values :admin ["handle=?" (:handle admin)] admin)))

(defn reset-blog []  
  (sql/with-connection 
    db  
    (sql/transaction
      (drop-table :admin)
      (drop-table :blog)
      (drop-table :comment)
      (drop-table :file)
      (create-admin-table)
      (create-blog-table)
      (create-comments-table)
      (create-file-table))    
    nil))

(defn get-admin []
  (try (first (db-read "select * from admin"))
    (catch java.sql.SQLException ex
      (when (.contains (.getMessage ex) "relation \"admin\" does not exist")
        (reset-blog)))))

(defn export []
  {:admin (dissoc (get-admin) :pass)
   :posts
   (vec (for [blog (db-read "select * from blog")]
          (assoc blog :comments (vec (db-read "select * from comment where blogid = ?" (:id blog))))))})

(defn import-posts [blog]
  (try 
    (let [content (read (new java.io.PushbackReader (new java.io.StringReader blog)))
          author (:handle (get-admin))]      
      (sql/with-connection
        db
        (doseq [{:keys [time title content]} (:posts content)]
          (println "importing post" title)          
          (sql/insert-values
            :blog
            [:time :title :content :author]
            [(new java.sql.Timestamp (.getTime time)) title content author]))))
    "import successful"
    (catch Exception ex 
      (do
        (println (.getMessage ex))
        (.printStackTrace ex)
        (.getMessage ex)))))


#_
  (defn update-schema []
    (println "updating tables...")
    (sql/with-connection 
      db
      (sql/transaction      
        (sql/do-commands "alter table blog add public boolean"))))

