(ns yuggoth.models.db
  (:use yuggoth.models.schema yuggoth.config)
  (:require [clojure.java.jdbc :as sql])
  (:import java.sql.Timestamp java.util.Date))

(defmacro with-db [f & body]
  `(sql/with-connection
    (deref yuggoth.config/db) (~f ~@body)))

(defn db-read
  "returns the result of running the supplied SQL query"
  [& query]
  (with-db sql/with-query-results res (vec query) (doall res)))


;files

(defn to-byte-array [x]  
  (with-open [input (new java.io.FileInputStream x)
              buffer (new java.io.ByteArrayOutputStream)]
    (clojure.java.io/copy input buffer)
    (.toByteArray buffer)))

(defn fix-file-name [filename]
  (.replaceAll filename "[^a-zA-Z0-9-\\.]" ""))

(defn store-file [{:keys [tempfile filename content-type] :as file}]  
  (with-db sql/update-or-insert-values
    :file
    ["name=?" filename]
    {:type content-type :name (fix-file-name filename) :data (to-byte-array tempfile)}))

(defn list-files []
  (map :name (db-read "select name from file")))

(defn delete-file [name]  
  (with-db sql/delete-rows :file ["name=?" name]))

(defn get-file [name]
  (first (db-read "select * from file where name=?" name)))


;;blog posts

(defn update-post [id title content public]
  (let [int-id (Integer/parseInt id)] 
    (with-db sql/update-values
      :blog
      ["id=?" int-id]
      {:id int-id :title title :content content :public (Boolean/parseBoolean public)})))

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
  (with-db sql/insert-values
    :blog
    [:time :title :content :author :public]
    [(new Timestamp (.getTime (new Date))) title content author (Boolean/parseBoolean public)]))

(defn post-visible [id public]
  (with-db sql/update-values
    :blog 
    ["id=?" (Integer/parseInt id)]
    {:public public}))

(defn get-last-post [] 
  (first (db-read "select * from blog where id = (select max(id) from blog)")))

(defn get-last-public-post []
  (first (db-read "select * from blog where public='true' order by id desc limit 1")))

(defn last-post-id []
  (or (:id (first (db-read "select id from blog order by id desc limit 1"))) 0))

;;comments
(defn add-comment [blog-id content author]
  (with-db sql/insert-values
    :comment
    [:blogid :time :content :author]
    [(Integer/parseInt blog-id) 
     (new Timestamp (.getTime (new Date))) 
     content 
     (if (> (count author) 100) (.substring author 0 100) author)]))

(defn get-comments [blog-id]
  (db-read "select * from comment where blogid=?" blog-id))

(defn get-latest-comments [n]
  (db-read "select * from comment order by time desc limit ?" n))

(defn delete-comment [id]  
  (with-db sql/delete-rows :comment ["id=?" (Integer/parseInt id)]))

;;tags
(defn tag-post [blogid tag]
  (sql/insert-values
    :tag_map
    [:blogid :tag]
    [blogid tag]))

(defn tags []
  (map :name (db-read "select * from tag")))

(defn add-tag [tag-name]
  (with-db sql/insert-values
    :tag [:name] [(.toLowerCase tag-name)]))

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
    (with-db sql/transaction
      (sql/delete-rows :tag_map ["blogid=?" id])
        (doseq [tag tags]
          (if (nil? (sql/with-query-results res ["select * from tag where name=?" tag] (doall res)))
            (sql/insert-values :tag [:name] [tag]))
          (tag-post id tag)))))

;;admin user
(defn set-admin [admin]
  (with-db sql/insert-record :admin admin))

(defn update-admin [admin]
  (with-db sql/update-values :admin ["handle=?" (:handle admin)] admin))

(defn get-admin []  
  (first (db-read "select * from admin")))
