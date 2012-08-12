(ns yuggoth.models.schema  
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
