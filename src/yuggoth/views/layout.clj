(ns yuggoth.views.layout
  (:use yuggoth.config
        noir.request
        hiccup.element
        hiccup.form
        hiccup.util
        [hiccup.page :only [include-css include-js html5]])
  (:require [yuggoth.util :as util]
            [noir.validation :as vali]
            [yuggoth.models.db :as db]
            [noir.session :as session]
            [selmer.parser :as parser]
            [ring.util.response :refer [response]])
  (:import java.util.Calendar compojure.response.Renderable))

(def template-path "yuggoth/views/templates/")

(deftype RenderableTemplate [template params]
  Renderable
  (render [this request]
    (println request)
    (->> (assoc params :servlet-context (:context request))
         (parser/render-file (str template-path template))
         response)))

(defn render [template & [params]]
  (RenderableTemplate. template (assoc params :admin (session/get :admin))))

(defn format-post [{:keys [id time title] :as post}]
  (assoc post
         :date (util/format-time time)
         :url (util/format-title-url id title)))

(defn posts-list []
  (->> (db/get-posts 5)
       (sort-by :time)
       (reverse)
       (map format-post)))

(defn footer-text []
  (let [adm (db/get-admin)]
    (str
      "Copyright (C) 2012-" (.get (Calendar/getInstance) Calendar/YEAR) " "
      (if-not (and (nil? adm) (nil? (:handle adm)))
        (clojure.string/capitalize (:handle adm))))))

(defn render-blog-page [title template & [params]]
  (let [html-title (if (string? title) title (:title title))
        title-elements (when (map? title) (:elements title))
        site-title (:title (db/get-admin))]
    (render template
            (assoc params
                   :title html-title
                   :site-title site-title
                   :posts (posts-list)
                   :tags (db/tags)
                   :footer (footer-text)
                   :selected-page
                   (cond
                     (.startsWith html-title (text :archives-title))
                     "#archives"
                     (.startsWith html-title (text :latest-comments-title))
                     "#latest"
                     (.startsWith html-title (text :login-title))
                     "#login"
                     (.startsWith html-title (text :about-title))
                     "#about"
                     (.startsWith html-title (text :new-post))
                     "#new-post"
                     :else "#home")))))


;; TODO remove
(defn header []
  [:div.header [:h1 [:div.site-title (:title (db/get-admin))]]])

(defn tag-list []
  [:p.taglist
   (for [tag (db/tags)]
     [:div.tag (link-to (str "/tag/" (:slug tag)) [:span.tagon (:name tag)])])])

(defn menu []
  [:div.menu 
   (into
     [:ul.nav
      [:li#home (link-to "/" (text :home-title))]
      [:li#archives (link-to "/archives" (text :archives-title))]
      [:li#about (link-to "/p/about" (text :about-title))]
      [:li#rss (link-to "/rss" [:div#rss "RSS"] #_(image "/img/rss.jpg"))]]
     (if (session/get :admin) 
       [[:li (link-to "/admin" "Admin")]
        [:li (link-to "/logout" (text :logout))]]))])

(defn sidebar [title]
  [:span
     [:h3 (text :recent-posts-title)]     
     (conj
       [:ul
        (for [{:keys [id time title]} (reverse (sort-by :time (db/get-posts 5)))]
          [:li 
           (link-to (str "/blog/" (util/format-title-url id title))
                    title
                    [:div.date (util/format-time time)])])]
       [:li (link-to "/archives" (text :more))])
     (tag-list)])

(defn footer []
  (let [adm (db/get-admin)]
    [:div.footer
     [:p (str "Copyright (C) 2012-" (.get (Calendar/getInstance) Calendar/YEAR) " ") 
      (if-not (and (nil? adm)
                   (nil? (:handle adm)))
        (clojure.string/capitalize (:handle adm)))
      (when-not (session/get :admin) [:span " (" (link-to "/login" (text :login)) ")"]) 
      (text :powered-by)
      (link-to "http://github.com/yogthos/yuggoth" "Yuggoth")]]))

(defn common [title & content]  
  (let [html-title (if (string? title) title (:title title))
        title-elements (when (map? title) (:elements title))
        site-title (:title (db/get-admin))]
    (html5
      [:head
       [:meta {:charset "utf-8"}]
       [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge,chrome=1"}]
       [:meta {:name "viewport"
               :content "width=device-width, initial-scale=1, maximum-scale=1"}]
       [:link {:rel "alternate" :type "application/rss+xml" :title site-title :href "/rss"}]
       [:title site-title]
       (include-css "/css/bootstrap.min.css")
       (include-css "/css/bootstrap-responsive.min.css")
       (include-js "/js/jquery.min.js")
       (include-js  "/js/bootstrap.js")]
      [:body              
       (hidden-field "selected" 
                     (cond
                      (.startsWith html-title (text :archives-title))        
                      "#archives"
                      (.startsWith html-title (text :latest-comments-title)) 
                      "#latest"
                      (.startsWith html-title (text :login-title))
                      "#login"
                      (.startsWith html-title (text :about-title))
                      "#about"
                      (.startsWith html-title (text :new-post))
                      "#new-post"
                      :else "#home"))
       [:div {:class "navbar offset1 span12"}
        [:div.navbar-inner
         [:a.brand {:href "/"} site-title]
         (menu)]
        [:div#content {:class "container span12"}
         [:div.row-fluid
          [:div.span9
           [:div {:class title} [:h3 html-title title-elements]]
           content]
          [:div.span3 (sidebar html-title)]]
         [:div {:style "text-align: center;"} (footer)]]]
       
       (include-js "/js/markdown.js"
                   "/js/shCore.js"
                   "/js/brushes/shBrushClojure.js"
                   "/js/brushes/shBrushBash.js"
                   "/js/brushes/shBrushCss.js"
                   "/js/brushes/shBrushJava.js"
                   "/js/brushes/shBrushJScript.js"
                   "/js/brushes/shBrushPlain.js"
                   "/js/brushes/shBrushXml.js")
       ;;workaround for hiccup not handling URLs without protocol correctly
       [:script {:type "text/javascript", :src "//ajax.googleapis.com/ajax/libs/jquery/1.8.0/jquery.min.js"}]
       (include-js "/js/jquery.alerts.js"
                   "/js/site.js")])))

(defn admin
  ([title content] (admin title nil content))
  ([title link content]  
     (let [html-title (if (string? title) title (:title title))
           title-elements (when (map? title) (:elements title))
           site-title (:title (db/get-admin))]
       (html5
        [:head
         [:meta {:charset "utf-8"}]
         [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge,chrome=1"}]
         [:meta {:name "viewport"
                 :content "width=device-width, initial-scale=1, maximum-scale=1"}]
         [:title (str site-title " - " title)]
         (include-css "/css/bootstrap.min.css")
         (include-css "/css/bootstrap-responsive.min.css")
         (include-js "/js/jquery.min.js")
         (include-js  "/js/bootstrap.js")]
        [:body
         [:div {:class "navbar offset1 span12"}
          [:div {:class "navbar-inner"}
           [:a {:class "brand" :href "/admin"} site-title]
           [:ul {:class "nav"}
            [:li [:a {:href "/admin/posts"} "Posts"]]
            [:li [:a {:href "/admin/pages"} "Pages"]]
            [:li [:a {:href "/admin/tags"} "Tags"]]
            [:li [:a {:href "/admin/comments"} "Comments"]]
            [:li [:a {:href "/admin/cache/clear"} "Clear Cache"]]]]]
         [:div {:id "header"}]
         [:div {:id "content" :class "container offset1 span12"}
          [:row
           [:legend html-title]
           (if-not (nil? link) [:div {:align "right"} (link-to (:link link) (:text link))])]
          content]
                                        ;(if (not (nil? init_script)) [:script init_script])
         ])))
  )
