(ns yuggoth.views.common
  (:use hiccup.element hiccup.form hiccup.util
        [noir.core]
        [noir.validation :as vali]        
        [hiccup.page :only [include-css include-js html5]])
  (:require [yuggoth.views.util :as util]
            [yuggoth.models.db :as db]
            [noir.session :as session]))

(defn header []
  [:div.header [:h1 [:div.site-title (:title (db/get-admin))]]])

(defn tag-list []
  (into [:p.taglist] 
        (for [tag (db/tags)]
          [:div.tag (link-to (str "/tag/" tag) [:span.tagon tag])])))

(defn menu []
  [:div.menu 
   (into
     (if (session/get :admin) 
       [:ul.menu-items          
        [:li (link-to "/logout" "logout")]
        [:li (link-to "/profile" "profile")]
        [:li (link-to "/upload" "upload")]
        [:li#latest (link-to "/latest-comments" "latest comments")]
        [:li#new-post (link-to "/make-post" "New post")]]
       [:ul.menu-items])     
     [[:li#rss (link-to "/rss" [:div#rss "rss"] (image "/img/rss.jpg"))]      
      [:li#about (link-to "/about" "About")]      
      [:li#archives (link-to "/archives" "Archives")]
      [:li#home (link-to "/" "Home")]])])

(defn sidebar [title]
  (if (or (= "New post" title) (= "Edit post" title))    
    [:div.sidebar-preview
     [:h2 [:span.render-preview "Preview (click to redraw)"]]
     [:div#post-preview]]
    
    [:div.sidebar
     [:h2 "Recent posts"]     
     (-> [:ul]       
       (into 
         (for [{:keys [id time title]} (reverse (sort-by :time (db/get-posts 5)))]
           [:li 
            (link-to (str "/blog/" (str id "-" (url-encode title)))
                     title
                     [:div.date (util/format-time time)])]))
       (conj [:li (link-to "/archives" "more...")]))
     (tag-list)]))

(defn footer []
  [:div.footer
   [:p "Copyright (C) 2012 " 
    (:handle (db/get-admin)) 
    (when (not (session/get :admin)) [:span " (" (link-to "/login" "login") ")"]) 
    " - Powered by: "
    (link-to "http://github.com/yogthos/yuggoth" "Yuggoth")]])

(defpartial layout [title & content]
  (let [html-title (if (string? title) title (:title title))
        title-elements (when (map? title) (:elements title))]        
    (html5
      [:head
       [:link {:rel "alternate" :type "application/rss+xml" :title (:title (db/get-admin)) :href "/rss"}]       
       [:title html-title]
       (include-css (util/get-css)
                    "/css/jquery.alerts.css"
                    "/css/shCoreYuggoth.css")]      
      [:body       
       (hidden-field "selected" 
                     (condp = (first (.split html-title " "))
                       "Archives" "#archives"
                       "Latest"   "#latest"
                       "Login"    "#login"
                       "About"    "#about"
                       "New"      "#new-post"
                       "#home"))
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