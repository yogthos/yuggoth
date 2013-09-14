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
            [noir.session :as session])
  (:import java.util.Calendar))

(defn header []
  [:div.header [:h1 [:div.site-title (:title (db/get-admin))]]])

(defn tag-list []
  [:p.taglist
   (for [tag (db/tags)]
     [:div.tag (link-to (str "/tag/" (:slug tag)) [:span.tagon (:name tag)])])])

(defn menu []
  [:div.menu 
   (into
     (if (session/get :admin) 
       [:ul.menu-items          
        [:li (link-to "/logout" (text :logout))]
        [:li (link-to "/admin" "Admin")]
        #_[:li (link-to "/upload" (text :upload))]
        #_[:li#latest (link-to "/latest-comments" (text :latest-comments-title))]
        #_[:li#new-post (link-to "/make-post" (text :new-post))]]
       [:ul.menu-items])     
     [[:li#rss (link-to "/rss" [:div#rss "rss"] #_(image "/img/rss.jpg"))]      
      [:li#about (link-to "/about" (text :about-title))]      
      [:li#archives (link-to "/archives" (text :archives-title))]
      [:li#home (link-to "/" (text :home-title))]])])

(defn sidebar [title]
  (if (or (= (text :new-post) title) (= (text :edit-post) title))    
    [:div.sidebar-preview
     [:h2 [:span.render-preview (text :preview-title)]]
     [:div#post-preview]]
    
    [:div.sidebar
     [:h2 (text :recent-posts-title)]     
     (conj
       [:ul
        (for [{:keys [id time title]} (reverse (sort-by :time (db/get-posts 5)))]
          [:li 
           (link-to (str "/blog/" (util/format-title-url id title))
                    title
                    [:div.date (util/format-time time)])])]
       [:li (link-to "/archives" (text :more))])
     (tag-list)]))

(defn footer []
  [:div.footer
   [:p (str "Copyright (C) 2012-" (.get (Calendar/getInstance) Calendar/YEAR) " ") 
    (clojure.string/capitalize (:handle (db/get-admin))) 
    (when-not (session/get :admin) [:span " (" (link-to "/login" (text :login)) ")"]) 
    (text :powered-by)
    (link-to "http://github.com/yogthos/yuggoth" "Yuggoth")]])

(defn common [title & content]  
  (let [html-title (if (string? title) title (:title title))
        title-elements (when (map? title) (:elements title))]            
    (html5
      [:head
       [:link {:rel "alternate" :type "application/rss+xml" :title (:title (db/get-admin)) :href "/rss"}]       
       [:title html-title]
       (include-css (util/get-css)
                    "/css/jquery.alerts.css"
                    "/css/shCoreYuggoth.css")
       [:script {:type "text/javascript"} (str "var context=\"" (:context *request*) "\";")]]
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
       [:div.container
        (header)
        (menu)
        [:div.contents 
         [:div.post
          [:div.entry-title [:h2 html-title title-elements]]
          [:div.entry-content content]]
         (sidebar html-title)]          
        (footer)]
       
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
         (include-css "/bootstrap/css/bootstrap.css")
         (include-css "/bootstrap/css/bootstrap-responsive.css")
         (include-js "/js/jquery.min.js")
         (include-js  "/bootstrap/js/bootstrap.js")]
        [:body
         [:div {:class "navbar offset1 span12"}
          [:div {:class "navbar-inner"}
           [:a {:class "brand" :href "/admin"} site-title]
           [:ul {:class "nav"}
            [:li [:a {:href "/admin/posts"} "Posts"]]
            [:li [:a {:href "/admin/pages"} "Pages"]]
            [:li [:a {:href "/admin/tags"} "Tags"]]
            [:li [:a {:href "/admin/comments"} "Comments"]]
            [:li [:a {:href "/admin/cache/clear"} "Clear Cache"]]
            #_(for [nav nav_links]
                (nav-item nav url_base))]]]
                                        ;(page-nav url_base)
         [:div {:id "header"}]
         [:div {:id "content" :class "container offset1 span12"}
          [:row
           [:legend html-title]
           (if-not (nil? link) [:div {:align "right"} (link-to (:link link) (:text link))])]
          content]
                                        ;(if (not (nil? init_script)) [:script init_script])
         ])))
  )

(comment
       #_(include-js "/js/markdown.js"
                   "/js/shCore.js"
                   "/js/brushes/shBrushClojure.js"
                   "/js/brushes/shBrushBash.js"
                   "/js/brushes/shBrushCss.js"
                   "/js/brushes/shBrushJava.js"
                   "/js/brushes/shBrushJScript.js"
                   "/js/brushes/shBrushPlain.js"
                   "/js/brushes/shBrushXml.js")
       ;;workaround for hiccup not handling URLs without protocol correctly
       ;[:script {:type "text/javascript", :src "//ajax.googleapis.com/ajax/libs/jquery/1.8.0/jquery.min.js"}]
       #_(include-js "/js/jquery.alerts.js"
                   "/js/site.js")
)

