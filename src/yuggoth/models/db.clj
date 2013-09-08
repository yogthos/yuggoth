(ns yuggoth.models.db  
  (:require [yuggoth.models.schema :refer :all]
            [yuggoth.config :refer [db]]
            [clojure.java.jdbc :as sql]
            [clojure.java.jdbc.sql :refer [where]])
  (:import java.sql.Timestamp java.util.Date))

(defn db-update-or-insert [db table record where-clause]
  (sql/db-transaction [t-con db]
    (let [result (sql/update! t-con table record where-clause)]
      (if (zero? (first result))
        (sql/insert! t-con table record)
        result))))

;files

(defn to-byte-array [x]  
  (with-open [input (new java.io.FileInputStream x)
              buffer (new java.io.ByteArrayOutputStream)]
    (clojure.java.io/copy input buffer)
    (.toByteArray buffer)))

(defn fix-file-name [filename]
  (.replaceAll filename "[^a-zA-Z0-9-\\.]" ""))

(defn store-file [{:keys [tempfile filename content-type] :as file}]  
  (db-update-or-insert @db
    :file
    {:type content-type :name (fix-file-name filename) :data (to-byte-array tempfile)}
    ["name=?" filename]))

(defn list-files []
  (map :name (sql/query @db ["select name from file"])))

(defn delete-file [name]  
  (println name)
  (sql/delete! @db :file (where {:name name})))

(defn get-file [name]
  (first (sql/query @db ["select * from file where name=?" name])))


;;blog posts

(defn update-post [id title content public]
  (let [int-id (Integer/parseInt id)] 
    (sql/update! @db
      :blog
      {:id int-id :title title :content content :public (Boolean/parseBoolean public)}
      ["id=?" int-id])))

(defn get-posts [& [limit full? private?]]    
  (try
    (sql/query @db
      [(str "select id, time, title, author, public" (if full? ", content") 
            " from blog " (if (not private?) "where public='true'") " order by id desc " 
            (if limit (str "limit " limit)))])
    (catch Exception ex nil)))

(defn get-post [id]  
  (first (sql/query @db ["select * from blog where id=?" (Integer/parseInt id)])))

(defn get-public-post-id [id next?]
  (:id
    (first
      (sql/query @db 
        [(if next?
           "select id from blog where id > ? and public='true' order by id asc limit 1"
           "select id from blog where id < ? and public='true' order by id desc limit 1")] 
        (Integer/parseInt id)))))


(defn store-post [title content author public]
  (sql/insert! @db
    :blog
    {:time (new Timestamp (.getTime (new Date)))
     :title title
     :content content
     :author author
     :public (Boolean/parseBoolean public)}))

(defn post-visible [id public]
  (sql/update! @db
    :blog
    {:public public}
    ["id=?" (Integer/parseInt id)]))

(defn get-last-post [] 
  (first (sql/query @db ["select * from blog where id = (select max(id) from blog)"])))

(defn get-last-public-post []
  (first (sql/query @db ["select * from blog where public='true' order by id desc limit 1"])))

(defn last-post-id []
  (or (:id (first (sql/query @db ["select id from blog order by id desc limit 1"]))) 0))

;;comments
(defn add-comment [blog-id content author]
  (sql/insert! @db
    :comment
    {:blogid (Integer/parseInt blog-id)
     :time (new Timestamp (.getTime (new Date)))
     :content content
     :author (if (> (count author) 100) (.substring author 0 100) author)}))

(defn get-comments [blog-id]
  (sql/query @db ["select * from comment where blogid=?" blog-id]))

(defn get-latest-comments [n]
  (sql/query @db ["select * from comment order by time desc limit ?" n]))

(defn delete-comment [id]  
  (sql/delete! @db :comment (where {:id (Integer/parseInt id)})))

;;tags
(defn tag-post [blogid tag & [db]]
  (sql/insert! (or db @db)
    :tag_map
    {:blogid blogid :tag tag}))

(defn tags []
  (map :name (sql/query @db ["select * from tag"])))

(defn add-tag [tag-name & [db]]
  (sql/insert! (or db @db)
    :tag {:name (.toLowerCase tag-name)}))

(defn delete-tags [tags]
  (doseq [tag tags] 
    (sql/delete! @db :tag_map (where {:tag tag}))
    (sql/delete! @db :tag (where {:name tag}))))

(defn posts-by-tag [tag-name]
  (sql/query @db ["select id, time, title, public from blog, tag_map where id=blogid and tag_map.tag=?" 
                  tag-name]))

(defn tags-by-post [postid]
  (mapcat vals (sql/query @db ["select tag from tag_map where blogid=?" postid])))

(defn update-tags [blogid blog-tags]    
  (let [id (if (string? blogid) (Integer/parseInt blogid) blogid)
        current-tags (tags)]    
    (sql/db-transaction [t-con @db]
      (sql/delete! t-con :tag_map (where {:blogid id}))
        (doseq [tag blog-tags]
          (if-not (some #{tag} current-tags) (add-tag tag t-con))
          (tag-post id tag t-con)))))

;;admin user
(defn set-admin [admin]
  (sql/insert! @db :admin admin))

(defn update-admin [admin]
  (sql/update! @db :admin admin ["handle=?" (:handle admin)]))

(defn get-admin []  
  (first (sql/query @db ["select * from admin"])))
