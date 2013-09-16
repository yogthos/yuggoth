(ns yuggoth.models.db  
  (:require [yuggoth.models.schema :refer :all]
            [yuggoth.config :refer [db]]
            [clojure.java.jdbc :as sql]
            [clojure.java.jdbc.sql :refer [where]]
            [clj-time [format :as timef] [coerce :as timec]])
  (:import java.sql.Timestamp java.util.Date))

(defn db-update-or-insert [db table record where-clause]
  (sql/db-transaction [t-con db]
    (let [result (sql/update! t-con table record where-clause)]
      (if (zero? (first result))
        (sql/insert! t-con table record)
        result))))

;;admin user
(defn set-admin [admin]
  (sql/insert! @db :admin admin))

(defn update-admin [admin]
  (sql/update! @db :admin admin ["handle=?" (:handle admin)]))

(defn get-admin []  
  (first (sql/query @db ["select * from admin"])))

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
(defn update-post [id title tease content pubtime public page slug]
  (let [int-id (Integer/parseInt id)
        timeval (->> pubtime (timef/parse (timef/formatter "yyyy-MM-dd"))
                     timec/to-timestamp)]
    (sql/update! @db
      :blog
      {:title title :tease tease :content content :time timeval
       :page (Boolean/parseBoolean page) :slug slug
       :public (Boolean/parseBoolean public)}
      ["id=?" int-id])))

(defn admin-get-posts [& [limit offset]]
  (try
    (sql/query @db
               [(str "select id, time, title, author, public from blog "
                     "where page <> 'true' order by id desc "
                     (if (and (not (nil? limit))
                              (> limit 0)) (str "limit " limit
                                                (if (and (not (nil? offset))
                                                         (> offset 0))
                                                  (str " offset " offset)))))])
    (catch Exception ex nil)))
                     

(defn get-posts [& [limit full? private?]]    
  (try
    (sql/query @db
      [(str "select id, time, title, author, public" (if full? ", content, tease") 
            " from blog where page = 'false'" (if (not private?) " and public='true' ")
            "order by id desc "
            (if limit (str "limit " limit)))])
    (catch Exception ex nil)))

(defn admin-get-pages [& [limit]]    
  (try
    (sql/query @db
      [(str "select id, time, title, author, public, slug"  
            " from blog where page = 'true' "
            "order by title asc "
            (if limit (str "limit " limit)))])
    (catch Exception ex nil)))

(defn get-post [id]  
  (first (sql/query @db ["select * from blog where id=?" (Integer/parseInt id)])))

(defn get-page [slug]  
  (first (sql/query @db ["select * from blog where slug = ?" slug])))

(defn get-public-post-id [postid next?]
  (:id
   (first
    (sql/query @db 
               [(if next?
                  "select id from blog where id > ? and public='true' and page = 'false' order by id asc limit 1"
                  "select id from blog where id < ? and public='true' and page = 'false' order by id desc limit 1") (Integer/parseInt postid)]))))


(defn store-post [title tease content time public page slug]
  (let [author (:handle (get-admin))]
    (first (sql/insert! @db
                        :blog
                        {:time (or time (new Timestamp (.getTime (new Date))))
                         :title title
                         :tease tease
                         :content content
                         :author author
                         :slug slug
                         :page (Boolean/parseBoolean page)
                         :public (Boolean/parseBoolean public)}))))

(defn post-visible [id public]
  (sql/update! @db
    :blog
    {:public public}
    ["id=?" (Integer/parseInt id)]))

(defn get-last-post [] 
  (first (sql/query @db ["select * from blog where id = (select max(id) from blog where page = 'false')"])))

(defn get-last-public-post []
  (first (sql/query @db ["select * from blog where public='true' and page = 'false' order by id desc limit 1"])))

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
(defn tag-post [blogid tagid]
  (sql/insert! @db 
    :tag_map
    {:blogid blogid :tagid tagid}))

(defn untag-post [blogid tagid]
  (sql/delete! @db 
    :tag_map
    (where {:blogid blogid :tagid tagid})))

(defn admin-tags []
  (sql/query @db ["select * from tag order by name asc"])
  #_(map :name ))

(defn tags []
  (sql/query @db ["select * from tag where id in (select distinct tagid from tag_map) order by name asc"])
  #_(map :name ))

(defn add-tag [tag_name tag_slug]
  ; TODO insert slug value here also - need regex to strip out non-slug chars
  (sql/insert! @db 
    :tag {:name tag_name :slug tag_slug}))

(defn update-tag [id tag_name tag_slug]
  (sql/update! @db :tag {:name tag_name :slug tag_slug}
               ["id = ?" (Integer/parseInt id)]))

(defn delete-tag [tagid]
  (let [int_tagid (Integer/parseInt tagid)
        tagged_posts (count
                      (sql/query @db
                                 ["select count(*) from tag_map where tagid = ?" int_tagid]))]
    (if (> tagged_posts 0) (sql/delete! @db :tag_map (where {:tagid int_tagid})))
    (sql/delete! @db :tag (where {:id int_tagid}))))

(defn posts-by-tag [tag-name]
  (sql/query @db ["select id, time, title, public from blog, tag_map where id=blogid and tag_map.tag=?" 
                  tag-name]))

(defn posts-by-tag-slug [slug]
  (sql/query @db ["select blog.id id, time, title, public from blog, tag_map, tag where blog.id = blogid and tagid = tag.id and tag.slug=?" slug]))

(defn tag-by-slug [slug]
  (first (sql/query @db ["select name from tag where slug = ?" slug])))

(defn get-tag [id]
  (first (sql/query @db ["select * from tag where id = ?" id])))

(defn tag-ids-by-post [postid]
  (mapcat vals (sql/query @db ["select tagid from tag_map where blogid = ?" postid])))

(defn tags-by-post [postid]
  (sql/query @db ["select id, name, slug from tag t, tag_map tm where t.id = tm.tagid and blogid=?" postid])
  #_(mapcat vals ))

(defn update-tags [blogid blog-tags]    
  (let [id (if (string? blogid) (Integer/parseInt blogid) blogid)
        current-tags (tags)]    
    (sql/db-transaction [t-con @db]
      (sql/delete! t-con :tag_map (where {:blogid id}))
        (doseq [tag blog-tags]
          (if-not (some #{tag} current-tags) (add-tag tag t-con))
          (tag-post id tag t-con)))))
