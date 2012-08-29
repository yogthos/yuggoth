(ns yuggoth.models.db
  (:use yuggoth.models.schema config)
  (:require [clojure.java.jdbc :as sql])
  (:import java.sql.Timestamp java.util.Date))

(defn db-read
  "returns the result of running the supplied SQL query"
  [query & args]
  (sql/with-connection 
    @db
    (sql/with-query-results res (vec (cons query args)) (doall res))))

(defn transaction
  "runs a function with the supplied arguments in an SQL transaction
eg: (transaction add-user email firstname lastname password)"
  [f & args]
  (sql/with-connection @db
    (sql/transaction
      (apply f args))))

;files

(defn to-byte-array [x]  
  (with-open [input (new java.io.FileInputStream x)
              buffer (new java.io.ByteArrayOutputStream)]
    (clojure.java.io/copy input buffer)
    (.toByteArray buffer)))

(defn fix-file-name [filename]
  (.replaceAll filename "[^a-zA-Z0-9-\\.]" ""))

(defn store-file [{:keys [tempfile filename content-type]}]
  (sql/with-connection 
    @db
    (sql/update-or-insert-values
      :file
      ["name=?" filename]
      {:type content-type :name (fix-file-name filename) :data (to-byte-array tempfile)})))

(defn list-files []
  (map :name (db-read "select name from file")))

(defn delete-file [name]  
  (sql/with-connection @db (sql/delete-rows :file ["name=?" name])))

(defn get-file [name]
  (first (db-read "select * from file where name=?" name)))


;;blog posts

(defn update-post [id title content public]
  (let [int-id (Integer/parseInt id)] 
    (sql/with-connection
      @db
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
    @db
    (sql/insert-values
      :blog
      [:time :title :content :author :public]
      [(new Timestamp (.getTime (new Date))) title content author (Boolean/parseBoolean public)])))

(defn post-visible [id public]
  (sql/with-connection 
    @db
    (sql/update-values
      :blog 
      ["id=?" (Integer/parseInt id)]
      {:public public})))

(defn get-last-post [] 
  (first (db-read "select * from blog where id = (select max(id) from blog)")))

(defn get-last-public-post []
  (first (db-read "select * from blog where public='true' order by id desc limit 1")))

(defn last-post-id []
  (or (:id (first (db-read "select id from blog order by id desc limit 1"))) 0))


;;comments
(defn add-comment [blog-id content author]
  (sql/with-connection 
    @db
    (sql/insert-values
      :comment
      [:blogid :time :content :author]
      [(Integer/parseInt blog-id) (new Timestamp (.getTime (new Date))) content (.substring author 0 100)])))

(defn get-comments [blog-id]
  (db-read "select * from comment where blogid=?" blog-id))

(defn get-latest-comments [n]
  (db-read "select * from comment order by time desc limit ?" n))

(defn delete-comment [id]  
  (sql/with-connection
    @db 
    (sql/delete-rows :comment ["id=?" (Integer/parseInt id)])))

;;tags
(defn tag-post [blogid tag]
  (sql/insert-values
    :tag_map
    [:blogid :tag]
    [blogid tag]))

(defn tags []
  (map :name (db-read "select * from tag")))

(defn add-tag [tag-name]
  (sql/with-connection
    @db
    (sql/insert-values
      :tag [:name] [(.toLowerCase tag-name)])))

(defn delete-tags [tags]
  (sql/with-connection
    @db
    (doseq [tag tags] 
      (sql/delete-rows :tag_map ["tag=?" tag])
      (sql/delete-rows :tag ["name=?" tag]))))

(defn posts-by-tag [tag-name]
  (db-read "select id, time, title, public from blog, tag_map where id=blogid and tag_map.tag=?" 
           tag-name))

(defn tags-by-post [postid]
  (mapcat vals (db-read "select tag from tag_map where blogid=?" postid)))

(defn update-tags [blogid tags]    
  (let [id (if (string? blogid) (Integer/parseInt blogid) blogid)]    
    (sql/with-connection
      @db
      (sql/transaction
        (sql/delete-rows :tag_map ["blogid=?" id])
        (doseq [tag tags]
          (if (nil? (sql/with-query-results res ["select * from tag where name=?" tag] (doall res)))
            (sql/insert-values :tag [:name] [tag]))
          (tag-post id tag))))))

;;admin user
(defn set-admin [admin]
  (sql/with-connection @db (sql/insert-record :admin admin)))

(defn update-admin [admin]
  (sql/with-connection 
    @db
    (sql/update-values :admin ["handle=?" (:handle admin)] admin)))


(defn get-admin []  
  (first (db-read "select * from admin")))


;;backup
(defn export []
  {:admin (dissoc (get-admin) :pass)
   :posts
   (vec (for [blog (db-read "select * from blog")]
          (assoc blog :comments (vec (db-read "select * from comment where blogid = ?" (:id blog))))))})

(defn import-posts [blog]
  (try 
    (let [content (read (new java.io.PushbackReader (new java.io.StringReader blog)))
          admin   (get-admin)
          author (:handle admin)]
      
      (sql/with-connection
        @db
        (sql/transaction 
          (sql/update-or-insert-values :admin ["handle=?" author] (assoc admin :about (:about (:admin content))))
          (sql/do-commands "ALTER SEQUENCE blog_id_seq RESTART WITH 1")          
          (sql/do-commands "ALTER SEQUENCE comment_id_seq RESTART WITH 1")
          (sql/do-commands "delete from blog")
          (sql/do-commands "delete from comment")          
          (doseq [{:keys [id time title content comments]} (sort-by :id (:posts content))]
            (println "importing post" id "-" title)          
            (let [{:keys [id title]} (sql/insert-values
                                                 :blog
                                                 [:time :title :content :author :public]
                                                 [(new java.sql.Timestamp (.getTime time)) title content author true])] 
              (println "inserted" id title)
              (doseq [{:keys [author time content]} comments] 
              (sql/insert-values
                :comment
                [:blogid :time :content :author]
                [id (new java.sql.Timestamp (.getTime time)) content author])))))))
    "import successful"
    (catch Exception ex 
      (let [next-ex (or (.getNextException ex) ex)]
        (println (.getMessage next-ex))
        (.printStackTrace next-ex)
        (.getMessage next-ex)))))

