(ns catalog.cms-dynamic-content
  (:require  #?@(:cljs [[goog.string]])
             [clojure.set :as set]
             [spice.selector :as selector]
             [spice.maps :as maps]
             [clojure.string :as string]))

(def rich-text-hierarchy
  (-> (make-hierarchy)
      ;; NOTE: This basically is just setting it up so that the keyword
      ;;       on the left gets built as the entity on the right

      ;;            child                 parent
      (derive :rich-text/heading-1 :rich-text/heading)
      (derive :rich-text/heading-2 :rich-text/heading)
      (derive :rich-text/heading-3 :rich-text/heading)
      (derive :rich-text/heading-4 :rich-text/heading)
      (derive :rich-text/heading-5 :rich-text/heading)
      (derive :rich-text/heading-6 :rich-text/heading)))

(defmulti build-hiccup-tag
  (fn [{:as   _node
        :keys [data content node-type]}]
    (cond
      (and (= node-type "embedded-asset-block")
           (some-> data
                   :target
                   :file
                   :content-type
                   (string/starts-with? "video")))
      :rich-text/embedded-video-block

      (and (= node-type "paragraph")
           (some-> content
                   first
                   :value
                   empty?))
      :rich-text/spacer

      :else
      (keyword "rich-text" node-type)))
  :hierarchy #'rich-text-hierarchy)

(defn build-hiccup-content [tag content]
  (into tag
        (map build-hiccup-tag)
        content))

(defmethod build-hiccup-tag :rich-text/heading [{:keys [content]}]
  (build-hiccup-content [:h3] content))

(defmethod build-hiccup-tag :rich-text/paragraph [{:keys [content]}]
  (build-hiccup-content [:p] content))

(defmethod build-hiccup-tag :rich-text/spacer [{:keys []}]
  [:div.py4])

(defmethod build-hiccup-tag :rich-text/hyperlink [{:keys [content data]}]
  (build-hiccup-content [:a {:href (:uri data)}] content))

(defmethod build-hiccup-tag :rich-text/unordered-list [{:keys [content]}]
  (build-hiccup-content [:ul] content))

;;TODO Needs styling before we enable this in production
(defmethod build-hiccup-tag :rich-text/ordered-list [{:keys [content]}]
  (build-hiccup-content [:ol] content))

(defmethod build-hiccup-tag :rich-text/list-item [{:keys [content]}]
  (build-hiccup-content [:li] content))

(defmethod build-hiccup-tag :rich-text/embedded-asset-block [{:as _node :keys [data]}]
  [:a {:href (:url (:file (:target data)))}
   (:title (:target data))])

(defmethod build-hiccup-tag :rich-text/embedded-video-block [{:as _node :keys [data]}]
  (let [target (:target data)
        file   (:file target)]
    [:div.mt2.relative
     [:video.relative.z0
      {:controls true
       :autoplay true
       :style    {:width "100%"}}
      [:source {:src (:url file)}]]
     [:div.white.absolute.left-0.top-0.z1.px2.py1
      [:span.h3.bold.pr1 "Watch:"]
      (:title target)]]))

(defmethod build-hiccup-tag :rich-text/text [{:keys [value]}]
  ;; TODO: Strip initial newlines?
  value)

(defmethod build-hiccup-tag :rich-text/document [{:keys [content]}]
  ;; TODO: Strip initial newlines?
  (build-hiccup-content [:div] content))

(defmethod build-hiccup-tag :default [node]
  ;; TODO: Productionalize this error message
  (println "Attempting to render unknown node type" node)
  nil)

(defn cms-and-sku->template-slot-hiccup
  [cms-dynamic-content-data sku]
  #?(:cljs
     (do
       (->> (set/rename-keys cms-dynamic-content-data
                             ;; NOTE: This should only really be used when we can't or won't
                             ;;       dictate the slugs of template slots in contentful. If
                             ;;       the slugs in contenful match the slugs here no rename
                             ;;       is necessary. This allows us to overwrite the older cellar
                             ;;       entries without having to add a mapping here; no deploy needed
                             ;;
                             ;;       See existing template-slot entries in contentful for examples
                             {})
            (maps/map-values
             (fn [template-slot]
               (some->> (template-slot :selectable-values)
                    (map (fn [selectable-value ]
                           (merge selectable-value
                                  (:selector selectable-value))))
                    (selector/match-essentials
                     (assoc sku :selector/essentials (into #{} (comp
                                                                (map :selector)
                                                                (mapcat keys))
                                                           (:selectable-values template-slot))))
                    ;; TODO Move the code responsible for merging the selector attributes into the top level
                    ;; of the selectable value to the API / Handler areas)
                    (into [:div]
                          (comp
                           (map :value)
                           (map build-hiccup-tag))))))
            (maps/deep-remove-nils)))))
