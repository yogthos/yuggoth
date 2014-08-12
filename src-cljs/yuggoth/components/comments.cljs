(ns yuggoth.components.comments
  (:require [reagent.core :as reagent :refer [atom]]
            [yuggoth.session :as session]
            [yuggoth.util
             :refer [GET
                     POST
                     text
                     markdown
                     text-input]]))

(defn example [source & result]
  [:tr [:td (text source)] [:td result]])

(defn help [show-help?]
  (when @show-help?
    [:div
     [:table.mdhelp
      [example :italics-help [:em (text :italics-example)]]
      [example :bold-help [:b (text :bold-example)]]
      [example :strikethrough-help [:del (text :strikethrough-example)]]
      [example :link-help [:a {:href "http://http://example.net/"} (text :link-example)]]
      [example :superscript-help (text :super-example) [:sup (text :script-example)]]
      [example :quote-help [:blockquote (text :quote-example)]]
      [example :code-help [:code (text :code-help)]]]
    [:br]]))

(defn submit-comment [blogid author comment-text]
  (POST "/comment"
        {:params {:blogid blogid
                  :content @comment-text
                  :author (if (session/get :admin)
                            (session/get-in [:profile :handle])
                            @author)}
                  :handler (fn [comment]
                             (reset! comment-text "")
                             (session/update-in! [:post :comments] conj comment))}))

(defn comment-form []
  (let [blogid       (session/get-in [:post :id])
        comment-text (atom nil)
        author       (atom "anonymous")
        show-help?   (atom false)]
    (fn []
      [:div.comment-form
       [:textarea.comment-text
        {:placeholder (text :comment)
         :tab-index 4
         :value @comment-text
         :on-change #(reset! comment-text (-> % .-target .-value))}]
       [:br]
       (if (not-empty @comment-text)
         [:div.comment-preview
          (markdown @comment-text)])
       [:div.authorid
        (if (session/get :admin)
          [:span.author (text :commenting-as) (session/get-in [:profile :handle])]
          [:div
           [:span "by "]
           [text-input author {:class "author"}]])]
       [help show-help?]
       [:span.button {:on-click #(swap! show-help? not)} (text :help)]
       (when (not-empty @comment-text)
         [:span.button
          {:tab-index 5
           :on-click #(submit-comment blogid author comment-text)}
          (text :submit)])])))

(defn remove-comment [id]
  (fn [_]
    (session/update-in!
     [:post :comments]
     (fn [comments]
       (remove #(= (:id %) id) comments)))))

(defn delete-comment-button [id blogid]
  [:span.submit-comment
        {:tab-index 5
         :on-click #(POST "/delete-comment"
                          {:params {:id id :blogid blogid}
                           :handler (remove-comment id)})}
        (text :delete)])

(defn comment [{:keys [id blogid author content time]}]
  [:div.comment
    [:h4 time " - " author]
    [:div.comment-content
     [:div (markdown content)]
     (when (session/get :admin)
       [delete-comment-button id blogid])]])

(defn comments []
  (if-let [comments (session/get-in [:post :comments])]
    [:div.comments
     [:h2 (text :comments)]
     [:hr]
     (->> (for [{:keys [id] :as content} comments]
            ^{:key id} [comment content])
          (interpose [:hr]))
     [comment-form]]))

