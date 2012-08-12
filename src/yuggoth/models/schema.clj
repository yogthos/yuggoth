(ns yuggoth.models.schema
  (:use [yuggoth.models.db :only [db drop-table]])
  (:require [clojure.java.jdbc :as sql]))

;;file table
(defn create-file-table []
  (sql/create-table
    :file
    [:type "varchar(50)"]
    [:name "varchar(50)"]
    [:data "bytea"]))

;;blog table
(defn create-blog-table []
  (sql/create-table
    :blog
    [:id "SERIAL"]
    [:time :timestamp]
    [:title "varchar(100)"]
    [:content "TEXT"]
    [:author "varchar(100)"]    
    [:public boolean]))

;;comment table 
(defn create-comments-table []
  (sql/create-table
    :comment
    [:blogid :int]
    [:time :timestamp]    
    [:content "TEXT"]
    [:author "varchar(100)"]))

;;tag table
(defn create-tag-table []
  (sql/create-table
    :tag
    [:name "varchar(50)"]))

(defn create-tag-map-table []
  (sql/create-table
    :tag_map
    [:blogid :int]
    [:tag "varchar(50)"]))

;;admin table
(defn create-admin-table []
  (sql/create-table
    :admin   
    [:title "varchar(100)"]
    [:style "varchar(50)"]
    [:about "TEXT"]
    [:handle "varchar(100)"]
    [:pass   "varchar(100)"]
    [:email  "varchar(50)"]))

(defn reset-blog []  
  (sql/with-connection 
    db  
    (sql/transaction
      (drop-table :admin)
      (drop-table :blog)
      (drop-table :comment)
      (drop-table :file)
      (drop-table :tag)
      (drop-table :tag_map)
      (create-admin-table)
      (create-blog-table)
      (create-comments-table)
      (create-file-table)
      (create-tag-table)
      (create-tag-map-table))    
    nil))

#_(def updated (atom false))
#_(defn update-schema []
  (when (not @updated)
    (println "updating tables...")
    (sql/with-connection 
      db
      (sql/transaction      
        (sql/do-commands "alter table tag alter column name type varchar(50)")
        (sql/do-commands "alter table tag_map alter column tag type varchar(50)")))
    (reset! updated true)))

#_(def added-tags (atom false))
#_(defn add-tags []
  (when (not @added-tags)     
    (sql/with-connection
      db 
      (sql/transaction
        (create-tag-table)
        (create-tag-map-table)))
    (reset! added-tags true)))